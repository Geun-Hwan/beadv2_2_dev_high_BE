package com.dev_high.batch

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BatchConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val batchHelper: BatchHelper,
) {
    @Bean
    fun auctionLifecycleJob(): Job {
        return JobBuilder("auctionLifecycleJob", jobRepository)
            .start(startAuctionsUpdateStep())
            .next(startAuctionsPostProcessingStep())
            .next(endAuctionsUpdateStep())
            .next(endAuctionsPostProcessingStep())
            .build()
    }

    @Bean
    fun startAuctionsUpdateStep(): Step {
        return StepBuilder("startAuctionsUpdateStep", jobRepository)
            .tasklet(batchHelper::startAuctionsUpdate, txManager)
            .build()
    }

    @Bean
    fun startAuctionsPostProcessingStep(): Step {
        return StepBuilder("startAuctionsPostProcessingStep", jobRepository)
            .tasklet(batchHelper::startAuctionsPostProcessing, txManager)
            .build()
    }

    @Bean
    fun endAuctionsUpdateStep(): Step {
        return StepBuilder("endAuctionsUpdateStep", jobRepository)
            .tasklet(batchHelper::endAuctionsUpdate, txManager)
            .build()
    }

    @Bean
    fun endAuctionsPostProcessingStep(): Step {
        return StepBuilder("endAuctionsPostProcessingStep", jobRepository)
            .tasklet(batchHelper::endAuctionsPostProcessing, txManager)
            .build()
    }
}
