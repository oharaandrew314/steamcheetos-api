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
    fun listGames(user: User): Collection<Game> {
        val games = mutableSetOf<Game>()

        for (player in playersDao.listForUser(user)) {
            val gameIds = gameLibraryDao.listGameIds(player)
            games.addAll(gamesDao.batchGet(player.platform, gameIds))
        }

        // TODO maybe filter out games with no achievements
        // TODO add metadata to game library item for achievement progress

        return games
    }

    fun listGames(user: User, platform: Platform): Collection<Game> {
        val player = playersDao.listForUser(user)
            .firstOrNull { it.platform == platform }
            ?: return emptyList()

        val gameIds = gameLibraryDao.listGameIds(player)
        return gamesDao.batchGet(platform, gameIds)
    }

    fun listAchievements(platform: Platform, gameId: String): Collection<Achievement>? {
        val game = gamesDao[platform, gameId] ?: return null

        return achievementsDao[game]
    }

    fun listAchievementStatus(user: User, platform: Platform, gameId: String): Collection<AchievementStatus>? {
        val game = gamesDao[platform, gameId] ?: return null

        val player = playersDao.listForUser(user)
            .firstOrNull { it.platform == platform }
            ?: return null

        return achievementStatusDao[player, game]
    }

    fun getGame(platform: Platform, gameId: String): Game? {
        return gamesDao[platform, gameId]
    }
}