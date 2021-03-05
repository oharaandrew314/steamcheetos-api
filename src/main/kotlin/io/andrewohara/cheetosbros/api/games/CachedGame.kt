package io.andrewohara.cheetosbros.api.games

import io.andrewohara.cheetosbros.sources.Source
import java.time.Instant

data class CachedGame(
    val uid: Uid,
    val name: String,
    val displayImage: String?,

    val achievements: Int,
    val lastUpdated: Instant
) {
    companion object {
        fun create(game: Source.Game, achievements: Int, lastUpdated: Instant) = CachedGame(
            uid = game.uid,
            name = game.name,
            displayImage = game.displayImage,
            achievements = achievements,
            lastUpdated = lastUpdated
        )
    }
}