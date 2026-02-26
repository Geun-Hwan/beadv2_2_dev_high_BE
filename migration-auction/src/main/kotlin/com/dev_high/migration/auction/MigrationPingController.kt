package com.dev_high.migration.auction

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/migration")
class MigrationPingController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> = mapOf("status" to "ok", "service" to "migration-auction")
}
