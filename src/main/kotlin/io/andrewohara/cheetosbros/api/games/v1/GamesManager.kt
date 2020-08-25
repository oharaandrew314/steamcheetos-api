package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.CacheConfig
import io.andrewohara.cheetosbros.api.users.PlayersDao
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.*
import java.time.Duration
import java.time.Instant

class GamesManager(
        private val playersDao: PlayersDao,
        private val gamesDao: GamesDao,
        private val gameLibraryDao: GameLibraryDao,
        private val achievementsDao: AchievementsDao,
        private val achievementStatusDao: AchievementStatusDao,
        private val syncManager: SyncManager,
        private val cacheConfig: CacheConfig,
        private val time: () -> Instant
) {
    fun listGames(user: User, platform: Platform? = null): Collection<Game> {
        return playersDao.listForUser(user)
                .filter { platform == null || it.platform == platform }
                .flatMap { player -> listGames(user, player) }
    }

    private fun listGames(user: User, player: Player): Collection<Game> {
        val libraryCacheDate = playersDao.getLibraryCacheDate(player)
        if (libraryCacheDate == null || Duration.between(libraryCacheDate, time()) > cacheConfig.library) {
            syncManager.syncLibrary(user, player)
        }

        val ids = gameLibraryDao.listGameIds(player)
        return gamesDao.batchGet(player.platform, ids)
    }

    fun listAchievements(user: User, platform: Platform, gameId: String): Collection<Achievement>? {
        val game = gamesDao[platform, gameId] ?: return null

        val achievementsCacheDate = gamesDao.getAchievementsCacheDate(game)
        if (achievementsCacheDate == null || Duration.between(achievementsCacheDate, time()) > cacheConfig.achievements) {
            syncManager.syncAchievements(user, game)
        }

        return achievementsDao.list(game)
    }

    fun listAchievementStatus(user: User, playerId: String, gameId: String): Collection<AchievementStatus>? {
        val player = getAuthorizedPlayer(user, playerId) ?: return null
        val game = gamesDao[player.platform, gameId] ?: return null

        val cacheDate = gameLibraryDao.getAchievementStatusCacheDate(player, game)
        if (cacheDate == null || Duration.between(cacheDate, time()) > cacheConfig.achievementStatuses) {
            syncManager.syncAchievementStatuses(user, game, player)
        }

        return achievementStatusDao.list(player, game)
    }

    fun getGame(platform: Platform, gameId: String): Game? {
        return gamesDao[platform, gameId]
    }

    private fun getAuthorizedPlayer(user: User, playerId: String): Player? {
        val userPlayers = playersDao.listForUser(user)

        for (userPlayer in userPlayers) {
            if (userPlayer.id == playerId) return userPlayer

            val friend = playersDao.getFriends(userPlayer)?.firstOrNull { it.id == playerId }
            if (friend != null) return friend
        }

        return null
    }
}