package com.project.product.controller.dto.request

import com.project.product.service.dto.command.ProductBuyCancelCommand

data class ProductBuyCancelRequest(
    val requestId: String,
) {
    fun toCommand(): ProductBuyCancelCommand = ProductBuyCancelCommand(requestId)
}
