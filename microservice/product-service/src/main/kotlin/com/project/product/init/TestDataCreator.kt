package com.project.product.init

import com.project.product.domain.Product
import com.project.product.repository.ProductRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class TestDataCreator(
    private val productRepository: ProductRepository,
) {
    @PostConstruct
    fun createTestData() {
        productRepository.save(Product(quantity = 100L, price = 100L))
        productRepository.save(Product(quantity = 100L, price = 200L))
    }
}
