package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.SocialLink
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.openxbl.OpenXblSource

class SourcesManager(
        private val steamSource: Source,
        private val gamesDao: GamesDao,
        private val achievementsDao: AchievementsDao,
        private val userGamesDao: UserGamesDao,
        private val achievementStatusDao: AchievementStatusDao,
) {
    private fun SocialLink.source() = when(platform) {
        Game.Platform.Xbox -> OpenXblSource(token!!)
        Game.Platform.Steam -> steamSource
    }

    fun discoverGames(user: User, platform: Game.Platform): Collection<Game> {
        val socialLink = user.socialLinkForPlatform(platform) ?: return emptyList()
        val source = socialLink.source()

        return source.games(socialLink.id)
    }

    fun syncGame(user: User, game: Game) {
        val socialLink = user.socialLinkForPlatform(game.platform) ?: return
        val source = socialLink.source()

        if (gamesDao[game.uuid] == null) {
            gamesDao.save(game)
            println("Saved new game: (${game.platform}) ${game.name}")
        }

        val achievements = source.achievements(game.id)
        achievementsDao.batchSave(game, achievements)

        val userGame = UserGame(gameUuid = game.uuid, lastPlayed = null)
        userGamesDao.save(user, userGame)

        val userAchievements = source.userAchievements(appId = game.id, userId = socialLink.id)
        achievementStatusDao.batchSave(user, game, userAchievements)

        println("Updated achievements for (${game.platform}) ${game.name}")
    }
}