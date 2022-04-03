package io.andrewohara.cheetosbros.jobs

import io.andrewohara.cheetosbros.lib.*
import io.andrewohara.dynamokt.DynamoKtConverted
import io.andrewohara.dynamokt.DynamoKtPartitionKey
import io.andrewohara.dynamokt.DynamoKtSortKey
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch
import java.time.Instant
import java.util.*

class JobsDao(private val client: DynamoDbEnhancedClient, private val table: DynamoDbTable<Job>) {

    operator fun plusAssign(job: Job) {
        table.putItem(job)
    }

    operator fun plusAssign(jobs: Collection<Job>) {
        table.batchPut(client, jobs)
    }

    operator fun minusAssign(job: Job) {
        val key = Key.builder()
            .partitionValue(job.userId)
            .sortValue(job.gameId)
            .build()

        table.deleteItem(key)
    }

    fun delete(userId: String, jobId: UUID) {
        val key = Key.builder()
            .partitionValue(userId)
            .sortValue(jobId.toString())
            .build()

        table.deleteItem(key)
    }

    fun count(userId: String): Int {
        val key = Key.builder().partitionValue(userId).build()
        return table.query(QueryConditional.keyEqualTo(key)).count()  // TODO is this correct?
    }

    operator fun get(userId: String, jobId: UUID): Job? {
        val key = Key.builder()
            .partitionValue(userId)
            .sortValue(jobId.toString())
            .build()

        return table.getItem(key)
    }
}

data class Job(
    @DynamoKtPartitionKey
    val userId: String,

    @DynamoKtSortKey
    val gameId: String,

    @DynamoKtConverted(InstantAsLongConverter::class)
    val expires: Instant,
)