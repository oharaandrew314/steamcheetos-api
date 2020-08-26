package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.api.CacheConfig
import io.andrewohara.cheetosbros.sources.CacheDao
import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.Player
import io.andrewohara.cheetosbros.sources.SourceManager
import java.time.Instant
import java.util.*

class UsersManager(
        private val usersDao: UsersDao,
        private val playersDao: PlayersDao,
        private val cacheConfig: CacheConfig,
        private val sourceManager: SourceManager,
        private val cacheDao: CacheDao,
        private val time: () -> Instant
) {

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

    fun getPlayer(user: User, platform: Platform): Player? {
        return playersDao.listForUser(user)
                .firstOrNull { it.platform == platform }
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
                .flatMap { getFriends(user, it) }
    }

    private fun getFriends(user: User, player: Player): Collection<Player> {
        if (cacheDao.isFriendsCacheExpired(player)) {
            val friendIds = sourceManager.getFriends(user, player)
            playersDao.saveFriends(player, friendIds)
            cacheDao.updateFriendsCache(player, time() + cacheConfig.friends)
        }

        return playersDao.getFriends(player) ?: emptyList()
    }
}