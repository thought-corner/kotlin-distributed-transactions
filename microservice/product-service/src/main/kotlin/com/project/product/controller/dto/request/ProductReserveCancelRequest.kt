package com.project.product.controller.dto.request

import com.project.product.service.dto.command.ProductReserveCancelCommand

data class ProductReserveCancelRequest(
    val requestId: String,
) {
    fun toCommand(): ProductReserveCancelCommand = ProductReserveCancelCommand(requestId)
}
