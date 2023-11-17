import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

class MainKtTest {

    @Serializable
    data class UserLogin(
        @SerialName("username")
        val userName: String,
        val password: String,
    )

    @Test
    fun testPostLogin() = testApplication {
        application {
            loadServer()
        }
        client.post("/login") {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            val data = listOf("username" to "jetbrains", "password" to "foobar").formUrlEncode()
            println(data)
            setBody(data)
        }.apply {
            println(call.response.bodyAsText())
            println(call.request.url)
            println(call.request.content)
        }
    }
}