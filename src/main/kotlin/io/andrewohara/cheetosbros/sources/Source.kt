package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.Uid
import java.lang.Exception
import java.time.Instant

data class Player(
    val uid: Uid,
    val username: String,
    val avatar: String?,
    var token: String?
)

enum class Platform { Steam, Xbox }

data class GameData(
    val uid: Uid,
    val name: String,
    val displayImage: String?
)

data class Achievement(
    val id: String,
    val name: String,
    val description: String?,
    val hidden: Boolean,
    val icons: List<String>,
    val score: Int?
)

data class AchievementStatus(
    val achievementId: String,
    val unlockedOn: Instant?
)

interface Source {
    val platform: Platform

    fun getPlayer(playerId: String): Player?
    fun getFriends(playerId: String): Collection<String>
    fun library(playerId: String): Collection<GameData>
    fun achievements(gameId: String): Collection<Achievement>
    fun userAchievements(gameId: String, playerId: String): Collection<AchievementStatus>
}

class SourceAccessDenied(message: String): Exception(message)