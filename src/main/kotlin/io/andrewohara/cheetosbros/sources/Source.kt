package io.andrewohara.cheetosbros.sources

import java.time.Instant

data class Player(
        val id: String,
        val platform: Game.Platform,
        val displayName: String,
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
        val gameId: String,
        val name: String,
        val description: String?,
        val hidden: Boolean,
        val icons: List<String>
) {
    val uuid = "$gameId-$id"
}

data class GameStatus(
        val gameUuid: String,
        val achievements: Collection<AchievementStatus>
)

data class AchievementStatus(
        val id: String,
        val unlockedOn: Instant?
) {
    val unlocked = unlockedOn != null
}

data class GameDetails(
        val game: Game,
        val achievements: Collection<Achievement>
)

interface Source {
    fun resolveUserId(username: String): String?
    fun games(userId: String): Collection<Game>
    fun achievements(appId: String): Collection<Achievement>
    fun userAchievements(appId: String, userId: String): Collection<AchievementStatus>
}