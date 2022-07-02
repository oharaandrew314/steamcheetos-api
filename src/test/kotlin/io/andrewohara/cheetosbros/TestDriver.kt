package io.andrewohara.cheetosbros

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
import io.andrewohara.utils.jdk.SameThreadExecutorService
import io.andrewohara.utils.jdk.toClock
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.model.*
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.filter.CorsPolicy
import java.time.Duration
import java.time.Instant

val tNow: Instant = Instant.parse("2021-03-04T01:00:00Z")
val testCdnHost = Uri.of("https://cdn.cheetos.test")

class TestDriver: HttpHandler {

    private val clock = tNow.toClock()

    val tables = Storage.InMemory<DynamoTable>()

    private val dynamo = let {
        val userId = Attribute.string().required("userId")
        val gameId = Attribute.string().required("id")
        tables["games"] = DynamoTable(
            TableDescription(
                TableName = TableName.of("games"),
                AttributeDefinitions = listOf(
                    userId.asAttributeDefinition(),
                    gameId.asAttributeDefinition()
                ),
                KeySchema = listOf(
                    KeySchema(userId.name, KeyType.HASH),
                    KeySchema(gameId.name, KeyType.RANGE)
                )
            )
        )

        val libraryId = Attribute.string().required("libraryId")
        val achievementId = Attribute.string().required("id")
        tables["achievements"] = DynamoTable(
            TableDescription(
                TableName = TableName.of("achievements"),
                AttributeDefinitions = listOf(
                    libraryId.asAttributeDefinition(),
                    achievementId.asAttributeDefinition()
                ),
                KeySchema = listOf(
                    KeySchema(libraryId.name, KeyType.HASH),
                    KeySchema(achievementId.name, KeyType.RANGE)
                )
            )
        )

        FakeDynamoDb(tables, clock).client()
    }

    val gamesDao = GamesDao(dynamo, TableName.of("games"))
    val achievementsDao = AchievementsDao(dynamo, TableName.of("achievements"))
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
        executor = SameThreadExecutorService(),
        imageCdnHost = testCdnHost
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

    fun createGame(userId: String, gameData: GameData, achievementData: List<AchievementData>, unlockData: List<AchievementStatusData>, favourite: Boolean = false): CreateGameResult {
        val game = gameData
            .toGame(userId, clock.instant())
            .copy(favourite = favourite)

        val unlocksById = unlockData.associate { it.achievementId to it.unlockedOn }
        val achievements = achievementData.map { it.toAchievement(userId, unlockedOn = unlocksById[it.id]) }

        gamesDao += game
        achievementsDao += achievements
        return CreateGameResult(game, achievements)
    }
}

data class CreateGameResult(val game: Game, val achievements: List<Achievement>)