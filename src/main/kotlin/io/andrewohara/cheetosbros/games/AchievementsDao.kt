package io.andrewohara.cheetosbros.games

import io.andrewohara.cheetosbros.lib.batchPut
import io.andrewohara.dynamokt.DynamoKtConverted
import io.andrewohara.dynamokt.DynamoKtPartitionKey
import io.andrewohara.dynamokt.DynamoKtSortKey
import org.http4k.core.Uri
import software.amazon.awssdk.enhanced.dynamodb.*
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.time.Instant

class AchievementsDao(private val client: DynamoDbEnhancedClient, private val table: DynamoDbTable<Achievement>) {

    operator fun plusAssign(achievements: Collection<Achievement>) {
        table.batchPut(client, achievements)
    }

    operator fun get(userId: String, gameId: String): Collection<Achievement> {
        val key = Key.builder()
            .partitionValue("$userId:$gameId")
            .build()

        return table.query(QueryConditional.keyEqualTo(key))
            .asSequence()
            .flatMap { it.items() }
            .toList()
    }
}

data class Achievement(
    @DynamoKtPartitionKey
    @DynamoKtConverted(LibraryIdConverter::class)
    val libraryId: LibraryId,
    @DynamoKtSortKey
    val id: String,

    val name: String,
    val description: String?,
    val hidden: Boolean,
    val score: Int?,
    val unlockedOn: Instant?,

    @DynamoKtConverted(UriConverter::class)
    val iconLocked: Uri?,
    @DynamoKtConverted(UriConverter::class)
    val iconUnlocked: Uri?,
)

data class LibraryId(
    val userId: String,
    val gameId: String,
)

class LibraryIdConverter: AttributeConverter<LibraryId> {
    override fun transformFrom(input: LibraryId): AttributeValue = AttributeValue.fromS("${input.userId}:${input.gameId}")

    override fun transformTo(input: AttributeValue): LibraryId {
        val (userId, gameId) = input.s().split(":")
        return LibraryId(userId, gameId)
    }

    override fun type(): EnhancedType<LibraryId> = EnhancedType.of(LibraryId::class.java)

    override fun attributeValueType(): AttributeValueType = AttributeValueType.S
}

class UriConverter: AttributeConverter<Uri> {
    override fun transformFrom(input: Uri): AttributeValue  = AttributeValue.fromS(input.toString())

    override fun transformTo(input: AttributeValue) = Uri.of(input.s())

    override fun type(): EnhancedType<Uri> = EnhancedType.of(Uri::class.java)

    override fun attributeValueType(): AttributeValueType = AttributeValueType.S
}