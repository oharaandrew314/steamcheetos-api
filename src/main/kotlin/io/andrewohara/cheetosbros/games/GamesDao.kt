package io.andrewohara.cheetosbros.games

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import io.andrewohara.cheetosbros.sources.GameData
import io.andrewohara.lib.batchPutItem
import org.http4k.connect.amazon.dynamodb.*
import org.http4k.connect.amazon.dynamodb.model.*
import org.http4k.core.Uri
import org.http4k.format.autoDynamoLens
import java.io.IOException
import java.time.Instant

class GamesDao(private val dynamo: DynamoDb, private val tableName: TableName) {

    private val lens = DynamoDbMoshi.autoDynamoLens<Game>()
    private val userIdAttr = Attribute.string().required("userId")
    private val gameIdAttr = Attribute.string().required("id")

    operator fun get(userId: String): List<Game> {
        return dynamo.queryPaginated(
            TableName = tableName,
            KeyConditionExpression = "$userIdAttr = :val1",
            ExpressionAttributeValues = mapOf(":val1" to userIdAttr.asValue(userId))
        )
            .flatMap { it.onFailure { e -> throw IOException("Error querying items: $e") }.map(lens) }
            .toList()
    }

    operator fun get(userId: String, gameId: String): Game? {
        return dynamo.getItem(
            TableName = tableName,
            Key = Item(userIdAttr of userId, gameIdAttr of gameId)
        )
            .map { it.item?.let(lens) }
            .onFailure { throw IOException("Error getting item: $it") }
    }

    operator fun plusAssign(game: Game) {
        val item = Item().with(lens of game)
        dynamo.putItem(
            TableName = tableName,
            Item = item
        ).onFailure { throw IOException("Error putting item: $it") }
    }

    operator fun plusAssign(games: Collection<Game>) {
        dynamo.batchPutItem(tableName, games, lens)
            .onFailure { throw IOException("Error putting items: $it") }
    }
}

data class Game(
    val userId: String,
    val id: String,

    val displayImage: Uri,
    val name: String,

    val achievementsTotal: Int?,
    val achievementDataExpires: Instant = Instant.EPOCH,

    val achievementsUnlocked: Int?,
    val progressExpires: Instant = Instant.EPOCH,

    val favourite: Boolean = false,
)

fun GameData.toGame(userId: String, time: Instant) = Game(
    userId = userId,
    id = id,
    name = name,
    displayImage = displayImage,
    achievementsTotal = null,
    progressExpires = time,
    achievementDataExpires = time,
    achievementsUnlocked = null,
)