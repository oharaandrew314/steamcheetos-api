package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Player

data class User(
        val id: String,
        val displayName: String,
        val xbox: Player? = null,
        val steam: Player? = null,
        val openxblToken: String? = null
) {
    fun playerForPlatform(platform: Game.Platform) = when(platform) {
        Game.Platform.Xbox -> xbox
        Game.Platform.Steam -> steam
    }

    fun withPlayer(player: Player, token: String?): User {
        return when(player.platform) {
            Game.Platform.Xbox -> copy(xbox = player, openxblToken = token)
            Game.Platform.Steam -> copy(steam = player)
        }
    }
}