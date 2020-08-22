package io.andrewohara.cheetosbros.sources

import java.time.Instant

data class Player(
        val id: String,
        val platform: Game.Platform,
        val username: String,
        val avatar: String?
) {
    val uuid = "$platform-$id"
}

data class Game(
        val id: String,
        val platform: Platform,
        val name: String,
        val displayImage: String?,
        val icon: String?
) {
    val uuid = "$platform-$id"

    enum class Platform { Steam, Xbox }
}

data class Achievement(
        val id: String,
        val name: String,
        val description: String?,
        val hidden: Boolean,
        val icons: List<String>,
        val score: Int?
)

data class UserGame(
        val gameUuid: String,
        val lastPlayed: Instant?
)

data class AchievementStatus(
        val achievementId: String,
        val unlockedOn: Instant?
) {
    val unlocked = unlockedOn != null
}

interface Source {
    fun getPlayer(id: String): Player?
    fun getFriends(userId: String): Collection<String>
    fun resolveUserId(username: String): String?
    fun games(userId: String): Collection<Game>
    fun achievements(appId: String): Collection<Achievement>
    fun userAchievements(appId: String, userId: String): Collection<AchievementStatus>
}