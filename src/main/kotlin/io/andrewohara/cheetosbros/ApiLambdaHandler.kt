package io.andrewohara.cheetosbros

import io.andrewohara.cheetosbros.api.ServiceBuilder
import io.andrewohara.cheetosbros.api.auth.KmsAuthorizationDao
import io.andrewohara.utils.http4k.logSummary
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.Http
import org.http4k.connect.amazon.kms.Http
import org.http4k.connect.amazon.kms.KMS
import org.http4k.core.*
import org.http4k.filter.*
import org.http4k.serverless.ApiGatewayV2LambdaFunction
import org.http4k.serverless.AppLoader
import org.slf4j.LoggerFactory
import java.time.Clock

object ApiLambdaLoader: AppLoader {

    override fun invoke(env: Map<String, String>): HttpHandler {
        val log = LoggerFactory.getLogger(javaClass)

        val corsPolicy = CorsPolicy(
            OriginPolicy.AnyOf(env.getValue("CORS_ORIGINS").split(",")),
            headers = listOf("Authorization"),
            methods = listOf(Method.GET, Method.POST),
            credentials = true
        )

        val steamBackend = ResponseFilters.logSummary()
            .then(ClientFilters.SetBaseUriFrom(Uri.of("https://api.steampowered.com")))
            .then(RequestFilters.Modify({ it.query("key", env.getValue("STEAM_API_KEY"))}))
            .then(JavaHttpClient())

        val service = ServiceBuilder.gameService(
            dynamo = DynamoDb.Http(env),
            achievementsTableName = env.getValue("ACHIEVEMENTS_TABLE"),
            gamesTableName = env.getValue("GAMES_TABLE"),
            steamBackend = steamBackend,
            clock = Clock.systemUTC(),
            imageCdnHost = Uri.of(env.getValue("CDN_HOST"))
        )

        val auth = ServiceBuilder.authService(
            serverHost = Uri.of(env.getValue("SERVER_HOST")),
            authDao = KmsAuthorizationDao(
                kms = KMS.Http(env),
                keyId = KMSKeyId.parse(env.getValue("AUTH_KEY_ID"))
            )
        )

        val api = ServiceBuilder.api(
            cheetosService = service,
            authService = auth,
            corsPolicy = corsPolicy
        )

        return ResponseFilters.logSummary(log).then(api)
    }
}

class ApiLambdaHandler: ApiGatewayV2LambdaFunction(ApiLambdaLoader)