package io.andrewohara.cheetosbros.api.games

import java.time.Instant

data class OwnedGameDetails(
    val uid: Uid,
    val name: String,
    val achievementsTotal: Int,
    val achievementsCurrent: Int,
    val displayImage: String?,
    val lastUpdated: Instant
) {
    constructor(game: CachedGame, ownedGame: OwnedGame): this(
        uid = game.uid,
        name = game.name,
        achievementsTotal = game.achievements,
        achievementsCurrent = ownedGame.achievements,
        displayImage = game.displayImage,
        lastUpdated = ownedGame.lastUpdated
    )
}