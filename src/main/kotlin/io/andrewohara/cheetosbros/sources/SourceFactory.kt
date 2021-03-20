package io.andrewohara.cheetosbros.sources

//import io.andrewohara.cheetosbros.sources.openxbl.OpenXblSource

interface SourceFactory {
    operator fun get(player: Player): Source?
}

class SourceFactoryImpl(private val steamSource: Source): SourceFactory {

    override fun get(player: Player) = when(player.uid.platform) {
//        Platform.Xbox -> OpenXblSource(player.token!!)
        Platform.Steam -> steamSource
        else -> null
    }
}