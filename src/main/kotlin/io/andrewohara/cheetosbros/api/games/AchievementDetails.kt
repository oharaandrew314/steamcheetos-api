package io.andrewohara.cheetosbros.api.games

import io.andrewohara.cheetosbros.sources.Achievement
import java.time.Instant

data class AchievementDetails(
    val id: String,
    val name: String,
    val description: String?,
    val hidden: Boolean,
    val icons: List<String>,
    val score: Int?,

    val unlockedOn: Instant? = null,
) {
    val unlocked = unlockedOn != null

    companion object {
        fun create(achievement: Achievement, unlockedOn: Instant?) = AchievementDetails(
            id = achievement.id,
            name = achievement.name,
            description = achievement.description,
            hidden = achievement.hidden,
            icons = achievement.icons,
            score = achievement.score,
            unlockedOn = unlockedOn
        )
    }
}