package com.project.product.controller.dto.request

import com.project.product.service.dto.command.ProductReserveCommand

data class ProductReserveRequest(
    val requestId: String,
    val items: List<ReserveItem>,
) {
    fun toCommand(): ProductReserveCommand =
        ProductReserveCommand(
            requestId = requestId,
            items = items.map { ProductReserveCommand.ReserveItem(it.productId, it.reserveQuantity) },
        )

    data class ReserveItem(
        val productId: Long,
        val reserveQuantity: Long,
    )
}
