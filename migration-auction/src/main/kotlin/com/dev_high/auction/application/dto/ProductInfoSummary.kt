package com.dev_high.auction.application.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProductInfoSummary(
    val id: String,
    val name: String,
    val description: String,
    val categories: List<CategoryInfo>,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CategoryInfo(
        val id: String,
        val name: String,
    )
}
