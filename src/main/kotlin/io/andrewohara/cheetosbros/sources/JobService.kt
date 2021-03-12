package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.Uid
import io.andrewohara.cheetosbros.api.users.User
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class JobService(
    private val service: SourceManager,
    private val jobsDao: JobsDao
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    fun execute(jobId: UUID, job: Job, time: Instant) {
        val nextJobs = service.execute(job, time)
        jobsDao.batchInsert(nextJobs, time)
        jobsDao.delete(userId = job.userId, jobId=jobId)
    }

    fun insertDiscoveryJob(user: User, player: Player, time: Instant) {
        if (countJobsInProgress(user.id) > 0) {
            log.warn("There are already jobs in progress for ${user.id}.  Will not start discovery job.")
            return
        }

        val job = Job(userId = user.id, platform = player.uid.platform, gameId = null)
        jobsDao.insert(job, time)
    }

    fun insertSyncGameJob(user: User, gameId: Uid, time: Instant) {
        val job = Job(userId = user.id, platform = gameId.platform, gameId = gameId)
        jobsDao.insert(job, time)
    }

    fun countJobsInProgress(userId: UUID) = jobsDao.count(userId)
}