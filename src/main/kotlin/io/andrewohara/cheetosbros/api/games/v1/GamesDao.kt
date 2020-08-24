package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Platform

class GamesDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoGame, String, Void>(tableName, client)

    fun save(game: Game) {
        val item = DynamoGame(game)
        mapper.save(item)
    }

    fun batchSave(games: Collection<Game>) {
        val items = games.map { DynamoGame(it) }
        mapper.batchSave(items)
    }

    operator fun get(platform: Platform, gameId: String): Game? {
        return mapper.load(uuid(platform, gameId))?.toGame()
    }

    fun batchGet(platform: Platform, ids: Collection<String>): Collection<Game> {
        val items = ids.map { DynamoGame(uuid = uuid(platform, it)) }
        return mapper.batchLoad(items).map { it.toGame() }
    }

    @DynamoDBDocument
    data class DynamoGame(
            @DynamoDBHashKey
            var uuid: String? = null,

            var id: String? = null,
            var platform: String? = null,
            var name: String? = null,
            var displayImage: String? = null
    ) {
        constructor(game: Game) : this(
                uuid = uuid(game.platform, game.id),
                id = game.id,
                platform = game.platform.toString(),
                name = game.name,
                displayImage = game.displayImage
        )

        fun toGame() = Game(
                id = id!!,
                platform = Platform.valueOf(platform!!),
                name = name!!,
                displayImage = displayImage
        )
    }

    companion object {
        private fun uuid(platform: Platform, id: String) = "$platform-$id"
    }
}

