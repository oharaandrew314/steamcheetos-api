package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.users.User

class SyncManager(private val syncExecutor: SyncExecutor, private val sourcesManager: SourcesManager) {

    fun sync(user: User) {
        sync(user, Game.Platform.Xbox)
        sync(user, Game.Platform.Steam)
    }

    private fun sync(user: User, platform: Game.Platform) {
        syncExecutor.run {
            val friends = sourcesManager.discoverFriends(user, platform)
            for (friend in friends) {
                syncExecutor.run {
                    sourcesManager.syncFriend(user, friend)
                }
            }
        }

        syncExecutor.run {
            val xboxGames = sourcesManager.discoverGames(user, platform)
            for (game in xboxGames) {
                syncExecutor.run {
                    sourcesManager.syncGame(user, game)
                }
            }
        }
    }
}