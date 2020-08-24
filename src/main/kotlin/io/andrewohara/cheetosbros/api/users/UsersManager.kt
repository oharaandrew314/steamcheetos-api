package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.Player
import java.util.*

class UsersManager(private val usersDao: UsersDao, private val playersDao: PlayersDao) {

    operator fun get(cheetosUserId: String): User? {
        return usersDao[cheetosUserId]
    }

    operator fun get(player: Player): User? {
        val userId = playersDao.getUserId(player) ?: return null
        return usersDao[userId]
    }

    fun createUser(displayName: String): User {
        val user = User(id = UUID.randomUUID().toString(), displayName = displayName)

        usersDao.save(user)

        return user
    }

    fun linkSocialLogin(user: User, player: Player, token: String?) {
        when(player.platform) {
            Platform.Xbox -> {
                usersDao.save(user.copy(openxblToken = token))
            }
            Platform.Steam -> {}
        }
        playersDao.save(player)
        playersDao.linkUser(player, user)
    }

    fun getFriends(user: User, platform: Platform? = null): Collection<Player>? {
        return playersDao.listForUser(user)
                .filter { platform == null || it.platform == platform }
                .flatMap { playersDao.getFriends(it) ?: emptyList() }
    }
}