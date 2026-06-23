package com.project.monolithic.repository

import com.project.monolithic.domain.OrderItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * OrderItemRepository.findAllByOrderId 파생 쿼리 검증 (실제 MySQL).
 */
@Tag("repository")
@Tag("integration")
class OrderItemRepositoryTest
    @Autowired
    constructor(
        private val orderItemRepository: OrderItemRepository,
    ) : AbstractRepositoryTest() {
        @Test
        fun `findAllByOrderId 는 해당 주문의 항목만 반환한다`() {
            // given: 주문 1 에 항목 2개, 주문 2 에 항목 1개
            orderItemRepository.saveAll(
                listOf(
                    OrderItem(orderId = 1, productId = 10, quantity = 2),
                    OrderItem(orderId = 1, productId = 11, quantity = 3),
                    OrderItem(orderId = 2, productId = 12, quantity = 1),
                ),
            )

            // when: 주문 1 의 항목 조회
            val items = orderItemRepository.findAllByOrderId(1)

            // then: 주문 1 의 항목 2개만, 모두 orderId=1
            assertEquals(2, items.size)
            assertTrue(items.all { it.orderId == 1L })
            assertEquals(setOf(10L, 11L), items.map { it.productId }.toSet())
        }

        @Test
        fun `findAllByOrderId 는 일치하는 항목이 없으면 빈 리스트를 반환한다`() {
            // given: 주문 1 에만 항목 존재
            orderItemRepository.save(OrderItem(orderId = 1, productId = 10, quantity = 2))

            // when: 항목이 없는 주문 99 조회
            val items = orderItemRepository.findAllByOrderId(99)

            // then: 빈 리스트(널이 아님)
            assertTrue(items.isEmpty())
        }
    }
