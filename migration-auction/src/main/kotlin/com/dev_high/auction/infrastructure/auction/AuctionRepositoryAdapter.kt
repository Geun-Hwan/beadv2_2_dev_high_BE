package com.dev_high.auction.infrastructure.auction

import com.dev_high.auction.application.dto.AuctionFilterCondition
import com.dev_high.auction.application.dto.AuctionProductProjection
import com.dev_high.auction.domain.Auction
import com.dev_high.auction.domain.AuctionLiveState
import com.dev_high.auction.domain.AuctionRepository
import com.dev_high.auction.domain.AuctionStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneId

@Repository
class AuctionRepositoryAdapter(
    private val auctionJpaRepository: AuctionJpaRepository,
    private val entityManager: EntityManager,
) : AuctionRepository {
    override fun save(auction: Auction): Auction = auctionJpaRepository.save(auction)

    override fun findById(id: String): java.util.Optional<Auction> = auctionJpaRepository.findById(id)

    override fun findByIdIn(ids: List<String>): List<Auction> {
        if (ids.isEmpty()) return emptyList()
        return entityManager.createQuery(
            "select a from Auction a where a.id in :ids and a.deletedYn = 'N'",
            Auction::class.java,
        )
            .setParameter("ids", ids)
            .resultList
    }

    override fun findByProductIdAndDeletedYn(productId: String): List<Auction> {
        return auctionJpaRepository.findByProductIdAndDeletedYnOrderByCreatedAtDesc(productId, "N")
    }

    override fun existsByProductIdAndStatusInAndDeletedYn(
        productId: String,
        statuses: List<AuctionStatus>,
        deletedYn: String,
    ): Boolean {
        return auctionJpaRepository.existsByProductIdAndStatusInAndDeletedYn(productId, statuses, deletedYn)
    }

    override fun findByProductId(productId: String): List<Auction> {
        return auctionJpaRepository.findByProductIdOrderByCreatedAtDesc(productId)
    }

    override fun findByProductIdIn(productIds: List<String>): List<Auction> {
        if (productIds.isEmpty()) return emptyList()
        return entityManager.createQuery(
            "select a from Auction a where a.productId in :productIds and a.deletedYn = 'N'",
            Auction::class.java,
        )
            .setParameter("productIds", productIds)
            .resultList
    }

    override fun bulkUpdateStartStatus(): List<AuctionProductProjection> = auctionJpaRepository.bulkUpdateStart()

    override fun bulkUpdateEndStatus(): List<AuctionProductProjection> = auctionJpaRepository.bulkUpdateEnd()

    override fun bulkUpdateStatus(auctionIds: List<String>, status: AuctionStatus): List<String> {
        return auctionJpaRepository.bulkUpdateStatus(auctionIds, status.name)
    }

    override fun filterAuctions(condition: AuctionFilterCondition): Page<Auction> {
        val cb = entityManager.criteriaBuilder

        val countQuery = cb.createQuery(Long::class.java)
        val countRoot = countQuery.from(Auction::class.java)
        val countLiveJoin = countRoot.join<Auction, AuctionLiveState>("liveState", JoinType.LEFT)
        val countPredicates = buildPredicates(cb, countRoot, countLiveJoin, condition)
        countQuery.select(cb.count(countRoot)).where(*countPredicates.toTypedArray())
        val total = entityManager.createQuery(countQuery).singleResult ?: 0L

        val dataQuery = cb.createQuery(Auction::class.java)
        val dataRoot = dataQuery.from(Auction::class.java)
        val dataLiveJoin = dataRoot.join<Auction, AuctionLiveState>("liveState", JoinType.LEFT)
        val dataPredicates = buildPredicates(cb, dataRoot, dataLiveJoin, condition)
        dataQuery.select(dataRoot).where(*dataPredicates.toTypedArray())
        val orders = buildOrders(cb, dataRoot, condition.sort)
        if (orders.isNotEmpty()) {
            dataQuery.orderBy(orders)
        } else {
            dataQuery.orderBy(cb.desc(dataRoot.get<OffsetDateTime>("updatedAt")))
        }

        val offset = condition.pageNumber * condition.pageSize
        val content = entityManager.createQuery(dataQuery)
            .setFirstResult(offset)
            .setMaxResults(condition.pageSize)
            .resultList

        return PageImpl(
            content,
            PageRequest.of(condition.pageNumber, condition.pageSize, condition.sort),
            total,
        )
    }

    override fun getEndingSoonAuctionCount(status: AuctionStatus, withinHours: Int): Long? {
        val now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
        val until = now.plusHours(withinHours.toLong())

        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(Long::class.java)
        val root = query.from(Auction::class.java)
        query.select(cb.count(root)).where(
            cb.equal(root.get<AuctionStatus>("status"), status),
            cb.equal(root.get<String>("deletedYn"), "N"),
            cb.greaterThanOrEqualTo(root.get("auctionEndAt"), now),
            cb.lessThanOrEqualTo(root.get("auctionEndAt"), until),
        )
        return entityManager.createQuery(query).singleResult
    }

    override fun getAuctionCount(status: AuctionStatus, asOf: OffsetDateTime?): Long? {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(Long::class.java)
        val root = query.from(Auction::class.java)
        val predicates = mutableListOf<Predicate>()
        predicates.add(cb.equal(root.get<AuctionStatus>("status"), status))
        predicates.add(cb.equal(root.get<String>("deletedYn"), "N"))
        if (asOf != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), asOf))
        }
        query.select(cb.count(root)).where(*predicates.toTypedArray())
        return entityManager.createQuery(query).singleResult
    }

    private fun buildPredicates(
        cb: jakarta.persistence.criteria.CriteriaBuilder,
        root: jakarta.persistence.criteria.Root<Auction>,
        liveJoin: jakarta.persistence.criteria.Join<Auction, AuctionLiveState>,
        condition: AuctionFilterCondition,
    ): MutableList<Predicate> {
        val predicates = mutableListOf<Predicate>()
        val currentBid = liveJoin.get<BigDecimal>("currentBid")
        val effectiveBid: Expression<BigDecimal> = cb.selectCase<BigDecimal>()
            .`when`(
                cb.or(
                    cb.isNull(currentBid),
                    cb.equal(currentBid, BigDecimal.ZERO),
                ),
                root.get("startBid"),
            )
            .otherwise(currentBid)

        condition.deletedYn?.let { predicates.add(cb.equal(root.get<String>("deletedYn"), it)) }
        condition.status?.takeIf { it.isNotEmpty() }?.let { predicates.add(root.get<AuctionStatus>("status").`in`(it)) }
        condition.minBid?.let { predicates.add(cb.greaterThanOrEqualTo(effectiveBid, it)) }
        condition.maxBid?.let { predicates.add(cb.lessThanOrEqualTo(effectiveBid, it)) }
        condition.productId?.let { predicates.add(cb.equal(root.get<String>("productId"), it)) }
        condition.sellerId?.let { predicates.add(cb.equal(root.get<String>("sellerId"), it)) }
        condition.startFrom?.let { predicates.add(cb.greaterThanOrEqualTo(root.get("auctionStartAt"), it)) }
        condition.startTo?.let { predicates.add(cb.lessThanOrEqualTo(root.get("auctionStartAt"), it)) }
        condition.endFrom?.let { predicates.add(cb.greaterThanOrEqualTo(root.get("auctionEndAt"), it)) }
        condition.endTo?.let { predicates.add(cb.lessThanOrEqualTo(root.get("auctionEndAt"), it)) }
        return predicates
    }

    private fun buildOrders(
        cb: jakarta.persistence.criteria.CriteriaBuilder,
        root: jakarta.persistence.criteria.Root<Auction>,
        sort: Sort,
    ): List<jakarta.persistence.criteria.Order> {
        val orders = mutableListOf<jakarta.persistence.criteria.Order>()

        for (order in sort) {
            val property = when (order.property) {
                "createdAt" -> "createdAt"
                "auctionStartAt" -> "auctionStartAt"
                "auctionEndAt" -> "auctionEndAt"
                "updatedAt" -> "updatedAt"
                else -> throw IllegalArgumentException("허용되지 않은 정렬 필드")
            }
            orders.add(
                if (order.isAscending) cb.asc(root.get<Any>(property)) else cb.desc(root.get<Any>(property)),
            )
        }
        return orders
    }
}
