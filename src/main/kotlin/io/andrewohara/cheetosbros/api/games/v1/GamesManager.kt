package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.*

class GamesManager(
        private val gamesDao: GamesDao,
        private val gameLibraryDao: GameLibraryDao,
        private val achievementsDao: AchievementsDao,
        private val achievementStatusDao: AchievementStatusDao
) {
    fun listGames(user: User): Collection<OwnedGame> {
        return user.players.values
            .flatMap { gameLibraryDao[it] }
    }

    fun getGame(player: Player, gameId: String): OwnedGame? {
        return gameLibraryDao[player, gameId]
    }

    fun listAchievements(platform: Platform, gameId: String): Collection<Achievement>? {
        val game = gamesDao[platform, gameId] ?: return null

        return achievementsDao[game]
    }

    fun listAchievementStatus(user: User, platform: Platform, gameId: String): Collection<AchievementStatus>? {
        val game = gamesDao[platform, gameId] ?: return null
        val player = user.players[platform] ?: return null

        return achievementStatusDao[player, game]
    }
}