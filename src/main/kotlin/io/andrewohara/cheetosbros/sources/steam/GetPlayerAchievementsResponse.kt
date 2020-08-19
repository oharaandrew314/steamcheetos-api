package io.andrewohara.cheetosbros.sources.steam

data class GetPlayerAchievementsResponse(
        val playerstats: PlayerStats
) {
    data class PlayerStats(
            val steamID: Long,
            val gameName: String,
            val achievements: List<Achievement>?
    ) {
        data class Achievement(
                val apiname: String,
                val achieved: Int,
                val unlocktime: Long
        )
    }
}