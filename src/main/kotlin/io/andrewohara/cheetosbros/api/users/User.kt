package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.Game

data class User(
        val id: String,
        var displayName: String? = null,
        var xbox: XboxUser? = null,
        var steam: SteamUser? = null
) {
    fun userIdForPlatform(platform: Game.Platform) = when(platform) {
        Game.Platform.Xbox -> xbox?.xuid
        Game.Platform.Steam -> steam?.steamId64?.toString()
    }
}

data class XboxUser(
        val xuid: String,
        val gamertag: String,
        val token: String
)

data class SteamUser(
        val steamId64: Long,
        val username: String
)