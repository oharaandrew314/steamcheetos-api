package io.andrewohara.cheetosbros

import io.andrewohara.cheetosbros.games.*
import io.andrewohara.cheetosbros.sources.AchievementStatusData
import io.andrewohara.cheetosbros.sources.UserData
import io.andrewohara.cheetosbros.sources.steam.SteamClient
import org.http4k.core.Uri
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class CheetosService(
    private val gamesDao: GamesDao,
    private val achievementsDao: AchievementsDao,
    private val steam: SteamClient,
    private val clock: Clock,
    private val achievementDataRetention: Duration,
    private val progressRetention: Duration,
    private val recentGameLimit: Int,
    private val syncTimeout: Duration,
    private val executor: ExecutorService,
    private val imageCdnHost: Uri
) {

    fun getUser(userId: String): UserData? {
        return steam.getPlayer(userId)
    }

    fun listGames(userId: String): Collection<Game> {
        return gamesDao[userId].withImageHost(imageCdnHost)
    }

    fun refreshGames(userId: String): List<Game> {
        val existingIds = gamesDao[userId].map { it.id }.toSet()
        val time = clock.instant()

        val games = steam.listOwnedGames(userId.toLong())
            .filter { it.id !in existingIds }
            .map { gameData -> gameData.toGame(userId, time) }
        gamesDao += games

        steam.listRecentGameIds(userId, recentGameLimit)
            .map { gameId -> executor.submit { refreshAchievements(userId, gameId) } }
            .mapNotNull { task ->
                try {
                    task.get(syncTimeout.seconds, TimeUnit.SECONDS)
                } catch (e: TimeoutException) {
                    null
                }
            }


        return gamesDao[userId].withImageHost(imageCdnHost)
    }

    fun updateGame(userId: String, gameId: String, favourite: Boolean): Game? {
        return gamesDao[userId, gameId]
            ?.copy(favourite = favourite)
            ?.also { gamesDao += it }
    }

    fun listAchievements(userId: String, gameId: String): Collection<Achievement> {
        return achievementsDao[userId, gameId].witImageHost(imageCdnHost)
    }

    fun listAchievementStatus(gameId: String, userId: String): Collection<AchievementStatusData> {
        return steam.userAchievements(gameId = gameId.toLong(), playerId = userId.toLong())
    }

    fun refreshAchievements(userId: String, gameId: String): List<Achievement>? {
        val game = gamesDao[userId, gameId] ?: return null
        return refreshAchievements(game).witImageHost(imageCdnHost)
    }

    fun updateAchievement(userId: String, gameId: String, achievementId: String, favourite: Boolean): Achievement? {
        return achievementsDao[userId, gameId, achievementId]
            ?.copy(favourite = favourite)
            ?.also { achievementsDao += it }
    }

    private fun refreshAchievements(game: Game): List<Achievement> {
        val time = clock.instant()

        val refreshAchievementData = game.achievementDataExpires >= time
        val achievementData = if (refreshAchievementData) {
            steam.achievements(game.id.toLong())
        } else {
            achievementsDao[game.userId, game.id].map { it.toData() }
        }

        val progressData = steam.userAchievements(playerId = game.userId.toLong(), gameId = game.id.toLong())
            .associateBy { it.achievementId }

        val updatedGame = game.copy(
            achievementsTotal = progressData.size,
            achievementsUnlocked = progressData.values.count { it.unlockedOn != null },
            achievementDataExpires = if (refreshAchievementData) (time + achievementDataRetention) else game.achievementDataExpires,
            progressExpires = time + progressRetention
        )
        val achievements = achievementData
            .map { data -> data.toAchievement(game.userId, unlockedOn = progressData[data.id]?.unlockedOn) }

        achievementsDao += achievements
        gamesDao += updatedGame

        return achievements
    }

    fun getFriends(userId: String): Collection<UserData> {
        val friendIds = steam.getFriends(userId)
        return steam.getPlayers(friendIds)
    }
}