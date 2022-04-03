package io.andrewohara.cheetosbros.lib

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.time.Instant

object DynamoLimits {
    const val batchSize = 25
}

class InstantAsLongConverter: AttributeConverter<Instant> {

    override fun transformFrom(input: Instant): AttributeValue = AttributeValue.fromN(input.epochSecond.toString())

    override fun transformTo(input: AttributeValue): Instant = Instant.ofEpochSecond(input.n().toLong())

    override fun type(): EnhancedType<Instant> = EnhancedType.of(Instant::class.java)

    override fun attributeValueType(): AttributeValueType = AttributeValueType.N
}

inline fun <reified T> DynamoDbTable<T>.batchPut(client: DynamoDbEnhancedClient, items: Collection<T>) {
    if (items.isEmpty()) return

    val batches = items.chunked(DynamoLimits.batchSize).map { chunk ->
        WriteBatch.builder(T::class.java).apply {
            mappedTableResource(this@batchPut)
            for (item in chunk) {
                addPutItem(item)
            }
        }.build()
    }

    for (batch in batches) {
        client.batchWriteItem {
            it.addWriteBatch(batch)
        }
    }
}