package io.andrewohara.cheetosbros.sources

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted
import io.andrewohara.cheetosbros.api.games.Uid
import io.andrewohara.cheetosbros.lib.*
import java.time.Duration
import java.time.Instant
import java.util.*

class JobsDao(client: AmazonDynamoDB, table: String, private val expiry: Duration = Duration.ofHours(1)) {

    val tableMapper = DynamoUtils.mapper<DynamoJob, UUID, UUID>(table, client)

    fun insert(job: Job, time: Instant) = batchInsert(setOf(job), time)

    fun batchInsert(jobs: Collection<Job>, time: Instant) {
        val items = jobs.map { job ->
            DynamoJob(
                userId = job.userId,
                jobId = UUID.randomUUID(),
                platform = job.platform,
                gameId = job.gameId,
                expires = time + expiry
            )
        }

        tableMapper.batchSave(items)
    }

    fun delete(userId: UUID, jobId: UUID) {
        val key = DynamoJob(userId = userId, jobId = jobId)
        tableMapper.delete(key)
    }

    fun count(userId: UUID): Int {
        val query = DynamoDBQueryExpression<DynamoJob>()
            .withHashKeyValues(DynamoJob(userId = userId))

        return tableMapper.count(query)
    }

    operator fun get(userId: UUID, jobId: UUID): Job? {
        val item = tableMapper.load(userId, jobId) ?: return null

        return Job(
            userId = item.userId!!,
            platform = item.platform!!,
            gameId = item.gameId
        )
    }
}

data class DynamoJob(
    @DynamoDBHashKey
    @DynamoDBTypeConverted(converter = UUIDConverter::class)
    var userId: UUID? = null,

    @DynamoDBRangeKey
    @DynamoDBTypeConverted(converter = UUIDConverter::class)
    var jobId: UUID? = null,

    @DynamoDBTypeConverted(converter = UidConverter::class)
    var gameId: Uid? = null,

    @DynamoDBTypeConverted(converter = PlatformConverter::class)
    var platform: Platform? = null,

    @DynamoDBTypeConverted(converter = EpochInstantConverter::class)
    var expires: Instant? = null
)