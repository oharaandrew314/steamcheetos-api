package io.andrewohara.cheetosbros.api.auth

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.javalin.core.security.AccessManager
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse
import java.security.SecureRandom
import java.util.*
import javax.servlet.http.Cookie

class CheetosAuthorizationHandler(private val users: UsersManager): AccessManager {

    private val random = SecureRandom()
    private val base64 = Base64.getUrlEncoder()

    override fun manage(handler: Handler, ctx: Context, permittedRoles: MutableSet<Role>) {
        val user = getAuthorizedUser(ctx)

        val role = if (user == null) {
            CheetosRole.Public
        } else {
            ctx.attribute("user", user)
            CheetosRole.User
        }

        if (role !in permittedRoles) throw UnauthorizedResponse()

        handler.handle(ctx)
    }

    private fun getAuthorizedUser(ctx: Context): User? {
        val sessionId = ctx.cookie("cheetosbros-session-id") ?: return null
        return users.getUserByToken(sessionId)
    }

    fun createSession(ctx: Context): String {
        val sessionId = let {
            val buffer = ByteArray(24)
            random.nextBytes(buffer)
            base64.encodeToString(buffer)
        }

        val cookie = Cookie("cheetosbros-session-id", sessionId).apply {
            isHttpOnly = true
        }
        ctx.cookie(cookie)

        return sessionId
    }
}

enum class CheetosRole: Role {
    Public, User
}