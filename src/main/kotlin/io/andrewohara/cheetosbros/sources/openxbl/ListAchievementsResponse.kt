package io.andrewohara.cheetosbros.sources.openxbl

import java.math.BigDecimal
import java.time.Instant

data class ListAchievementsResponse(
        val achievements: Collection<XboxAchievement>
) {

    data class XboxAchievement(
            val id: String,
            val name: String,
            val mediaAssets: Collection<MediaAsset>,
            val isSecret: Boolean,
            val description: String,
            val lockedDescription: String,
            val rarity: Rarity?,
            val progressState: String,
            val progression: Progression
    )

    data class MediaAsset(
            val name: String,
            val type: String,  // possible values so far are "Icon"
            val url: String
    )

    data class Rarity(
            val currentCategory: String, //possible values so far are "Rare"
            val currentPercentage: BigDecimal
    )

    data class Progression(
            val requirements: Collection<Requirements>,
            val timeUnlocked: Instant
    )

    data class Requirements(
            val current: String,
            val target: String,
            val operationType: String,  // known values are "Maximum",
            val valueType: String // known values are "Integer"
    )
}