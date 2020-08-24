package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.v1.AchievementStatusDao
import io.andrewohara.cheetosbros.api.games.v1.AchievementsDao
import io.andrewohara.cheetosbros.api.games.v1.GameLibraryDao
import io.andrewohara.cheetosbros.api.games.v1.GamesDao
import io.andrewohara.cheetosbros.api.users.PlayersDao
import io.andrewohara.cheetosbros.api.users.User
import java.lang.Exception

class SyncManager(
        private val sourceFactory: SourceFactory,
        private val syncExecutor: SyncExecutor,
        private val gamesDao: GamesDao,
        private val achievementsDao: AchievementsDao,
        private val gameLibraryDao: GameLibraryDao,
        private val achievementStatusDao: AchievementStatusDao,
        private val playersDao: PlayersDao
) {
    fun sync(user: User) {
        for (platform in Platform.values()) {
            sync(user, platform)
        }
    }

//    fun syncGame(user: User, platform: Platform, gameId: String) {
//        val source = sourceFactory[user, platform] ?: return
//
//        syncGame(gameId, source)
//    }

    private fun sync(user: User, platform: Platform) {
        val playerId = playersDao.listForUser(user)
                .firstOrNull { it.platform == platform }
                ?.id
                ?: return
        val source = sourceFactory[user, platform] ?: return

        syncExecutor.run {
            val library = syncLibrary(playerId, source)
            for (libraryItem in library) {
                syncGame(libraryItem.gameId, source)
            }

            val friendIds = syncFriends(playerId, source)

            for (currentPlayerId in (friendIds + playerId)) {
                syncExecutor.run {
                    syncPlayer(currentPlayerId, source)
                }

                for (libraryItem in library) {
                    syncExecutor.run {
                        syncAchievements(currentPlayerId, libraryItem.gameId, source)
                    }
                }
            }
        }
    }

    private fun syncFriends(playerId: String, source: Source): Collection<String> {
        try {
            val player = playersDao[source.platform, playerId] ?: return emptyList()

            val friendIds = source.getFriends(playerId)
            playersDao.saveFriends(player, friendIds)

            println("Discovered ${friendIds.size} friends of (${source.platform}) ${player.username}")
            return friendIds
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun syncPlayer(id: String, source: Source) {
        try {
            val player = source.getPlayer(id) ?: return
            playersDao.save(player)

            println("Updated profile for (${player.platform}) ${player.username}")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun syncLibrary(playerId: String, source: Source): Collection<LibraryItem> {
        try {
            val player = playersDao[source.platform, playerId] ?: return emptyList()

            val games = source.library(player.id)
            gamesDao.batchSave(games)

            val library = games.map { LibraryItem(platform = source.platform, gameId = it.id) }
            gameLibraryDao.batchSave(player, library)

            println("Synced ${library.size} games for (${player.platform}) ${player.username}")
            return library

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun syncGame(gameId: String, source: Source) {
        try {
            val game = gamesDao[source.platform, gameId] ?: return

            val achievements = source.achievements(game.id)
            achievementsDao.batchSave(game, achievements)

            println("Updated ${achievements.size} achievements for game: (${game.platform}) ${game.name}")
        } catch(e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun syncAchievements(playerId: String, gameId: String, source: Source) {
        try {
            val game = gamesDao[source.platform, gameId] ?: return
            val player = playersDao[source.platform, playerId] ?: return

            val achievements = source.userAchievements(gameId = gameId, playerId = playerId)
            achievementStatusDao.batchSave(player, game, achievements)

            println("Updated ${game.name} achievements for (${player.platform}) ${player.username}")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}