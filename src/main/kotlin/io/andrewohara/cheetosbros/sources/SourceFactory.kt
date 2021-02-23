package io.andrewohara.cheetosbros.sources

//import io.andrewohara.cheetosbros.sources.openxbl.OpenXblSource
import io.andrewohara.cheetosbros.sources.steam.SteamSource

interface SourceFactory {
    operator fun get(player: Player): Source?
}

class SourceFactoryImpl(steamKey: String): SourceFactory {

    private val steamSource = SteamSource(steamKey)

    override fun get(player: Player) = when(player.platform) {
//        Platform.Xbox -> OpenXblSource(player.token!!)
        Platform.Steam -> steamSource
        else -> null
    }
}