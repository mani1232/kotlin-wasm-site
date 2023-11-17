import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dataClasses.ChatUser
import io.ktor.http.*
import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

val tempDb = mutableListOf(
    ChatUser("mani123", "mani1232", "test")
)

val privateDir = File("private/")

fun main() {
    val keyStoreFile = File("build/keystore.jks")
    privateDir.mkdirs()
    val keyStore = buildKeyStore {
        certificate("sampleAlias") {
            password = "foobar"
            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
        }
    }
    keyStore.saveToFile(keyStoreFile, "123456")

    val environment = applicationEngineEnvironment {
        //connector {
        //    port = 8080
        //}
        sslConnector(
            keyStore = keyStore,
            keyAlias = "sampleAlias",
            keyStorePassword = { "123456".toCharArray() },
            privateKeyPassword = { "foobar".toCharArray() }) {
            port = 8080
            keyStorePath = keyStoreFile
        }
        module(Application::loadServer)
    }

    embeddedServer(Netty, environment).start(wait = true)
}

fun Application.loadServer() {
    install(DefaultHeaders) {
        header("X-Creator", "mani123")
    }
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }
    staticContent()
    webSockets()
}

fun Application.staticContent() {
    routing {
        staticResources("/static-js", "static/static-js")
        staticResources("/static-css", "static/static-css")
        staticResources("/static-html", "static/static-html")
        get("/") {
            call.respondFile(privateDir, "/index.html")
        }
        get("/ver") {
            call.respondText("HTTP version is ${call.request.httpVersion}")
        }
    }
}

fun Application.webSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 60
        }
    }
    val secret = "34567erhrgfuydfgdfg"
    val issuer = "http://127.0.0.1:80/"
    val audience = "ws://127.0.0.1:80/chat"
    val myRealm = "Access to 'chat'"
    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
        form("auth-form") {
            userParamName = "username"
            passwordParamName = "password"
            validate { credentials ->
                val user = tempDb.firstOrNull { it.userName == credentials.name && it.password == credentials.password }
                if (user != null) {
                    val token = JWT.create()
                        .withAudience(audience)
                        .withIssuer(issuer)
                        .withClaim("username", credentials.name)
                        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                        .sign(Algorithm.HMAC256(secret))
                    user.sessionTokens.add(token)
                    UserIdPrincipal(token)
                } else {
                    null
                }
            }
        }
        session<UserSession>("auth-session") {
            validate { session ->
                val user = tempDb.firstOrNull { it.sessionTokens.contains(session.token) }
                if (user != null) {
                    session
                } else {
                    null
                }
            }
            challenge {
                call.respondRedirect("/login")
            }
        }
    }

    routing {

        authenticate("auth-form") {
            post("/login") {
                val token = call.principal<UserIdPrincipal>()?.name.toString()
                call.sessions.set(UserSession(token = token, count = 1))
                call.respondRedirect("/hello")
            }
        }

        authenticate("auth-session") {
            get("/hello") {
                val userSession = call.principal<UserSession>()
                call.sessions.set(userSession?.copy(count = userSession.count + 1))
                call.respondText("Hello, ${userSession?.token}! Visit count is ${userSession?.count}.")
            }
        }

        authenticate("auth-jwt") {
            val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
            webSocket("/chat") {
                println("Adding user!")
                val thisConnection = Connection(this)
                connections += thisConnection
                try {
                    send("You are connected! There are ${connections.count()} users here.")
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val receivedText = frame.readText()
                        val textWithUsername = "[${thisConnection.name}]: $receivedText"
                        connections.forEach {
                            it.session.send(textWithUsername)
                        }
                    }
                } catch (e: Exception) {
                    println(e.localizedMessage)
                } finally {
                    println("Removing $thisConnection!")
                    connections -= thisConnection
                }
            }
        }

        get("/logout") {
            tempDb.firstOrNull { it.sessionTokens.remove(call.principal<UserSession>()?.token) }
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }
    }
}

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }

    val name = "user${lastId.getAndIncrement()}"
}