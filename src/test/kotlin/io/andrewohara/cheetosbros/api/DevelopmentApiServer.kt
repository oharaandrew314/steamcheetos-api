package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.FakeAuthorizationDao
import io.andrewohara.cheetosbros.api.auth.KmsAuthorizationDao
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.Http
import org.http4k.connect.amazon.kms.Http
import org.http4k.connect.amazon.kms.KMS
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.*
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val env = System.getenv()

    val dynamo = DynamoDb.Http(env)
    val kms = KMS.Http(env)

    val serverHost = Uri.of(env.getValue("SERVER_HOST"))

    val gameService = ServiceBuilder.gameService(
        dynamo = dynamo,
        achievementsTableName = env.getValue("ACHIEVEMENTS_TABLE"),
        gamesTableName = env.getValue("GAMES_TABLE"),
        steamBackend = ClientFilters.SetBaseUriFrom(Uri.of("https://api.steampowered.com"))
            .then(RequestFilters.Modify({ it.query("key", env.getValue("STEAM_API_KEY"))}))
            .then(DebuggingFilters.PrintResponse())
            .then(JavaHttpClient()),
        imageCdnHost = Uri.of("https://cdn.steamcheetos.com")
    )

    val api = ServiceBuilder.api(
        cheetosService = gameService,
        authService = ServiceBuilder.authService(
            serverHost = serverHost,
            authDao = KmsAuthorizationDao(kms, KMSKeyId.of(env.getValue("KMS_KEY_ID")))
//            authDao = FakeAuthorizationDao()
        ),
        corsPolicy = CorsPolicy(OriginPolicy.AllowAll(), listOf("Authorization"), Method.values().toList(), false)
    )

    DebuggingFilters.PrintRequestAndResponse()
        .then(api)
        .asServer(SunHttp(serverHost.port!!))
        .start()
        .also { println("Running on port ${serverHost.port}") }
        .block()
}