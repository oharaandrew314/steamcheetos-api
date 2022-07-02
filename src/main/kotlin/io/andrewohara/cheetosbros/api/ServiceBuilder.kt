package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.CheetosService
import io.andrewohara.cheetosbros.api.auth.*
import io.andrewohara.cheetosbros.api.v1.*
import io.andrewohara.cheetosbros.games.*
import io.andrewohara.cheetosbros.sources.steam.SteamClient
import io.andrewohara.utils.http4k.ContractUi
import io.andrewohara.utils.http4k.logErrors
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.v3.AutoJsonToJsonSchema
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.BearerAuthSecurity
import org.http4k.core.*
import org.http4k.filter.*
import org.http4k.format.*
import org.http4k.lens.RequestContextKey
import org.http4k.routing.*
import java.time.Clock
import java.time.Duration
import java.util.concurrent.Executors

object ServiceBuilder {

    fun gameService(
        dynamo: DynamoDb,
        gamesTableName: String,
        achievementsTableName: String,
        clock: Clock = Clock.systemUTC(),
        steamBackend: HttpHandler,
        achievementDataRetention: Duration = Duration.ofDays(30),
        progressRetention: Duration = Duration.ofDays(1),
        recentGameLimit: Int = 10,
        syncTimeout: Duration = Duration.ofSeconds(10),
        imageCdnHost: Uri
    ) = CheetosService(
        gamesDao = GamesDao(dynamo, TableName.of(gamesTableName)),
        achievementsDao = AchievementsDao(dynamo, TableName.of(achievementsTableName)),
        clock = clock,
        steam = SteamClient(steamBackend),
        achievementDataRetention = achievementDataRetention,
        progressRetention = progressRetention,
        recentGameLimit = recentGameLimit,
        syncTimeout = syncTimeout,
        executor = Executors.newCachedThreadPool(),
        imageCdnHost = imageCdnHost
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
        cheetosService: CheetosService,
        authService: AuthService,
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
                    ),
                    json = Gson,
                    apiRenderer = ApiRenderer.Auto(Gson, AutoJsonToJsonSchema(Gson))
                )
                descriptionPath =  "/swagger.json"
                routes += ApiV1(cheetosService, authService, authLens, security).routes()
            },
            descriptionPath =  "/swagger.json",
            displayOperationId = true
        )

        return ServerFilters.InitialiseRequestContext(contexts)
            .then(ServerFilters.Cors(corsPolicy))
            .then(ServerFilters.logErrors())
            .then(apiV1)
    }
}