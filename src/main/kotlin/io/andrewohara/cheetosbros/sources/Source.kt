package io.andrewohara.cheetosbros.sources

import java.time.Instant

data class Game(
        val id: String,
        val platform: Platform,
        val name: String,
        val displayImage: String?,
        val icon: String?
) {
    enum class Platform { Steam, Xbox }
}

data class Achievement(
        val id: String,
        val name: String,
        val description: String?,
        val hidden: Boolean,
        val icons: List<String>
)

data class AchievementStatus(
        val id: String,
        val unlocked: Boolean,
        val unlockedOn: Instant?
)

interface Source {
    fun resolveUserId(username: String): String?
    fun games(userId: String): Collection<Game>
    fun achievements(appId: String): Collection<Achievement>
    fun userAchievements(appId: String, userId: String): Collection<AchievementStatus>
}