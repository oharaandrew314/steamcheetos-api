package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.sources.Platform

data class OwnedGame(
    val platform: Platform,
    val id: String,
    val name: String,  // TODO remove
    val displayImage: String?, // TODO remove
    val currentAchievements: Int,
    val totalAchievements: Int // TODO remove
) {
    fun gameUid() = "$platform-$id"  // TODO remove
}