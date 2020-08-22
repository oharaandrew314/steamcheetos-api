package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.Player
import java.util.*

class UsersManager(private val usersDao: UsersDao, private val playersDao: PlayersDao, private val friendsDao: FriendsDao) {

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

    fun getFriends(user: User): Collection<Player> {
        val friends = friendsDao[user]

        val players = playersDao.batchGet(friends.map { it.uuid })
                .map { it.uuid to it }
                .toMap()

        return friends.map { friend ->
            players[friend.uuid] ?: Player(
                    id = friend.id,
                    platform = friend.platform,
                    avatar = null,
                    username = "Unknown Friend: ${friend.id}"
            )
        }
    }
}