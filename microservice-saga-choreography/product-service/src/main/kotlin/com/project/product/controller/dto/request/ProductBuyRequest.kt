package com.project.product.controller.dto.request

import com.project.product.service.dto.command.ProductBuyCommand

data class ProductBuyRequest(
    val requestId: String,
    val productInfos: List<ProductInfo>,
) {
    fun toCommand(): ProductBuyCommand =
        ProductBuyCommand(
            requestId,
            productInfos.map { ProductBuyCommand.ProductInfo(it.productId, it.quantity) },
        )

    data class ProductInfo(
        val productId: Long,
        val quantity: Long,
    )
}
