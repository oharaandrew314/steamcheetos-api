package io.andrewohara.lib

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.onFailure
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.batchWriteItem
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.ReqWriteItem
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.with
import org.http4k.lens.BiDiLens

fun <T> DynamoDb.batchPutItem(TableName: TableName, Items: Collection<T>, lens: BiDiLens<Item, T>): Result<Unit, RemoteFailure> {
    if (Items.isEmpty()) return Success(Unit)

    for (chunk in Items.chunked(25)) {
        val batch = chunk.map { obj ->
            val item = Item().with(lens of obj)
            ReqWriteItem.Put(item)
        }

        batchWriteItem(mapOf(TableName to batch))
            .onFailure { return it }
    }

    return Success(Unit)
}