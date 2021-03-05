package io.andrewohara.cheetosbros.sources

class FakeSourceFactory(private val steamSource: FakeSource, private val xboxSource: FakeSource): SourceFactory {

    override fun get(player: Player) = when(player.uid.platform) {
        Platform.Xbox -> xboxSource
        Platform.Steam -> steamSource
    }
}