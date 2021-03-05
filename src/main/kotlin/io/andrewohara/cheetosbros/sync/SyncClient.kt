package io.andrewohara.cheetosbros.sync

import com.amazonaws.services.sqs.AmazonSQS
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.andrewohara.cheetosbros.sources.Player
import io.andrewohara.cheetosbros.sources.Source

interface SyncClient {

    fun sync(player: Player)
    fun syncGame(player: Player, game: Source.Game)
}

class SqsSyncClient(private val sqs: AmazonSQS, private val url: String): SyncClient {

    private val mapper = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(SyncMessage::class.java)

    override fun sync(player: Player) {
        val message = SyncMessage(player = player, game = null)

        sqs.sendMessage(url, mapper.toJson(message))
    }

    override fun syncGame(player: Player, game: Source.Game) {
        val message = SyncMessage(player = player, game = game)

        sqs.sendMessage(url, mapper.toJson(message))
    }
}