package io.andrewohara.cheetosbros.api.users

data class User(
        val id: String,
        val displayName: String,
        val xbox: XboxUser?,
        val steam: SteamUser?
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