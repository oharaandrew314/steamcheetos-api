package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.games.Achievement
import io.andrewohara.cheetosbros.games.Game
import io.andrewohara.cheetosbros.sources.UserData
import org.http4k.core.Uri
import java.time.Instant

data class GameDtoV1(
    val id: String,
    val name: String,
    val achievementsTotal: Int?,
    val achievementsCurrent: Int?,
    val displayImage: Uri,
    val achievementsExpire: Instant,
    val favourite: Boolean,
)

data class AchievementDtoV1(
    val id: String,
    val name: String,
    val description: String?,
    val hidden: Boolean,
    val iconLocked: Uri?,
    val iconUnlocked: Uri?,

    val unlockedOn: Instant?,
    val unlocked: Boolean,

    val favourite: Boolean,
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
    iconLocked = iconLocked,
    iconUnlocked = iconUnlocked,
    unlocked = unlockedOn != null,
    unlockedOn = unlockedOn,
    favourite = favourite
)

fun Collection<Achievement>.toDtoV1s() = map { it.toDtoV1() }

fun Game.toDtoV1() = GameDtoV1(
    id = id,
    name = name,
    displayImage = displayImage,
    achievementsCurrent = achievementsUnlocked,
    achievementsTotal = achievementsTotal,
    achievementsExpire = progressExpires,
    favourite = favourite
)

fun UserData.toDtoV1() = UserDtoV1(
    name = username,
    avatar = avatar
)

data class UpdateGameRequestV1(
    val favourite: Boolean
)

data class UpdateAchievementRequestV1(
    val favourite: Boolean
)