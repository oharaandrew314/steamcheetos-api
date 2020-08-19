package io.andrewohara.cheetosbros.api.users

data class User(
        val id: String,
        var displayName: String? = null,
        var xbox: XboxUser? = null,
        var steam: SteamUser? = null
)

data class XboxUser(
        val xuid: String,
        val gamertag: String,
        val token: String
)

data class SteamUser(
        val steamId64: Long,
        val username: String
)