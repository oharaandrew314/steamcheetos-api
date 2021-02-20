package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.sources.Platform

data class OwnedGame(
    val platform: Platform,
    val id: String,
    val name: String,
    val displayImage: String?,
    val currentAchievements: Int,
    val totalAchievements: Int
)