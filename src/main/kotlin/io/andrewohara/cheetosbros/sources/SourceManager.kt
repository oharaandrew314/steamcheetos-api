package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.*
import io.andrewohara.cheetosbros.api.users.UsersDao
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.Instant

class SourceManager(
    private val sourceFactory: SourceFactory,
    private val usersDao: UsersDao,
    private val gamesDao: GamesDao,
    private val gameLibraryDao: GameLibraryDao,
    private val achievementsDao: AchievementsDao,
    private val achievementStatusDao: AchievementStatusDao,
    private val gameCacheDuration: Duration
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(job: Job, time: Instant): Collection<Job> {
        log.info("Action=Sync Job=$job")

        val user = usersDao[job.userId] ?: throw IllegalArgumentException("User not found: ${job.userId}")
        val player = user.players[job.platform] ?: throw IllegalArgumentException("${job.platform} player not found for user ${job.userId}")
        val game = job.gameId?.let(gamesDao::get)

        val nextJobs = if (game == null) {
            val gameIds = discoverGames(player)
            gameIds.map { Job(userId = job.userId, platform = job.platform, gameId = it) }
        } else {
            syncGame(player, game, time)
            emptySet()
        }

        log.info("Action=SyncComplete Job=$job NextJobs=${nextJobs.size}")
        return nextJobs
    }

    fun discoverGames(player: Player): Collection<Uid> {
        val source = sourceFactory[player] ?: throw IllegalArgumentException("No source found for player: ${player.username}")

        val games = source.library(player.uid.id)

        val existingUids = gamesDao
            .batchGet(games.map { it.uid })
            .map { it.uid }
            .toSet()

        for (game in games.filter { it.uid !in existingUids }) {
            gamesDao.create(game)
        }

        return games.map { it.uid }
    }

    fun syncGame(player: Player, existing: CachedGame, time: Instant) {
        val source = sourceFactory[player] ?: throw IllegalArgumentException("No source found for player: ${player.username}")

        val numAchievements = if (existing.lastUpdated == null || Duration.between(existing.lastUpdated, time) > gameCacheDuration) {
            val achievements = source.achievements(existing.uid.id)
            achievementsDao.batchSave(existing.uid, achievements)
            gamesDao.save(existing.copy(achievements = achievements.size, lastUpdated = time))

            achievements.size
        } else existing.achievements

        val numCompleted = if (numAchievements > 0) {
            val progress = source.userAchievements(existing.uid.id, player.uid.id)
            achievementStatusDao.batchSave(player.uid, existing.uid, progress)
            progress.count { it.unlockedOn != null }
        } else 0

        gameLibraryDao.save(player, OwnedGame(existing.uid, numCompleted, time))
    }
}