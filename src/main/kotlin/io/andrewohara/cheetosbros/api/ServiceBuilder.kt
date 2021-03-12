package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.SteamOpenID
import io.andrewohara.cheetosbros.api.games.*
import io.andrewohara.cheetosbros.api.users.SocialLinkDao
import io.andrewohara.cheetosbros.api.users.UsersDao
import io.andrewohara.cheetosbros.api.v1.AuthApiV1
import io.andrewohara.cheetosbros.api.v1.BaseApiV1
import io.andrewohara.cheetosbros.api.v1.GamesApiV1
import io.andrewohara.cheetosbros.sources.JobService
import io.andrewohara.cheetosbros.sources.JobsDao
import io.andrewohara.cheetosbros.sources.SourceFactoryImpl
import io.andrewohara.cheetosbros.sources.SourceManager
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import spark.Spark
import java.time.Duration
import java.time.Instant

class ServiceBuilder(
    dynamoDb: AmazonDynamoDB,
    gamesTableName: String, libraryTableName: String, achievementsTableName: String, achievementStatusTableName: String,
    usersTableName: String,  jobsTableName: String, steamKey: String, socialLinkTableName: String,
    private val frontendHost: String
) {
    companion object {
        fun fromEnv(steamKey: String) = ServiceBuilder(
            dynamoDb = AmazonDynamoDBClientBuilder.defaultClient(),
            gamesTableName = System.getenv("GAMES_TABLE"),
            achievementsTableName = System.getenv("ACHIEVEMENTS_TABLE"),
            achievementStatusTableName = System.getenv("ACHIEVEMENT_STATUS_TABLE"),
            usersTableName = System.getenv("USERS_TABLE"),
            socialLinkTableName = System.getenv("SOCIAL_LINK_TABLE"),
            libraryTableName = System.getenv("LIBRARY_TABLE"),
            steamKey = steamKey,
            frontendHost = System.getenv("FRONTEND_HOST"),
            jobsTableName = System.getenv("JOBS_TABLE")
        )
    }

    private val gamesDao = GamesDao(gamesTableName, dynamoDb)
    private val achievementsDao = AchievementsDao(achievementsTableName, dynamoDb)
    private val gameLibraryDao = GameLibraryDao(libraryTableName, dynamoDb)
    private val achievementStatusDao = AchievementStatusDao(achievementStatusTableName, dynamoDb)
    val jobsDao = JobsDao(dynamoDb, jobsTableName)
    val usersDao = UsersDao(usersTableName, dynamoDb)
    val socialLinks = SocialLinkDao(dynamoDb, socialLinkTableName)

    private val gamesManager = GamesManager(gamesDao, gameLibraryDao, achievementsDao, achievementStatusDao)

    val sourceManager = SourceManager(
        sourceFactory = SourceFactoryImpl(steamKey = steamKey),
        achievementsDao = achievementsDao,
        achievementStatusDao = achievementStatusDao,
        gamesDao = gamesDao,
        gameLibraryDao = gameLibraryDao,
        gameCacheDuration = Duration.ofDays(7),
        usersDao = usersDao
    )

    val jobService = JobService(sourceManager, jobsDao)
    private val steamOpenId = SteamOpenID(steamApi = SteamSource(steamKey))

    fun startSpark(authManager: AuthManager, port: Int? = null, cors: Boolean = false, decodeQueryParams: Boolean) {
        if (port != null) {
            Spark.port(port)
        }

        BaseApiV1()
        AuthApiV1(authManager, steamOpenId, frontendHost, decodeQueryParams)
        GamesApiV1(gamesManager, jobService) { Instant.now() }

        if (cors) {
            Spark.after(SparkCorsFilter(frontendHost))
        }

        Spark.awaitInitialization()
    }
}