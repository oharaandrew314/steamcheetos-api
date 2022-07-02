package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.games.Achievement
import io.andrewohara.cheetosbros.games.Game
import io.andrewohara.cheetosbros.sources.AchievementStatusData
import io.andrewohara.cheetosbros.sources.UserData
import org.http4k.core.Uri
import java.time.Instant

data class GameDtoV1(
    val id: String,
    val name: String,
    val achievementsTotal: Int?,
    val achievementsCurrent: Int?,
    val displayImage: String,
    val achievementsExpire: Instant,
    val favourite: Boolean,
)

data class AchievementDtoV1(
    val id: String,
    val name: String,
    val description: String?,
    val hidden: Boolean,
    val iconLocked: String,  // TODO only send current icon URL
    val iconUnlocked: String,

    val unlockedOn: Instant?,
    val unlocked: Boolean,

    val favourite: Boolean,
)

data class AchievementStatusDtoV1(
    val id: String,
    val unlockedOn: Instant?,
    val unlocked: Boolean
)

data class UserDtoV1(
    val id: String,
    val name: String,
    val avatar: Uri?
)

fun Achievement.toDtoV1() = AchievementDtoV1(
    id = id,
    name = name,
    description = description,
    hidden = hidden,
    iconLocked = iconLocked.toString(),
    iconUnlocked = iconUnlocked.toString(),
    unlocked = unlockedOn != null,
    unlockedOn = unlockedOn,
    favourite = favourite
)

fun Collection<Achievement>.toDtoV1s() = map { it.toDtoV1() }.toTypedArray()

fun Game.toDtoV1() = GameDtoV1(
    id = id,
    name = name,
    displayImage = displayImage.toString(),
    achievementsCurrent = achievementsUnlocked,
    achievementsTotal = achievementsTotal,
    achievementsExpire = progressExpires,
    favourite = favourite
)

fun UserData.toDtoV1() = UserDtoV1(
    id = id,
    name = username,
    avatar = avatar
)

fun AchievementStatusData.toDtoV1() = AchievementStatusDtoV1(
    id = achievementId,
    unlocked = unlockedOn != null,
    unlockedOn = unlockedOn
)

data class UpdateGameRequestV1(
    val favourite: Boolean
)

data class UpdateAchievementRequestV1(
    val favourite: Boolean
)