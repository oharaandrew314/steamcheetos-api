package io.andrewohara.cheetosbros.sources

import java.lang.Exception
import java.time.Instant

data class Player(
        val platform: Platform,
        val id: String,
        val username: String,
        val avatar: String?,
        var token: String?
) {
    fun uid() = "$platform-$id"
}

data class Game(
        val platform: Platform,
        val id: String,
        val name: String,
        val displayImage: String?
) {
    fun uid() = "$platform-$id"
}

enum class Platform { Steam, Xbox }

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
    fun library(playerId: String): Collection<Game>
    fun achievements(gameId: String): Collection<Achievement>
    fun userAchievements(gameId: String, playerId: String): Collection<AchievementStatus>
}

class SourceAccessDenied(message: String): Exception(message)