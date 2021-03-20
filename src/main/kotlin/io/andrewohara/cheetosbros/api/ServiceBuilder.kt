package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import io.andrewohara.cheetosbros.api.auth.*
import io.andrewohara.cheetosbros.api.games.*
import io.andrewohara.cheetosbros.api.users.SocialLinkDao
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.api.users.UsersDao
import io.andrewohara.cheetosbros.api.v1.AuthApiV1
import io.andrewohara.cheetosbros.api.v1.BaseApiV1
import io.andrewohara.cheetosbros.api.v1.GamesApiV1
import io.andrewohara.cheetosbros.lib.PemUtils
import io.andrewohara.cheetosbros.sources.*
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import org.http4k.core.Method
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.filter.CorsPolicy
import org.http4k.filter.Only
import org.http4k.filter.OriginPolicy
import org.http4k.filter.ServerFilters
import org.http4k.lens.RequestContextKey
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import java.time.Duration
import java.time.Instant

class ServiceBuilder(
    gamesDao: GamesDao,
    libraryDao: GameLibraryDao,
    achievementsDao: AchievementsDao,
    progressDao: AchievementStatusDao,
    private val usersDao: UsersDao,
    jobsDao: JobsDao,
    private val socialLinkDao: SocialLinkDao,
    steamSource: Source,
    sourceFactory: SourceFactory,
    private val frontendHost: String
) {
    companion object {
        fun fromProps(props: Map<String, String>): ServiceBuilder {
            val dynamo = AmazonDynamoDBClientBuilder.defaultClient()

            val steamSource = SteamSource(props.getValue("STEAM_API_KEY"))

            return ServiceBuilder(
                gamesDao = GamesDao(props.getValue("GAMES_TABLE"), dynamo),
                achievementsDao = AchievementsDao(props.getValue("ACHIEVEMENTS_TABLE"), dynamo),
                progressDao = AchievementStatusDao(props.getValue("ACHIEVEMENT_STATUS_TABLE"), dynamo),
                usersDao = UsersDao(props.getValue("USERS_TABLE"), dynamo),
                socialLinkDao = SocialLinkDao(dynamo, props.getValue("SOCIAL_LINK_TABLE")),
                libraryDao = GameLibraryDao(props.getValue("LIBRARY_TABLE"), dynamo),
                sourceFactory = SourceFactoryImpl(steamSource),
                steamSource = steamSource,
                frontendHost = props.getValue("FRONTEND_HOST"),
                jobsDao = JobsDao(dynamo, props.getValue("JOBS_TABLE"))
            )
        }
    }

    val gamesManager = GamesManager(gamesDao, libraryDao, achievementsDao, progressDao)

    val sourceManager = SourceManager(
        sourceFactory = sourceFactory,
        achievementsDao = achievementsDao,
        achievementStatusDao = progressDao,
        gamesDao = gamesDao,
        gameLibraryDao = libraryDao,
        gameCacheDuration = Duration.ofDays(7),
        usersDao = usersDao
    )

    fun createJwtAuth(issuer: String, privateKey: String, publicKey: String) = JwtAuthorizationDao(
        issuer = issuer,
        privateKey = PemUtils.parsePEMFile(privateKey)!!,
        publicKey = PemUtils.parsePEMFile(publicKey)!!
    )

    val jobService = JobService(sourceManager, jobsDao)
    private val steamOpenId = SteamOpenID(steamApi = steamSource)

    fun createHttp(authDao: AuthorizationDao): RoutingHttpHandler {
        val contexts = RequestContexts()
        val authLens = RequestContextKey.optional<User>(contexts)

        val authManager = AuthManager(authDao, usersDao, socialLinkDao)

        val routes = routes(
            BaseApiV1.getRoutes(),
            AuthApiV1(authManager, steamOpenId, frontendHost, authLens).getRoutes(),
            GamesApiV1(gamesManager, jobService, authLens, Instant::now).getRoutes()
        )

        val corsPolicy = CorsPolicy(
            OriginPolicy.Only(frontendHost),
            headers = listOf("Authorization"),
            methods = listOf(Method.GET, Method.POST),
            credentials = true
        )

        return ServerFilters.InitialiseRequestContext(contexts)
            .then(ServerFilters.Cors(corsPolicy))
            .then(AuthFilter(authLens, authManager::exchange))
            .then(routes)
    }
}