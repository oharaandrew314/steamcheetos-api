package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.api.games.AchievementDetails
import io.andrewohara.cheetosbros.api.games.OwnedGameDetails
import io.andrewohara.cheetosbros.api.games.Uid
import org.mapstruct.Mapper
import java.time.Instant

data class OwnedGameDetailsDtoV1(
    val uid: Uid,
    val name: String,
    val achievementsTotal: Int,
    val achievementsCurrent: Int,
    val displayImage: String?,
    val lastUpdated: Instant
)

data class AchievementDetailsDtoV1(
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

@Mapper
interface DtoMapper {
    fun toDtoV1(gameDetails: OwnedGameDetails): OwnedGameDetailsDtoV1
    fun toDtoV1(details: AchievementDetails): AchievementDetailsDtoV1
}