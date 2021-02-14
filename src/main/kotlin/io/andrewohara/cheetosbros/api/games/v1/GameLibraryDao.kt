package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.PlatformConverter
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Player
import io.andrewohara.cheetosbros.sources.Platform

class GameLibraryDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoLibraryItem, String, String>(tableName, client)

    fun listGameIds(player: Player): Collection<String> {
        val query = DynamoDBQueryExpression<DynamoLibraryItem>()
            .withHashKeyValues(DynamoLibraryItem(playerUuid = player.uuid()))
            .withProjectionExpression("gameId")

        return mapper.query(query).map { it.gameId!! }
    }

    fun save(player: Player, game: Game) {
        val item = DynamoLibraryItem(player, game)

        mapper.save(item)
    }

    fun batchSave(player: Player, games: Collection<Game>) {
        val items = games.map { DynamoLibraryItem(player, it) }

        mapper.batchSave(items)
    }

    @DynamoDBDocument
    data class DynamoLibraryItem(
        @DynamoDBHashKey
        var playerUuid: String? = null,

        @DynamoDBRangeKey
        var gameId: String? = null,

        @DynamoDBTypeConverted(converter = PlatformConverter::class)
        var platform: Platform? = null,
    ) {
        constructor(player: Player, game: Game) : this(
            playerUuid = "${player.platform}-${player.id}",
            gameId = game.id,
            platform = player.platform
        )
    }

    companion object {
        private fun Player.uuid() = "$platform-$id"
    }
}