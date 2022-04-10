package io.andrewohara.cheetosbros

import io.andrewohara.cheetosbros.api.ServiceBuilder
import io.andrewohara.cheetosbros.api.auth.KmsAuthorizationDao
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.*
import org.http4k.serverless.ApiGatewayV2LambdaFunction
import org.http4k.serverless.AppLoader
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.kms.KmsClient
import java.time.Clock

object ApiLambdaLoader: AppLoader {

    override fun invoke(env: Map<String, String>): HttpHandler {
        val dynamo = DynamoDbEnhancedClient.create()
        val kms = KmsClient.create()

        val corsPolicy = CorsPolicy(
            OriginPolicy.AnyOf(env.getValue("CORS_ORIGINS").split(",")),
            headers = listOf("Authorization"),
            methods = listOf(Method.GET, Method.POST),
            credentials = true
        )

        val steamBackend = ClientFilters
            .SetBaseUriFrom(Uri.of("https://api.steampowered.com"))
            .then(RequestFilters.Modify({ it.query("key", env.getValue("STEAM_API_KEY"))}))
            .then(JavaHttpClient())

        val service = ServiceBuilder.gameService(
            dynamo = dynamo,
            achievementsTableName = env.getValue("ACHIEVEMENTS_TABLE"),
            gamesTableName = env.getValue("GAMES_TABLE"),
            steamBackend = steamBackend,
            clock = Clock.systemUTC(),
        )

        val auth = ServiceBuilder.authService(
            serverHost = Uri.of(env.getValue("SERVER_HOST")),
            authDao = KmsAuthorizationDao(
                kms = kms,
                keyId = env.getValue("AUTH_KEY_ID")
            )
        )

        return ServiceBuilder.api(
            cheetosService = service,
            authService = auth,
            corsPolicy = corsPolicy
        )
    }
}

class ApiLambdaHandler: ApiGatewayV2LambdaFunction(ApiLambdaLoader)