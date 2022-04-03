package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.KmsAuthorizationDao
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.filter.CorsPolicy
import org.http4k.filter.Only
import org.http4k.filter.OriginPolicy
import org.http4k.serverless.ApiGatewayV2LambdaFunction
import org.http4k.serverless.AppLoader
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.kms.KmsClient
import java.time.Clock

object ApiLambdaLoader: AppLoader {

    override fun invoke(env: Map<String, String>): HttpHandler {
        val dynamo = DynamoDbEnhancedClient.create()
        val kms = KmsClient.create()

        val frontendHost = Uri.of(env.getValue("FRONTEND_HOST"))

        val gameService = ServiceBuilder.gameService(
            dynamo = dynamo,
            achievementsTableName = env.getValue("ACHIEVEMENTS_TABLE"),
            gamesTableName = env.getValue("GAMES_TABLE")
        )

        val syncService = ServiceBuilder.syncService(
            steamBackend = JavaHttpClient(),
            steamApiKey = env.getValue("STEAM_API_KEY"),
            clock = Clock.systemUTC(),
            gameService = gameService
        )

        val jobService = ServiceBuilder.jobService(
            dynamo = dynamo,
            jobsTableName = env.getValue("JOBS_TABLE"),
            clock = Clock.systemUTC(),
            syncService = syncService
        )

        return ServiceBuilder.api(
            gameService = gameService,
            authService = ServiceBuilder.authService(
                serverHost = Uri.of(env.getValue("SERVER_HOST")),
                authDao = KmsAuthorizationDao(
                    kms = kms,
                    keyId = env.getValue("AUTH_KEY_ID")
                )
            ),
            jobService = jobService,
            syncService = syncService,
            corsPolicy = CorsPolicy(
                OriginPolicy.Only(frontendHost.toString()),
                headers = listOf("Authorization"),
                methods = listOf(Method.GET, Method.POST),
                credentials = true
            )
        )
    }
}

class ApiLambdaHandler: ApiGatewayV2LambdaFunction(ApiLambdaLoader)