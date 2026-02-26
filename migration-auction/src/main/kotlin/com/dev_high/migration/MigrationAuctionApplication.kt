package com.dev_high.migration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "com.dev_high.migration",
        "com.dev_high.auction",
        "com.dev_high.exception",
    ]
)
class MigrationAuctionApplication

fun main(args: Array<String>) {
    runApplication<MigrationAuctionApplication>(*args)
}
