package io.andrewohara.cheetosbros.games

import io.andrewohara.dynamokt.DynamoKtConverted
import io.andrewohara.dynamokt.DynamoKtPartitionKey
import io.andrewohara.dynamokt.DynamoKtSortKey
import org.http4k.core.Uri
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import java.time.Instant

class GamesDao(private val table: DynamoDbTable<Game>) {

    operator fun get(userId: String): Collection<Game> {
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
}

data class Game(
    @DynamoKtPartitionKey
    val userId: String,

    @DynamoKtSortKey
    val id: String,

    @DynamoKtConverted(UriConverter::class)
    val displayImage: Uri?,

    val name: String,
    val achievementsTotal: Int,
    val achievementsUnlocked: Int,
    val lastUpdated: Instant
)