package com.project.monolithic.service

import com.project.monolithic.exception.BusinessException
import com.project.monolithic.repository.ProductRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun buyProduct(
        productId: Long,
        quantity: Long,
    ): Long {
        val product =
            productRepository.findByIdOrNull(productId)
                ?: throw BusinessException("상품을 찾을 수 없습니다: id=$productId")
        val totalPrice = product.calculateTotalPrice(quantity)
        product.decreaseStock(quantity)
        return totalPrice
    }
}
