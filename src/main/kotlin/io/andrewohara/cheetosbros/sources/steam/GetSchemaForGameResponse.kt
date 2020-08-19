package io.andrewohara.cheetosbros.sources.steam

data class GetSchemaForGameResponse(
        val game: GetSchemaForGameResponseContent
) {
    data class GetSchemaForGameResponseContent(
            val gameName: String?,
            val gameVersion: String?,
            val availableGameStats: GetSchemaForGameResponseStats?
    ) {
        data class GetSchemaForGameResponseStats(
                val achievements: List<SteamAchievement>?,
                val stats: List<SteamStat>?
        ) {
            data class SteamAchievement(
                    val name: String,
                    val defaultvalue: Long,
                    val displayName: String,
                    val hidden: Int,
                    val description: String?,
                    val icon: String,
                    val icongray: String
            )

            data class SteamStat(
                    val name: String,
                    val defaultvalue: Long?,
                    val displayName: String?
            )
        }
    }
}