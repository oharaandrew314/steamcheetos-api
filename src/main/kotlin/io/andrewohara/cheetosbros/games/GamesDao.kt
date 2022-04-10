package io.andrewohara.cheetosbros.games

import io.andrewohara.cheetosbros.sources.GameData
import io.andrewohara.dynamokt.DynamoKtConverted
import io.andrewohara.dynamokt.DynamoKtPartitionKey
import io.andrewohara.dynamokt.DynamoKtSortKey
import io.andrewohara.utils.dynamodb.v2.batchPut
import org.http4k.core.Uri
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import java.time.Instant

class GamesDao(private val client: DynamoDbEnhancedClient, private val table: DynamoDbTable<Game>) {

    operator fun get(userId: String): List<Game> {
        val key = Key.builder().partitionValue(userId).build()

        return table.query(QueryConditional.keyEqualTo(key))
            .flatMap { it.items() }
            .toList()
    }

    operator fun get(userId: String, gameId: String): Game? {
        val key = Key.builder()
            .partitionValue(userId)
            .sortValue(gameId)
            .build()

        return table.getItem(key)
    }

    operator fun plusAssign(game: Game) {
        table.putItem(game)
    }

    operator fun plusAssign(games: Collection<Game>) {
        table.batchPut(client, games)
    }
}

data class Game(
    @DynamoKtPartitionKey
    val userId: String,

    @DynamoKtSortKey
    val id: String,

    @DynamoKtConverted(UriConverter::class)
    val displayImage: Uri,

    val name: String,

    val achievementsTotal: Int?,
    val achievementDataExpires: Instant = Instant.EPOCH,

    val achievementsUnlocked: Int?,
    val progressExpires: Instant = Instant.EPOCH
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