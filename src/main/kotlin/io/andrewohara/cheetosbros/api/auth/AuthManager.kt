package io.andrewohara.cheetosbros.api.auth

import io.andrewohara.cheetosbros.api.users.SocialLink
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.javalin.core.security.AccessManager
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse
import java.lang.IllegalStateException
import javax.servlet.http.Cookie

class AuthManager(private val authorizationDao: AuthorizationDao, private val usersManager: UsersManager): AccessManager {

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
        val userId = authorizationDao.resolveUserId(sessionId) ?: return null
        return usersManager[userId]
    }

    private fun createSession(ctx: Context, user: User) {
        val token = authorizationDao.assignToken(user)

        val cookie = Cookie("cheetosbros-session-id", token).apply {
            isHttpOnly = true
        }
        ctx.cookie(cookie)
    }

    fun login(ctx: Context, socialLink: SocialLink) {
        val loggedInUser = ctx.attribute<User>("user")
        val linkedUser = usersManager[socialLink]

        when {
            linkedUser == null && loggedInUser == null -> {
                val newUser = usersManager.createUser()
                usersManager.linkSocialLogin(newUser, socialLink)

                createSession(ctx, newUser)
            }
            linkedUser == null && loggedInUser != null -> {
                usersManager.linkSocialLogin(loggedInUser, socialLink)
            }
            linkedUser != null && loggedInUser == null -> {
                createSession(ctx, linkedUser)
            }
            linkedUser != null && loggedInUser != null -> {
                if (linkedUser.id != loggedInUser.id) throw BadRequestResponse("User is already linked to another account")
            }
            else -> throw IllegalStateException()
        }
    }
}

enum class CheetosRole: Role {
    Public, User
}