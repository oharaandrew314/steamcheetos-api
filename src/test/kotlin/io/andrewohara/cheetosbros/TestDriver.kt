package io.andrewohara.cheetosbros

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.cheetosbros.api.ServiceBuilder
import io.andrewohara.cheetosbros.api.auth.AuthService
import io.andrewohara.cheetosbros.api.auth.FakeAuthorizationDao
import io.andrewohara.cheetosbros.api.auth.SteamOpenID
import io.andrewohara.cheetosbros.games.*
import io.andrewohara.cheetosbros.sources.AchievementData
import io.andrewohara.cheetosbros.sources.AchievementStatusData
import io.andrewohara.cheetosbros.sources.GameData
import io.andrewohara.cheetosbros.sources.steam.FakeSteamBackend
import io.andrewohara.cheetosbros.sources.steam.SteamClient
import io.andrewohara.dynamokt.DataClassTableSchema
import io.andrewohara.dynamokt.createTableWithIndices
import io.andrewohara.utils.jdk.SameThreadExecutorService
import io.andrewohara.utils.jdk.toClock
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.filter.CorsPolicy
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import java.time.Duration
import java.time.Instant

val tNow: Instant = Instant.parse("2021-03-04T01:00:00Z")

class TestDriver: HttpHandler {

    private val clock = tNow.toClock()

    private val dynamoBackend = MockDynamoBackend(clock)
    private val dynamoDb = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(MockDynamoDbV2(dynamoBackend))
        .build()

    val gamesDao = dynamoDb
        .table("games", DataClassTableSchema(Game::class))
        .also { it.createTableWithIndices() }
        .let { GamesDao(dynamoDb, it) }

    val achievementsDao = dynamoDb
        .table("achievements", DataClassTableSchema(Achievement::class))
        .also { it.createTableWithIndices() }
        .let { AchievementsDao(dynamoDb, it) }

    val steam = FakeSteamBackend()

    val service = CheetosService(
        gamesDao = gamesDao,
        achievementsDao = achievementsDao,
        steam = SteamClient(steam),
        clock = clock,
        achievementDataRetention = Duration.ofDays(1),
        progressRetention = Duration.ofDays(1),
        recentGameLimit = 2,
        syncTimeout = Duration.ofSeconds(10),
        executor = SameThreadExecutorService()
    )

    val auth = AuthService(
        authDao = FakeAuthorizationDao(),
        serverHost = Uri.of("https://cheetos-api.test.com"),
        steamOpenId = SteamOpenID()
    )

    private val api = ServiceBuilder.api(
        cheetosService = service,
        authService = auth,
        corsPolicy = CorsPolicy.UnsafeGlobalPermissive
    )

    override fun invoke(request: Request) = api(request)

    fun createGame(userId: String, gameData: GameData, achievementData: List<AchievementData>, unlockData: List<AchievementStatusData>): CreateGameResult {
        val game = gameData.toGame(userId, clock.instant())

        val unlocksById = unlockData.associate { it.achievementId to it.unlockedOn }
        val achievements = achievementData.map { it.toAchievement(userId, unlockedOn = unlocksById[it.id]) }

        gamesDao += game
        achievementsDao += achievements
        return CreateGameResult(game, achievements)
    }
}

data class CreateGameResult(val game: Game, val achievements: List<Achievement>)