package com.dev_high.batch

import com.dev_high.auction.application.AuctionLifecycleService
import com.dev_high.auction.application.dto.AuctionProductProjection
import com.dev_high.auction.domain.AuctionRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class BatchHelper(
    private val auctionRepository: AuctionRepository,
    private val lifecycleService: AuctionLifecycleService,
) {
    fun startAuctionsUpdate(stepContribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val targetIds: List<AuctionProductProjection> = auctionRepository.bulkUpdateStartStatus()

        val auctionIds = targetIds.mapNotNull { it.id }.distinct()
        val productIds = targetIds.mapNotNull { it.productId }.distinct()
        log.info("auction start target>> {}", targetIds.size)

        val ec: ExecutionContext = chunkContext.stepContext.stepExecution.jobExecution.executionContext
        ec.put("startAuctionIds", auctionIds)
        ec.put("startProductIds", productIds)

        return RepeatStatus.FINISHED
    }

    fun startAuctionsPostProcessing(stepContribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val ec: ExecutionContext = chunkContext.stepContext.stepExecution.jobExecution.executionContext

        @Suppress("UNCHECKED_CAST")
        val auctionIds = ec["startAuctionIds"] as? List<String>

        if (!auctionIds.isNullOrEmpty()) {
            lifecycleService.startBulkProcessing(auctionIds)
        }
        ec.remove("startAuctionIds")
        ec.remove("startProductIds")

        return RepeatStatus.FINISHED
    }

    fun endAuctionsUpdate(stepContribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val targetIds: List<AuctionProductProjection> = auctionRepository.bulkUpdateEndStatus()

        val auctionIds = targetIds.mapNotNull { it.id }.distinct()
        val productIds = targetIds.mapNotNull { it.productId }.distinct()

        log.info("auction end target>> {}", targetIds.size)
        val ec: ExecutionContext = chunkContext.stepContext.stepExecution.jobExecution.executionContext

        ec.put("endAuctionIds", auctionIds)
        ec.put("endProductIds", productIds)

        return RepeatStatus.FINISHED
    }

    fun endAuctionsPostProcessing(stepContribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val ec: ExecutionContext = chunkContext.stepContext.stepExecution.jobExecution.executionContext

        @Suppress("UNCHECKED_CAST")
        val auctionIds = ec["endAuctionIds"] as? List<String>

        if (!auctionIds.isNullOrEmpty()) {
            lifecycleService.endBulkProcessing(auctionIds)
        }
        ec.remove("endAuctionIds")
        ec.remove("endProductIds")

        return RepeatStatus.FINISHED
    }

    companion object {
        private val log = LoggerFactory.getLogger(BatchHelper::class.java)
    }
}
