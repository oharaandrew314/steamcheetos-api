package io.andrewohara.cheetosbros.sources.steam

data class GetOwnedGamesResponse(
        val response: GetOwnedGamesResponseData
) {
    data class GetOwnedGamesResponseData(
            val game_count: Int,
            val games: List<SteamGame>
    ) {
        data class SteamGame(
                val appid: Int,
                val name: String,
                val img_icon_url: String,
                val img_logo_url: String,
                val has_community_visible_stats: Boolean?,
                val playtime_forever: Int,
                val playtime_windows_forever: Int,
                val playtime_mac_forever: Int,
                val playtime_linux_forever: Int
        )
    }
}