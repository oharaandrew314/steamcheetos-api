package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.Player
import io.andrewohara.cheetosbros.sources.LibraryItem
import io.andrewohara.cheetosbros.sources.Platform
import java.time.Instant

class GameLibraryDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoLibraryItem, String, String>(tableName, client)

    fun listGameIds(player: Player): Collection<String> {
        val query = DynamoDBQueryExpression<DynamoLibraryItem>()
                .withHashKeyValues(DynamoLibraryItem(playerUuid = uuid(player)))
                .withProjectionExpression("gameId")

        val result = mapper.query(query)

        return result.mapNotNull { it.gameId }
    }

    fun get(user: User, gameUuid: String): LibraryItem? {
        return mapper.load(user.id, gameUuid)?.toGameStatus()
    }

//    fun save(player: Player, status: LibraryItem) {
//        val item = DynamoLibraryItem(player, status)
//        mapper.save(item)
//    }

    fun batchSave(player: Player, libraryItems: Collection<LibraryItem>) {
        val items = libraryItems.map { DynamoLibraryItem(player, it) }
        mapper.batchSave(items)
    }

    @DynamoDBDocument
    data class DynamoLibraryItem(
            @DynamoDBHashKey
            var playerUuid: String? = null,

            @DynamoDBRangeKey
            var gameId: String? = null,

            var platform: String? = null
    ) {
        constructor(player: Player, status: LibraryItem): this(
                playerUuid = "${player.platform}-${player.id}",
                gameId = status.gameId,
                platform = player.platform.toString(),
        )

        fun toGameStatus() = LibraryItem(
                platform = Platform.valueOf(platform!!),
                gameId = gameId!!
        )
    }

    companion object {
        private fun uuid(player: Player) = "${player.platform}-${player.id}"
    }
}