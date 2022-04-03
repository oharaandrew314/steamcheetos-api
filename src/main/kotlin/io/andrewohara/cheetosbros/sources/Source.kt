package io.andrewohara.cheetosbros.sources

import org.http4k.core.Uri
import java.lang.Exception
import java.time.Instant

data class UserData(
    val id: String,
    val username: String,
    val avatar: Uri?
)

data class GameData(
    val id: String,
    val name: String,
    val displayImage: String?
)

data class AchievementStatusData(
    val achievementId: String,
    val unlockedOn: Instant?
)

data class AchievementData(
    val gameId: String,
    val id: String,

    val name: String,
    val description: String?,
    val hidden: Boolean,
    val iconLocked: Uri?,
    val iconUnlocked: Uri?,
    val score: Int?,
)

class SourceAccessDenied(message: String): Exception(message)