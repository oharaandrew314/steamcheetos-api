package io.andrewohara.cheetosbros.api.auth

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.andrewohara.cheetosbros.sources.Player
import io.javalin.core.security.AccessManager
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse
import java.lang.IllegalStateException

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
        val token = ctx.cookie(ID_TOKEN_NAME) ?: return null
        val userId = authorizationDao.resolveUserId(token) ?: return null
        return usersManager[userId]
    }

    private fun createSession(ctx: Context, user: User) {
        val token = authorizationDao.assignToken(user)

        ctx.cookie(ID_TOKEN_NAME, token)
    }

    fun login(ctx: Context, player: Player, token: String?) {
        val loggedInUser = getAuthorizedUser(ctx)
        val linkedUser = usersManager[player]

        val effectiveUser = when {
            linkedUser == null && loggedInUser == null -> {
                usersManager.createUser(player, token)
            }
            linkedUser == null && loggedInUser != null -> {
                loggedInUser
            }
            linkedUser != null && loggedInUser == null -> {
                linkedUser
            }
            linkedUser != null && loggedInUser != null -> {
                if (linkedUser.id != loggedInUser.id) throw BadRequestResponse("User is already linked to another account")
                loggedInUser
            }
            else -> throw IllegalStateException()
        }

        usersManager.linkSocialLogin(effectiveUser, player, token)  // relinking will also update token
        createSession(ctx, effectiveUser)
    }

    companion object {
        private const val ID_TOKEN_NAME = "cheetosbros-id-token"
    }
}

enum class CheetosRole: Role {
    Public, User
}