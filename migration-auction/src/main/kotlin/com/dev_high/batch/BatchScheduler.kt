package com.dev_high.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class BatchScheduler(
    private val jobLauncher: JobLauncher,
    private val auctionLifecycleJob: Job,
    private val redisConnectionFactory: RedisConnectionFactory,
) {
    @Scheduled(cron = "\${auction.batch.lifecycle-cron:0 */10 * * * *}")
    fun runAuctionLifecycleJob() {
        try {
            jobLauncher.run(
                auctionLifecycleJob,
                JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters(),
            )
        } catch (e: Exception) {
            log.warn("auction 상태 변경 scheduled 실패 {}", e.message)
        }
    }

    @Scheduled(cron = "\${auction.batch.ranking-clear-cron:0 0 0 * * *}")
    fun clearTodayRanking() {
        try {
            redisConnectionFactory.connection.use { connection ->
                connection.serverCommands().flushDb()
                log.info("auction ranking redis db flushed at midnight")
            }
        } catch (e: Exception) {
            log.warn("auction ranking redis flush failed {}", e.message)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BatchScheduler::class.java)
    }
}
