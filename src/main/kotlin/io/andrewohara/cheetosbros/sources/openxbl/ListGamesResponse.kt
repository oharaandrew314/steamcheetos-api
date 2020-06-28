package io.andrewohara.cheetosbros.sources.openxbl

import java.time.Instant

data class ListGamesResponse(
        val xuid: String,
        val titles: Collection<Title>
)

data class Title(
        val titleId: String,
        val name: String,
        val displayImage: String?,
        val type: String, // may want to filter on "Game"
        val achievement: TitleAchievements,
        val titleHistory: TitleHistory
)

data class TitleAchievements(
        val currentAchievements: Int,
        val totalAchievements: Int,
        val currentGamerscore: Int,
        val totalGamerscore: Int,
        val progressPercentage: Int
)

data class TitleHistory(
    val lastTimePlayed: Instant,
    val visible: Boolean,
    val canHide: Boolean
)