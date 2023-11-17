import io.ktor.server.auth.*

data class UserSession(
    val token: String, val count: Int
) : Principal