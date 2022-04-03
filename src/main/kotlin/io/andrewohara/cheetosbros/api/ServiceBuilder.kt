package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.*
import io.andrewohara.cheetosbros.api.v1.*
import io.andrewohara.cheetosbros.games.*
import io.andrewohara.cheetosbros.jobs.Job
import io.andrewohara.cheetosbros.jobs.JobService
import io.andrewohara.cheetosbros.jobs.JobsDao
import io.andrewohara.cheetosbros.sources.steam.SteamClient
import io.andrewohara.cheetosbros.sync.SyncService
import io.andrewohara.dynamokt.DataClassTableSchema
import io.andrewohara.utils.http4k.ContractUi
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.BearerAuthSecurity
import org.http4k.core.*
import org.http4k.filter.*
import org.http4k.lens.RequestContextKey
import org.http4k.routing.*
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import java.time.Clock
import java.time.Duration

object ServiceBuilder {
    fun syncService(
        steamBackend: HttpHandler,
        steamApiKey: String,
        gameService: GameService,
        clock: Clock
    ): SyncService {
        return SyncService(
            steam = SteamClient(steamApiKey, steamBackend),
            gameService = gameService,
            clock = clock
        )
    }

    fun jobService(
        dynamo: DynamoDbEnhancedClient,
        jobsTableName: String,
        clock: Clock,
        jobRetention: Duration = Duration.ofHours(1),
        syncService: SyncService
    ) = JobService(
        jobsDao = JobsDao(dynamo, dynamo.table(jobsTableName, DataClassTableSchema(Job::class))),
        clock = clock,
        retention = jobRetention,
        syncService = syncService
    )

    fun gameService(
        dynamo: DynamoDbEnhancedClient,
        gamesTableName: String,
        achievementsTableName: String,
    ) = GameService(
        gamesDao = GamesDao(dynamo.table(gamesTableName, DataClassTableSchema(Game::class))),
        achievementsDao = AchievementsDao(dynamo, dynamo.table(achievementsTableName, DataClassTableSchema(Achievement::class)))
    )

    fun authService(
        authDao: AuthorizationDao,
        serverHost: Uri
    ) = AuthService(
        authDao = authDao,
        serverHost = serverHost,
        steamOpenId = SteamOpenID()
    )

    fun api(
        gameService: GameService,
        jobService: JobService,
        authService: AuthService,
        syncService: SyncService,
        corsPolicy: CorsPolicy
    ): RoutingHttpHandler {
        val contexts = RequestContexts()
        val authLens = RequestContextKey.required<String>(contexts, "auth")

        val security = BearerAuthSecurity(authLens, authService::authorize)

        val apiV1 = ContractUi(
            pageTitle = "SteamCheetos API",
            contract = contract {
                renderer = OpenApi3(
                    ApiInfo(
                        title = "SteamCheetos API",
                        version = "v1.0"
                    )
                )
                descriptionPath =  "/swagger.json"
                routes += ApiV1(gameService, jobService, authService, syncService, authLens, security).routes()
            },
            descriptionPath =  "/swagger.json",
            displayOperationId = true
        )

        return ServerFilters.InitialiseRequestContext(contexts)
            .then(ServerFilters.Cors(corsPolicy))
            .then(apiV1)
    }
}