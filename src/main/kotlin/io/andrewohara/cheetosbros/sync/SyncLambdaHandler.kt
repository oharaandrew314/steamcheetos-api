package io.andrewohara.cheetosbros.sync

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import io.andrewohara.cheetosbros.api.ServiceBuilder
import io.andrewohara.cheetosbros.jobs.Job
import org.http4k.client.JavaHttpClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import java.time.Clock
import java.time.Instant

class SyncLambdaHandler: RequestHandler<DynamodbEvent, Unit> {

    private val service = let {
        val dynamo = DynamoDbEnhancedClient.create()

        val gameService = ServiceBuilder.gameService(
            dynamo = dynamo,
            achievementsTableName = System.getenv("ACHIEVEMENTS_TABLE"),
            gamesTableName = System.getenv("GAMES_TABLE"),
        )

        val syncService = ServiceBuilder.syncService(
            steamApiKey = System.getenv("STEAM_API_KEY"),
            steamBackend = JavaHttpClient(),
            gameService = gameService,
            clock = Clock.systemUTC()
        )

        ServiceBuilder.jobService(
            dynamo = dynamo,
            jobsTableName = System.getenv("JOBS_TABLE"),
            clock = Clock.systemUTC(),
            syncService = syncService
        )
    }

    override fun handleRequest(input: DynamodbEvent, context: Context) {
        for (record in input.records) {
            val model = record.dynamodb.newImage ?: continue

            val job = Job(
                userId = model["userId"]?.s ?: continue,
                gameId = model["gameId"]?.s ?: continue,
                expires = model["expires"]?.n?.toLong()?.let { Instant.ofEpochSecond(it) } ?: continue
            )

            service(job)
        }
    }
}