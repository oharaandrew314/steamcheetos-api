package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.Game

class GamesDao(tableName: String, private val client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoGame, String, Void>(tableName, client)

    fun save(game: Game) {
        val item = DynamoGame(game)
        mapper.save(item)
    }

    fun batchSave(games: Collection<Game>) {
        val items = games.map { DynamoGame(it) }
        mapper.batchSave(items)
    }

    operator fun get(uuid: String): Game? {
        return mapper.load(uuid)?.toGame()
    }

    fun batchGet(uuids: Collection<String>): Collection<Game> {
        val items = uuids.map { DynamoGame(uuid = it) }
        return mapper.batchLoad(items).map { it.toGame() }
    }

    @DynamoDBDocument
    data class DynamoGame(
            @DynamoDBHashKey
            var uuid: String? = null,

            var id: String? = null,
            var platform: String? = null,
            var name: String? = null,
            var displayImage: String? = null,
            var icon: String? = null,
    ) {
        constructor(game: Game) : this(
                uuid = game.uuid,
                id = game.id,
                platform = game.platform.toString(),
                name = game.name,
                displayImage = game.displayImage,
                icon = game.displayImage
        )

        fun toGame() = Game(
                id = id!!,
                platform = Game.Platform.valueOf(platform!!),
                name = name!!,
                displayImage = displayImage,
                icon = icon
        )
    }
}

