package io.andrewohara.cheetosbros.jobs

import io.andrewohara.cheetosbros.sync.SyncService
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration

class JobService(
    private val jobsDao: JobsDao,
    private val retention: Duration,
    private val clock: Clock,
    private val syncService: SyncService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun startSync(userId: String) {
        log.info("Start sync for user $userId`")
        val gameIds = syncService.discoverGames(userId)
        log.info("Discovered ${gameIds.size} games for user $userId")
        jobsDao += gameIds.map { gameId ->
            Job(userId = userId, gameId = gameId, expires = clock.instant() + retention)
        }
    }

    fun startSyncJob(userId: String, gameId: String) {
        val job = Job(userId = userId, gameId = gameId, expires = clock.instant() + retention)
        jobsDao += job
    }

    operator fun invoke(job: Job) {
        log.info(job.toString())

        val result = syncService.syncGame(job.userId, job.gameId)
        log.info("Synced $result")

        jobsDao -= job
    }

    fun countJobsInProgress(userId: String): Int {
        return jobsDao.count(userId)
    }
}