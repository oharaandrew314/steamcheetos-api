package io.andrewohara.cheetosbros.sync

import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Player

data class SyncMessage(
    val player: Player,
    val game: Game? = null
)