package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.games.Achievement
import io.andrewohara.cheetosbros.games.Game
import io.andrewohara.cheetosbros.sources.UserData
import org.http4k.core.Uri
import java.time.Instant

data class GameDtoV1(
    val id: String,
    val name: String,
    val achievementsTotal: Int,
    val achievementsCurrent: Int,
    val displayImage: String?,
    val lastUpdated: Instant
)

data class AchievementDtoV1(
    val id: String,
    val name: String,
    val description: String?,
    val hidden: Boolean,
    val icons: List<String>,
    val score: Int?,

    val unlockedOn: Instant?,
    val unlocked: Boolean
)

data class JobStatusDtoV1(
    val count: Int
)

data class UserDtoV1(
    val name: String,
    val avatar: Uri?
)

fun Achievement.toDtoV1() = AchievementDtoV1(
    id = id,
    name = name,
    description = description,
    hidden = hidden,
    score = score,
    icons = listOfNotNull(iconLocked, iconUnlocked).map { it.toString() },
    unlocked = unlockedOn != null,
    unlockedOn = unlockedOn
)

fun Game.toDtoV1() = GameDtoV1(
    id = id,
    name = name,
    lastUpdated = lastUpdated,
    displayImage = displayImage,
    achievementsCurrent = achievementsUnlocked,
    achievementsTotal = achievementsTotal
)

fun UserData.toDtoV1() = UserDtoV1(
    name = username,
    avatar = avatar
)