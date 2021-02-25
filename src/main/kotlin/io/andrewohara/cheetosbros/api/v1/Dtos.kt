package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.api.games.v1.OwnedGame
import io.andrewohara.cheetosbros.sources.Platform

data class GameDtoV1(
    val platform: Platform,
    val uid: String,
    val name: String,
    val achievementsTotal: Int,
    val achievementsCurrent: Int,
    val displayImage: String?
) {
    constructor(ownedGame: OwnedGame): this(
        platform = ownedGame.platform,
        uid = ownedGame.gameUid(),
        name = ownedGame.name,
        achievementsTotal = ownedGame.totalAchievements,
        achievementsCurrent = ownedGame.currentAchievements,
        displayImage = ownedGame.displayImage
    )
}