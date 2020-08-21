package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.users.User

class SyncManager(private val syncExecutor: SyncExecutor, private val sourcesManager: SourcesManager) {

    fun sync(user: User) {
        syncExecutor.run {
            val xboxGames = sourcesManager.discoverGames(user, Game.Platform.Xbox)
            for (game in xboxGames) {
                syncExecutor.run {
                    sourcesManager.syncGame(user, game)
                }
            }
        }
        syncExecutor.run {
            val steamGames = sourcesManager.discoverGames(user, Game.Platform.Steam)
            for (game in steamGames) {
                syncExecutor.run {
                    sourcesManager.syncGame(user, game)
                }
            }
        }
    }
}