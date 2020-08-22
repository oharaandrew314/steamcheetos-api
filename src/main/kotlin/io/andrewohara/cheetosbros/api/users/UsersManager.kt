package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.Player
import java.util.*

class UsersManager(private val usersDao: UsersDao, private val playersDao: PlayersDao) {

    operator fun get(cheetosUserId: String): User? {
        return usersDao[cheetosUserId]
    }

    operator fun get(player: Player): User? {
        return usersDao[player.platform, player.id]
    }

    fun createUser(player: Player, token: String?): User {
        val user = User(id = UUID.randomUUID().toString(), displayName = player.username)
                .withPlayer(player, token)

        usersDao.save(user)
        playersDao.save(player)

        return user
    }

    fun linkSocialLogin(user: User, player: Player, token: String?): User {
        val updated = user.withPlayer(player, token)

        usersDao.save(updated)
        playersDao.save(player)

        return updated
    }
}