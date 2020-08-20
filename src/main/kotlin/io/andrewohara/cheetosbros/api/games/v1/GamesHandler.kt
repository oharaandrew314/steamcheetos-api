package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.GameDetails
import io.andrewohara.cheetosbros.sources.GameStatus
import io.andrewohara.cheetosbros.sources.openxbl.OpenXblSource
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import java.io.IOException
import java.lang.Exception
import kotlin.concurrent.thread

class GamesHandler(
        private val steamSource: SteamSource,
        private val gamesDao: GamesDao,
        private val gameStatusDao: GameStatusDao
) {

    fun listGames(user: User): Collection<GameDetails> {
        val userGames = gameStatusDao.list(user)
        return gamesDao[userGames.map { it.gameUuid }]
    }

    fun syncPlatform(user: User) {
        thread(start = true) {
            println("Syncing games for ${user.displayName}")
            try {
                syncPlatform(user, Game.Platform.Steam)
                syncPlatform(user, Game.Platform.Xbox)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun syncPlatform(user: User, platform: Game.Platform) {
        val socialLink = user.socialLinkForPlatform(platform) ?: return

        val source = when(platform) {
            Game.Platform.Steam -> steamSource
            Game.Platform.Xbox -> OpenXblSource(socialLink.token!!)
        }

        val ownedGames = source.games(socialLink.id)
        val existingGameIds = gamesDao[ownedGames.map { it.uuid }].map { it.game.uuid }
        for (game in ownedGames.filter { it.uuid !in existingGameIds }) {
            try {
                val achievements = source.achievements(game.id)
                val details = GameDetails(game = game, achievements = achievements)
                gamesDao.save(details)

                println("Saved new $platform game: ${game.name}")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        for (game in ownedGames) {
            val achievementStatuses = source.userAchievements(game.id, socialLink.id)
            val gameStatus = GameStatus(gameUuid = game.uuid, achievements = achievementStatuses)
            gameStatusDao.save(user, gameStatus)
            println("Updated ${achievementStatuses.size} user achievements for ${game.name}")
        }

        println("Synced ${ownedGames.size} games for user ${user.displayName} on $platform")
    }
}