package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.openxbl.OpenXblSource

class SourcesManager(
        private val steamSource: Source,
        private val gamesDao: GamesDao,
        private val achievementsDao: AchievementsDao,
        private val userGamesDao: UserGamesDao,
        private val achievementStatusDao: AchievementStatusDao,
) {
    private fun User.source(platform: Game.Platform): Source? = when(platform) {
        Game.Platform.Xbox -> openxblToken?.let { OpenXblSource(it) }
        Game.Platform.Steam -> steamSource
    }

    fun discoverGames(user: User, platform: Game.Platform): Collection<Game> {
        val player = user.playerForPlatform(platform) ?: return emptyList()
        val source = user.source(platform) ?: return emptyList()

        return source.games(player.id)
    }

    fun syncGame(user: User, game: Game) {
        val player = user.playerForPlatform(game.platform) ?: return
        val source = user.source(game.platform) ?: return

        if (gamesDao[game.uuid] == null) {
            gamesDao.save(game)
            println("Saved new game: (${game.platform}) ${game.name}")
        }

        val achievements = source.achievements(game.id)
        achievementsDao.batchSave(game, achievements)

        val userGame = UserGame(gameUuid = game.uuid, lastPlayed = null)  // TODO save last played
        userGamesDao.save(user, userGame)

        val userAchievements = source.userAchievements(appId = game.id, userId = player.id)
        achievementStatusDao.batchSave(user, game, userAchievements)

        println("Updated achievements for (${game.platform}) ${game.name}")
    }
}