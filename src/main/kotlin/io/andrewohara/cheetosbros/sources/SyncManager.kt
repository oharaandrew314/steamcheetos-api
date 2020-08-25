package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.v1.AchievementStatusDao
import io.andrewohara.cheetosbros.api.games.v1.AchievementsDao
import io.andrewohara.cheetosbros.api.games.v1.GameLibraryDao
import io.andrewohara.cheetosbros.api.games.v1.GamesDao
import io.andrewohara.cheetosbros.api.users.PlayersDao
import io.andrewohara.cheetosbros.api.users.User
import java.time.Instant

class SyncManager(
        private val sourceFactory: SourceFactory,
        private val gamesDao: GamesDao,
        private val achievementsDao: AchievementsDao,
        private val gameLibraryDao: GameLibraryDao,
        private val achievementStatusDao: AchievementStatusDao,
        private val playersDao: PlayersDao,
        private val time: () -> Instant
) {
    fun syncLibrary(user: User, player: Player) {
        val source = sourceFactory[user, player.platform] ?: return

        val games = source.library(player.id)
        gamesDao.batchSave(games)

        val library = games.map { LibraryItem(platform = source.platform, gameId = it.id) }
        gameLibraryDao.batchSave(player, library)

        playersDao.updateLibraryCacheDate(player, time())
    }

    fun syncFriends(user: User, player: Player) {
        val source = sourceFactory[user, player.platform] ?: return

        val friendIds = source.getFriends(player.id)
        playersDao.saveFriends(player, friendIds)

        // TODO update cache date
    }

//    private fun syncPlayer(id: String, source: Source) {
//        try {
//            val player = source.getPlayer(id) ?: return
//            playersDao.save(player)
//
//            println("Updated profile for (${player.platform}) ${player.username}")
//        } catch (e: SourceAccessDenied) {
//            println("ERROR: $e")
//        } catch (e: Exception) {
//            e.printStackTrace()
//            throw e
//        }
//    }

    fun syncAchievements(user: User, game: Game) {
        val source = sourceFactory[user, game.platform] ?: return

        val achievements = source.achievements(game.id)
        achievementsDao.batchSave(game, achievements)

        gamesDao.updateAchievementsCacheDate(game, time())
    }

    fun syncAchievementStatuses(user: User, game: Game, player: Player) {
        val source = sourceFactory[user, player.platform] ?: return

        val achievementStatuses = source.userAchievements(gameId = game.id, playerId = player.id)
        achievementStatusDao.batchSave(player, game, achievementStatuses)

        gameLibraryDao.updateAchievementStatusCacheDate(player, game, time())
    }
}