package com.dev_high.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auction.recommendation")
class AuctionRecommendationProperties {
    var similarLimit: Int = 50
    var winningLimit: Int = 200
    var lookbackDays: Int = 180
    var startBidRatio: Double = 0.8
    var winningBlendWeight: Double = 0.8
    var auctionBlendWeight: Double = 0.2
    var timeDecayDays: Double = 90.0
    var minSimilarity: Double = 0.2
    var rangePercent: Double = 0.15
    var aiEnabled: Boolean = true
    var cacheTtlMinutes: Long = 30
    var durationHours: Long = 24
    var startDelayMinutes: Long = 60
    var statusWeight: MutableMap<String, Double> = hashMapOf()
}
