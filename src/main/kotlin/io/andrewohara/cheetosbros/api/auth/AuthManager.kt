package io.andrewohara.cheetosbros.api.auth

import io.andrewohara.cheetosbros.api.users.*
import io.andrewohara.cheetosbros.sources.Player
import io.javalin.core.security.AccessManager
import io.javalin.core.security.Role
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse
import spark.Filter
import spark.Request
import spark.Response
import java.lang.IllegalStateException
import java.util.*

class AuthManager(private val authorizationDao: AuthorizationDao, private val usersDao: UsersDao, private val socialLinkDao: SocialLinkDao): AccessManager, Filter {

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
        return usersDao[userId]
    }

    private fun createSession(ctx: Context, user: User) {
        val token = authorizationDao.assignToken(user)

        ctx.cookie(ID_TOKEN_NAME, token)
    }

    fun login(ctx: Context, player: Player) {
        val loggedInUser = getAuthorizedUser(ctx)
        val linkedUser = lookup(player)

        val effectiveUser = when {
            linkedUser == null && loggedInUser == null -> {
                createUser(player)
            }
            linkedUser == null && loggedInUser != null -> {
                linkUser(loggedInUser, player)
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

        createSession(ctx, effectiveUser)
    }

    fun assignSessionToken(loggedInUser: User?, player: Player): String {
        val linkedUser = lookup(player)

        val effectiveUser = when {
            linkedUser == null && loggedInUser == null -> {
                createUser(player)
            }
            linkedUser == null && loggedInUser != null -> {
                linkUser(loggedInUser, player)
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

        return authorizationDao.assignToken(effectiveUser)
    }

    fun getUser(token: String): User? {
        val userId = authorizationDao.resolveUserId(token) ?: return null
        return usersDao[userId]
    }

    private fun createUser(player: Player): User {
        val user = User(id = UUID.randomUUID().toString(), players = mapOf(player.platform to player))

        usersDao.save(user)
        socialLinkDao.link(player, user)

        return user
    }

    private fun linkUser(user: User, player: Player): User {
        val updated = user.copy(players = user.players + mapOf(player.platform to player))
        usersDao.save(updated)
        socialLinkDao.link(player, updated)

        return updated
    }

    private fun lookup(player: Player): User? {
        val userId = socialLinkDao.lookupUserId(player) ?: return null
        return usersDao[userId]
    }

    companion object {
        const val ID_TOKEN_NAME = "session"
    }

    override fun handle(request: Request, response: Response) {
        val auth = request.cookie(ID_TOKEN_NAME)
            ?: request.headers(ID_TOKEN_NAME)?.replace("Authorization ", "")
            ?: return
        val user = getUser(auth) ?: return
        request.attribute("user", user)
    }
}

enum class CheetosRole: Role {
    Public, User
}