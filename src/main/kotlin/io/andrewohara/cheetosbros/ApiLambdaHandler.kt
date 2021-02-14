package io.andrewohara.cheetosbros

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest
import io.andrewohara.cheetosbros.api.ApiBuilder
import io.javalin.Javalin
import java.io.InputStream
import java.io.OutputStream

class ApiLambdaHandler: RequestStreamHandler {

    private val handler = let {

        val steamParamName = System.getenv("STEAM_API_KEY_NAME")
        val publicPemName = System.getenv("PUBLIC_PEM_NAME")
        val privatePemName =  System.getenv("PRIVATE_PEM_NAME")

        val ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient()
        val request = GetParametersRequest()
            .withWithDecryption(true)
            .withNames(steamParamName, publicPemName, privatePemName)

        val params = ssm.getParameters(request).parameters

        val builder = ApiBuilder(
            dynamoDb = AmazonDynamoDBClientBuilder.defaultClient(),
            gamesTableName = System.getenv("GAMES_TABLE"),
            achievementsTableName = System.getenv("ACHIEVEMENTS_TABLE"),
            achievementStatusTableName = System.getenv("ACHIEVEMENT_STATUS_TABLE"),
            usersTableName = System.getenv("USERS_TABLE"),
            playersTableName = System.getenv("PLAYERS_TABLE"),
            libraryTableName = System.getenv("LIBRARY_TABLE"),
            steamKey = params.first { it.name == steamParamName }.value,
            privateKey = params.first { it.name == privatePemName }.value,
            publicKey = params.first { it.name == publicPemName }.value,
            publicKeyIssuer = System.getenv("PEM_ISSUER")
        )

        val app = Javalin.createStandalone { config ->
            builder.updateConfig(config)
        }
        builder.registerController(app)

        JavalinHttpApiContainerHandler(app)
    }

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        val request = input.reader().readText()
        println(request)

        return handler.proxyStream(request.byteInputStream(), output, context)
    }
}