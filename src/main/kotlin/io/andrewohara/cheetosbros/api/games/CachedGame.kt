package io.andrewohara.cheetosbros.api.games

import io.andrewohara.cheetosbros.sources.GameData
import java.time.Instant

data class CachedGame(
    val uid: Uid,
    val name: String,
    val displayImage: String?,

    val achievements: Int,
    val lastUpdated: Instant?
) {
    companion object {
        fun create(gameData: GameData, achievements: Int, lastUpdated: Instant?) = CachedGame(
            uid = gameData.uid,
            name = gameData.name,
            displayImage = gameData.displayImage,
            achievements = achievements,
            lastUpdated = lastUpdated
        )
    }
}