package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.Player

data class User(
        val id: String,
        val players: Map<Platform, Player>
) {
        fun displayName() = players.values.firstOrNull()?.username ?: "user-$id"
}