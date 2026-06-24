package com.project.product.controller.dto.request

import com.project.product.service.dto.command.ProductReserveConfirmCommand

data class ProductReserveConfirmRequest(
    val requestId: String,
) {
    fun toCommand(): ProductReserveConfirmCommand = ProductReserveConfirmCommand(requestId)
}
