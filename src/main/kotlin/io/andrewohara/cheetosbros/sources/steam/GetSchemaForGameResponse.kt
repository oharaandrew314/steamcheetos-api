package io.andrewohara.cheetosbros.sources.steam

data class GetSchemaForGameResponse(
    val game: Content
) {
    data class Content(
        val gameName: String?,
        val gameVersion: String?,
        val availableGameStats: Data?
    ) {
        data class Data(
            val achievements: List<Achievement>?,
            val stats: List<Stat>?
        ) {
            data class Achievement(
                val name: String,
                val defaultvalue: Long,
                val displayName: String,
                val hidden: Int,
                val description: String?,
                val icon: String,
                val icongray: String
            )

            data class Stat(
                val name: String,
                val defaultvalue: Long?,
                val displayName: String?
            )
        }
    }
}