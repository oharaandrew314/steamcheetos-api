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
            val progression: Progression,
            val rewards: Collection<Reward>
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
            val current: String?,
            val target: String,
            val operationType: String,  // known values are "Maximum",
            val valueType: String // known values are "Integer"
    )

    data class Reward(
            val name: String?,
            val description: String?,
            val value: String,
            val type: String, // known values are "Gamerscore"
            val mediaAsset: MediaAsset?,
            val valueType: String,  // known values are "Int"
    )
}