package io.andrewohara.cheetosbros.sources.steam

data class StoreApiGameEntry(
    val success: Boolean,
    val data: Data?
) {
    data class Data(
        val type: String,
        val name: String,
        val steam_appid: Int,
        val header_image: String
    )
}