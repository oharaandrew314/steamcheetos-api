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
    fun listGames(user: User, platform: Platform? = null): Collection<Game> {
        return playersDao.listForUser(user)
                .filter { platform == null || it.platform == platform }
                .flatMap { player ->
                    val ids = gameLibraryDao.listGameIds(player)
                    gamesDao.batchGet(player.platform, ids)
                }
    }

    fun listAchievements(platform: Platform, gameId: String): Collection<Achievement>? {
        val game = gamesDao[platform, gameId] ?: return null

        return achievementsDao.list(game)
    }

    fun listAchievementStatus(user: User, playerId: String, gameId: String): Collection<AchievementStatus>? {
        val player = getAuthorizedPlayer(user, playerId) ?: return null
        val game = gamesDao[player.platform, gameId] ?: return null

        val achievements = achievementsDao.list(game)
        val statuses = achievementStatusDao.list(player, game)
                .map { it.achievementId to it }
                .toMap()

        return achievements.map { achievement ->
            statuses[achievement.id] ?: AchievementStatus(achievementId = achievement.id, unlockedOn = null)
        }
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