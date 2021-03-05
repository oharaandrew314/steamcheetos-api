package io.andrewohara.cheetosbros.api.games

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.*

class GamesManager(
    private val gamesDao: GamesDao,
    private val gameLibraryDao: GameLibraryDao,
    private val achievementsDao: AchievementsDao,
    private val achievementStatusDao: AchievementStatusDao
) {
    fun listGames(user: User): Collection<OwnedGameDetails> {
        val library = user.players.values
            .flatMap { gameLibraryDao[it] }

        val games = gamesDao.batchGet(library.map { it.uid })
            .map { it.uid to it }
            .toMap()

        return library.mapNotNull { ownedGame ->
            val game = games[ownedGame.uid]
            if (game != null) {
                OwnedGameDetails(game, ownedGame)
            } else null
        }
    }

    fun getGame(player: Player, gameId: String): OwnedGameDetails? {
        val ownedGame = gameLibraryDao[player, gameId] ?: return null
        val game = gamesDao[ownedGame.uid] ?: return null

        return OwnedGameDetails(game, ownedGame)
    }

    fun listAchievements(user: User, platform: Platform, gameId: String): Collection<AchievementDetails>? {
        val game = gamesDao[Uid(platform, gameId)] ?: return null
        val player = user.players[platform] ?: return null

        val achievements = achievementsDao[game.uid]
        val progress = achievementStatusDao[player.uid, game.uid]
            .map { it.achievementId to it }
            .toMap()

        return achievements.map { achievement ->
            val unlockedOn = progress[achievement.id]?.unlockedOn
            AchievementDetails.create(achievement, unlockedOn)
        }
    }
}