package io.andrewohara.cheetosbros.api.games

import java.time.Instant

data class OwnedGame(
    val uid: Uid,
    val achievements: Int,
    val lastUpdated: Instant
)