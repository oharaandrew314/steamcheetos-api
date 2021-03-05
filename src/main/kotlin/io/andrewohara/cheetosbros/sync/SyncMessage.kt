package io.andrewohara.cheetosbros.sync

import io.andrewohara.cheetosbros.sources.Player
import io.andrewohara.cheetosbros.sources.Source

data class SyncMessage(
    val player: Player,
    val game: Source.Game? = null
)