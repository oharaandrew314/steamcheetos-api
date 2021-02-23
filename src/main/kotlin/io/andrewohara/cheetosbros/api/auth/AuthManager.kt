package io.andrewohara.cheetosbros.api.auth

import io.andrewohara.cheetosbros.api.users.*
import io.andrewohara.cheetosbros.sources.Player
import spark.Filter
import spark.Request
import spark.Response
import spark.Spark
import java.lang.IllegalStateException
import java.util.*

class AuthManager(private val authorizationDao: AuthorizationDao, private val usersDao: UsersDao, private val socialLinkDao: SocialLinkDao): Filter {

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
                if (linkedUser.id != loggedInUser.id) throw Spark.halt(400, "User is already linked to another account")
                loggedInUser
            }
            else -> throw IllegalStateException()
        }

        return authorizationDao.assignToken(effectiveUser)
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

    override fun handle(request: Request, response: Response) {
        val token = request.headers("Authorization")?.replace("Bearer ", "") ?: return

        val userId = authorizationDao.resolveUserId(token) ?: return
        val user = usersDao[userId] ?: return

        request.attribute("user", user)
    }
}