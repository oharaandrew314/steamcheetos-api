package io.andrewohara.cheetosbros.sources.steam

data class ResolveVanityURLResponse(
        val response: ResolveVanityURLResponseData
) {
    data class ResolveVanityURLResponseData(
        val success: Int,
        val steamid: String?
    )
}