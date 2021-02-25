package io.andrewohara.cheetosbros

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import io.andrewohara.cheetosbros.sources.SourceFactoryImpl
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.andrewohara.cheetosbros.api.games.v1.AchievementStatusDao
import io.andrewohara.cheetosbros.api.games.v1.AchievementsDao
import io.andrewohara.cheetosbros.api.games.v1.GameLibraryDao
import io.andrewohara.cheetosbros.api.games.v1.GamesDao
import io.andrewohara.cheetosbros.sources.SourceManager
import io.andrewohara.cheetosbros.sync.SqsSyncClient
import io.andrewohara.cheetosbros.sync.SyncMessage

class SyncLambdaHandler: RequestHandler<SQSEvent, Unit> {

    private val syncClient = SqsSyncClient(AmazonSQSClientBuilder.defaultClient(), System.getenv("SYNC_QUEUE_URL"))

    private val service = let {
        val steamParamName = System.getenv("STEAM_API_KEY_NAME")

        val ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient()
        val request = GetParameterRequest()
            .withName(steamParamName)
            .withWithDecryption(true)

        val steamApiKey = ssm.getParameter(request).parameter.value

        val dynamoDb = AmazonDynamoDBClientBuilder.defaultClient()

        SourceManager(
            sourceFactory = SourceFactoryImpl(steamKey = steamApiKey),
            achievementsDao = AchievementsDao(System.getenv("ACHIEVEMENTS_TABLE"), dynamoDb),
            achievementStatusDao = AchievementStatusDao(System.getenv("ACHIEVEMENT_STATUS_TABLE"), dynamoDb),
            gamesDao = GamesDao(System.getenv("GAMES_TABLE"), dynamoDb),
            gameLibraryDao = GameLibraryDao(System.getenv("LIBRARY_TABLE"), dynamoDb)
        )
    }

    private val messageMapper = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(SyncMessage::class.java)

    override fun handleRequest(input: SQSEvent, context: Context) {
        input.records
            .mapNotNull { messageMapper.fromJson(it.body) }
            .forEach { message ->
                if (message.game == null) {
                    for (game in service.discoverGames(message.player)) {
                        syncClient.syncGame(message.player, game)
                    }
                } else {
                    service.syncGame(message.player, message.game)
                }
            }
    }
}