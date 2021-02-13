package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.users.PlayersDao
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.*

class GamesManager(
        private val playersDao: PlayersDao,
        private val gamesDao: GamesDao,
        private val gameLibraryDao: GameLibraryDao,
        private val achievementsDao: AchievementsDao,
        private val achievementStatusDao: AchievementStatusDao
) {
    fun listGames(user: User, platform: Platform): Collection<Game> {
        val player = playersDao.listForUser(user)
            .firstOrNull { it.platform == platform }
            ?: return emptyList()

        val ids = gameLibraryDao.list(player).map { it.gameId }
        return gamesDao.batchGet(player.platform, ids)
    }

    fun listAchievements(platform: Platform, gameId: String): Collection<Achievement>? {
        val game = gamesDao[platform, gameId] ?: return null

        return achievementsDao.get(game)
    }

    fun listAchievementStatus(user: User, platform: Platform, gameId: String): Collection<AchievementStatus>? {
        val game = gamesDao[platform, gameId] ?: return null

        val player = playersDao.listForUser(user)
            .firstOrNull { it.platform == platform }
            ?: return null

        return achievementStatusDao.get(player, game)
    }

    fun getGame(platform: Platform, gameId: String): Game? {
        return gamesDao[platform, gameId]
    }
}