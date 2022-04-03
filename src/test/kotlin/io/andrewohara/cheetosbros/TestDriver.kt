package io.andrewohara.cheetosbros

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.cheetosbros.api.ServiceBuilder
import io.andrewohara.cheetosbros.api.auth.FakeAuthorizationDao
import io.andrewohara.cheetosbros.games.*
import io.andrewohara.cheetosbros.jobs.Job
import io.andrewohara.cheetosbros.jobs.JobsDao
import io.andrewohara.cheetosbros.sync.FakeSteamBackend
import io.andrewohara.cheetosbros.sync.GameSyncResult
import io.andrewohara.dynamokt.DataClassTableSchema
import io.andrewohara.dynamokt.createTableWithIndices
import io.andrewohara.utils.jdk.toClock
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.filter.CorsPolicy
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import java.time.Instant

val tnow: Instant = Instant.parse("2021-03-04T01:00:00Z")

class TestDriver: HttpHandler {

    private val clock = tnow.toClock()

    private val dynamoBackend = MockDynamoBackend(clock)
    private val dynamoDb = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(MockDynamoDbV2(dynamoBackend))
        .build()

//    private val usersDao = dynamoDb
//        .table("users", DataClassTableSchema(User::class))
//        .also { it.createTableWithIndices() }
//        .let { UsersDao(it) }

    val gamesDao = dynamoDb
        .table("games", DataClassTableSchema(Game::class))
        .also { it.createTableWithIndices() }
        .let { GamesDao(it) }

    val achievementsDao = dynamoDb
        .table("achievements", DataClassTableSchema(Achievement::class))
        .also { it.createTableWithIndices() }
        .let { AchievementsDao(dynamoDb, it) }

    private val jobsDao = dynamoDb
        .table("jobs", DataClassTableSchema(Job::class))
        .also { it.createTableWithIndices() }
        .let { JobsDao(dynamoDb, it) }

    private var nextId = 0

    val steam = FakeSteamBackend()

    val gameService = ServiceBuilder.gameService(
        dynamo = dynamoDb,
        gamesTableName = "games",
        achievementsTableName = "achievements"
    )

    val syncService = ServiceBuilder.syncService(
        steamApiKey = "key",
        steamBackend = steam,
        gameService = gameService,
        clock = clock
    )

    val jobService = ServiceBuilder.jobService(
        dynamo = dynamoDb,
        jobsTableName = "jobs",
        clock = clock,
        syncService = syncService
    )

    val authService = ServiceBuilder.authService(
        authDao = FakeAuthorizationDao(),
        serverHost = Uri.of("https://cheetos-api.test.com"),
    )

    private val api = ServiceBuilder.api(
        gameService = gameService,
        jobService = jobService,
        authService = authService,
        syncService = syncService,
        corsPolicy = CorsPolicy.UnsafeGlobalPermissive
    )

    override fun invoke(request: Request) = api(request)

    fun createGame(userId: String, name: String, vararg achievementData: Triple<String, String, Instant?>): GameSyncResult {
        val game = Game(
            userId = userId,
            id = nextId++.toString(),
            displayImage = null,
            name = name,
            achievementsTotal = achievementData.size,
            achievementsUnlocked = 0,
            lastUpdated = clock.instant()
        )

        val achievements = achievementData.map { (cName, cDesc, unlocked) ->
            Achievement(
                libraryId = LibraryId(userId = game.userId, gameId = game.id),
                id = nextId++.toString(),
                name = cName,
                description = cDesc,
                hidden = false,
                iconLocked = null,
                iconUnlocked = null,
                score = 0,
                unlockedOn = unlocked
            )
        }

        gamesDao += game
        achievementsDao += achievements

        return GameSyncResult(game, achievements.toSet())
    }
}