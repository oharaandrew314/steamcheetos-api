package io.andrewohara.cheetosbros.sources.steam

data class GetPlayerSummariesResponse(
        val response: GetPlayerSummariesResponseData
)

data class GetPlayerSummariesResponseData(
        val players: Set<PlayerSummary>
)

data class PlayerSummary(
        val steamid: String,
        val communityvisibilitystate: Int,
        val profilestate: Int,
        val personaname: String,
        val lastlogoff: Int,
        val profileurl: String,
        val avatar: String,
        val avatarmedium: String,
        val avatarfull: String
)