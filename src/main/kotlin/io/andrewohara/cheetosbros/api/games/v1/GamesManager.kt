package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.CacheConfig
import io.andrewohara.cheetosbros.api.users.PlayersDao
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.*
import java.time.Instant

class GamesManager(
        private val playersDao: PlayersDao,
        private val gamesDao: GamesDao,
        private val gameLibraryDao: GameLibraryDao,
        private val achievementsDao: AchievementsDao,
        private val achievementStatusDao: AchievementStatusDao,
        private val sourceManager: SourceManager,
        private val cacheConfig: CacheConfig,
        private val cacheDao: CacheDao,
        private val time: () -> Instant
) {
    fun listGames(user: User, platform: Platform? = null): Collection<Game> {
        return playersDao.listForUser(user)
                .filter { platform == null || it.platform == platform }
                .flatMap { player -> listGames(user, player) }
    }

    private fun listGames(user: User, player: Player): Collection<Game> {
        if (cacheDao.isLibraryCacheExpired(player)) {
            val library = sourceManager.getLibrary(user, player)
            gamesDao.batchSave(library)
            gameLibraryDao.batchSave(player, library.map { LibraryItem(platform = it.platform, gameId = it.id) })
            cacheDao.updateLibraryCache(player, time() + cacheConfig.library)
            return library
        }

        val ids = gameLibraryDao.listGameIds(player)
        return gamesDao.batchGet(player.platform, ids)
    }

    fun listAchievements(user: User, platform: Platform, gameId: String): Collection<Achievement>? {
        val game = gamesDao[platform, gameId] ?: return null

        if (cacheDao.isAchievementsCacheExpired(game)) {
            val achievements = sourceManager.getAchievements(user, game)
            achievementsDao.batchSave(game, achievements)
            cacheDao.updateAchievementsCache(game, time() + cacheConfig.achievements)
            return achievements
        }

        return achievementsDao.list(game)
    }

    fun listAchievementStatus(user: User, playerId: String, gameId: String): Collection<AchievementStatus>? {
        val player = getAuthorizedPlayer(user, playerId) ?: return null
        val game = gamesDao[player.platform, gameId] ?: return null

        if (cacheDao.isAchievementStatusCacheExpired(game, player)) {
            val achievementStatus = sourceManager.getAchievementStatus(user, game, player)
            achievementStatusDao.batchSave(player, game, achievementStatus)
            cacheDao.updateAchievementStatusCache(game, player, time() + cacheConfig.achievementStatuses)
            return achievementStatus
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