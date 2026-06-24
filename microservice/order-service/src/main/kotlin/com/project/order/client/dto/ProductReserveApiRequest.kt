package com.project.order.client.dto

data class ProductReserveApiRequest(
    val requestId: String,
    val items: List<ReserveItem>,
) {
    data class ReserveItem(
        val productId: Long,
        val reserveQuantity: Long,
    )
}
