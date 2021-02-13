package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.PlatformConverter
import io.andrewohara.cheetosbros.sources.Player
import io.andrewohara.cheetosbros.sources.LibraryItem
import io.andrewohara.cheetosbros.sources.Platform

class GameLibraryDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoLibraryItem, String, String>(tableName, client)

    fun list(player: Player): Collection<LibraryItem> {
        val query = DynamoDBQueryExpression<DynamoLibraryItem>()
            .withHashKeyValues(DynamoLibraryItem(playerUuid = player.uuid()))
            .withProjectionExpression("gameId")

        return mapper
            .query(query)
            .map { it.toModel() }
    }

    fun batchSave(player: Player, libraryItems: Collection<LibraryItem>) {
        val existingGameIds = mapper.batchLoad(libraryItems.map { DynamoLibraryItem(player, it) }).map { it.gameId }

        val toSave = libraryItems.filter { it.gameId !in existingGameIds }.map { DynamoLibraryItem(player, it) }
        mapper.batchSave(toSave)
    }

    @DynamoDBDocument
    data class DynamoLibraryItem(
        @DynamoDBHashKey
        var playerUuid: String? = null,

        @DynamoDBRangeKey
        var gameId: String? = null,

        var gameName: String? = null,

        @DynamoDBTypeConverted(converter = PlatformConverter::class)
        var platform: Platform? = null,
    ) {
        constructor(player: Player, status: LibraryItem) : this(
            playerUuid = "${player.platform}-${player.id}",
            gameId = status.gameId,
            platform = player.platform,
            gameName = status.gameName
        )

        fun toModel() = LibraryItem(
            platform = platform!!,
            gameId = gameId!!,
            gameName = gameName!!
        )
    }

    companion object {
        private fun Player.uuid() = "$platform-$id"
    }
}