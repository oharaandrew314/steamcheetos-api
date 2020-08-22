package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.FriendsDao
import io.andrewohara.cheetosbros.api.users.PlayersDao
import io.andrewohara.cheetosbros.api.users.User
import java.lang.Exception

class SourcesManager(
        private val sourceFactory: SourceFactory,
        private val gamesDao: GamesDao,
        private val achievementsDao: AchievementsDao,
        private val userGamesDao: UserGamesDao,
        private val achievementStatusDao: AchievementStatusDao,
        private val playersDao: PlayersDao,
        private val friendsDao: FriendsDao
) {
    private fun User.source(platform: Game.Platform) = sourceFactory[this, platform]

    fun discoverFriends(user: User, platform: Game.Platform): Collection<FriendsDao.Friend> {
        try {
            val player = user.playerForPlatform(platform) ?: return emptyList()
            val source = user.source(platform) ?: return emptyList()

            source.getPlayer(player.id)?.let { updatedPlayer ->
                playersDao.save(updatedPlayer)
                println("Updated ($platform) ${updatedPlayer.username}")
            }

            val friendIds = source.getFriends(player.id)
            val friends = user.syncFriends(platform, friendIds)

            println("Discovered ${friendIds.size} friends of ($platform) ${player.username}")
            return friends
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun User.syncFriends(platform: Game.Platform, friendIds: Collection<String>): Collection<FriendsDao.Friend> {
        val oldFriends = friendsDao[this, platform].toHashSet()
        val currentFriends = friendIds.map { FriendsDao.Friend(id = it, platform = platform) }.toHashSet()

        for (oldFriend in oldFriends) {
            if (oldFriend !in currentFriends) {
                friendsDao.remove(this, oldFriend)
            }
        }

        for (currentFriend in currentFriends) {
            if (currentFriend !in oldFriends) {
                friendsDao.add(this, currentFriend)
            }
        }

        return currentFriends
    }

    fun syncFriend(user: User, friend: FriendsDao.Friend) {
        try {
            val source = user.source(friend.platform) ?: return
            val player = source.getPlayer(friend.id) ?: return
            playersDao.save(player)
            println("Updated (${friend.platform}) ${player.username}")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun discoverGames(user: User, platform: Game.Platform): Collection<Game> {
        try {
            val player = user.playerForPlatform(platform) ?: return emptyList()
            val source = user.source(platform) ?: return emptyList()

            return source.games(player.id)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun syncGame(user: User, game: Game) {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}