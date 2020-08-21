package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.users.User
import java.util.concurrent.Executors

interface SyncExecutor {

    fun sync(user: User)
}

class LocalSyncExecutor(private val sourcesHandler: SourcesManager, threads: Int = 20): SyncExecutor {

    private val executor = Executors.newFixedThreadPool(threads)

    override fun sync(user: User) {
        executor.run {
            val xboxGames = sourcesHandler.discoverGames(user, Game.Platform.Xbox)
            for (game in xboxGames) {
                executor.run {
                    sourcesHandler.syncGame(user, game)
                }
            }
        }
        executor.run {
            val steamGames = sourcesHandler.discoverGames(user, Game.Platform.Steam)
            for (game in steamGames) {
                executor.run {
                    sourcesHandler.syncGame(user, game)
                }
            }
        }
    }
}