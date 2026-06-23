package com.project.monolithic.e2e

import com.project.monolithic.controller.dto.CreateOrderRequest
import com.project.monolithic.controller.dto.CreateOrderResponse
import com.project.monolithic.controller.dto.PlaceOrderRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper

/**
 * 주문 생성 → 결제까지의 전체 흐름을 실제 Spring 컨텍스트 + 실제 MySQL/Redis 로 검증하는 E2E 테스트.
 *
 * - 컨트롤러 → 파사드(분산락) → 서비스(@Transactional) → 도메인/리포지토리까지 모든 레이어가 실제로 동작한다.
 * - 외부 인프라(MySQL, Redis)는 Testcontainers 로 띄우고 @ServiceConnection 으로 자동 연결한다(Docker 필요).
 * - HTTP 호출은 MockMvc 로 디스패처 서블릿/필터 체인까지 태운다.
 *
 * 주의: 원본 OrderService.placeOrder 끝에 Thread.sleep(3000) 이 있어 결제 1건당 약 3초 소요된다.
 */
@Tag("e2e")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OrderE2ETest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper,
        private val jdbcTemplate: JdbcTemplate,
    ) {
        companion object {
            private const val PRODUCT_ID = 10L
            private const val PRODUCT_PRICE = 1_000L
            private const val INITIAL_STOCK = 100L
            private const val USER_ID = 1L // OrderService.placeOrder 가 사용하는 고정 userId
            private const val INITIAL_POINT = 100_000L

            @Container
            @ServiceConnection
            @JvmStatic
            val mysql = MySQLContainer(DockerImageName.parse("mysql:8.0"))

            @Container
            @ServiceConnection(name = "redis")
            @JvmStatic
            val redis =
                GenericContainer(DockerImageName.parse("redis:7-alpine"))
                    .apply { withExposedPorts(6379) }
        }

        @BeforeEach
        fun setUp() {
            // 매 테스트 전 데이터 초기화 후 시드 데이터 삽입
            jdbcTemplate.execute("DELETE FROM order_items")
            jdbcTemplate.execute("DELETE FROM orders")
            jdbcTemplate.execute("DELETE FROM products")
            jdbcTemplate.execute("DELETE FROM points")

            jdbcTemplate.update(
                "INSERT INTO products (id, price, stock) VALUES (?, ?, ?)",
                PRODUCT_ID,
                PRODUCT_PRICE,
                INITIAL_STOCK,
            )
            jdbcTemplate.update(
                "INSERT INTO points (id, user_id, amount) VALUES (?, ?, ?)",
                1L,
                USER_ID,
                INITIAL_POINT,
            )
        }

        @Test
        fun `주문을 생성하고 결제하면 주문이 완료되고 재고와 포인트가 차감된다`() {
            // given: 상품 2개 주문 생성
            val quantity = 2L
            val orderId = createOrder(PRODUCT_ID, quantity)

            // when: 결제
            placeOrder(orderId).andExpect { status { isOk() } }

            // then: 주문 완료 + 재고/포인트가 정확히 차감됨
            assertEquals("COMPLETED", orderStatus(orderId))
            assertEquals(INITIAL_STOCK - quantity, stockOf(PRODUCT_ID))
            assertEquals(INITIAL_POINT - PRODUCT_PRICE * quantity, pointOf(USER_ID))
        }

        @Test
        fun `결제 도중 포인트가 부족하면 트랜잭션이 롤백되어 재고도 원복된다`() {
            // given: 재고 차감은 가능하지만 결제 금액(2000)보다 포인트 잔액(100)이 부족한 상황
            jdbcTemplate.update("UPDATE points SET amount = ? WHERE user_id = ?", 100L, USER_ID)
            val quantity = 2L
            val orderId = createOrder(PRODUCT_ID, quantity)

            // when: 결제 시도 → 포인트 부족(IllegalArgumentException) → 전역 핸들러가 400 으로 매핑
            placeOrder(orderId).andExpect { status { isBadRequest() } }

            // then: @Transactional 롤백으로 재고/포인트/주문 상태가 그대로
            assertEquals(INITIAL_STOCK, stockOf(PRODUCT_ID), "재고 차감이 롤백되어야 한다")
            assertEquals(100L, pointOf(USER_ID), "포인트는 변하지 않아야 한다")
            assertEquals("CREATED", orderStatus(orderId), "주문은 완료되지 않아야 한다")
        }

        @Test
        fun `동일 주문을 두 번 결제해도 재고와 포인트는 한 번만 차감된다`() {
            // given: 주문 생성 후 1차 결제로 완료
            val quantity = 3L
            val orderId = createOrder(PRODUCT_ID, quantity)
            placeOrder(orderId).andExpect { status { isOk() } }

            // when: 이미 완료된 주문을 다시 결제(멱등)
            placeOrder(orderId).andExpect { status { isOk() } }

            // then: 차감은 1회분만 반영
            assertEquals("COMPLETED", orderStatus(orderId))
            assertEquals(INITIAL_STOCK - quantity, stockOf(PRODUCT_ID))
            assertEquals(INITIAL_POINT - PRODUCT_PRICE * quantity, pointOf(USER_ID))
        }

        private fun createOrder(
            productId: Long,
            quantity: Long,
        ): Long {
            val body =
                objectMapper.writeValueAsString(
                    CreateOrderRequest(listOf(CreateOrderRequest.OrderItem(productId, quantity))),
                )
            val responseBody =
                mockMvc
                    .post("/order") {
                        contentType = MediaType.APPLICATION_JSON
                        content = body
                    }.andExpect {
                        status { isOk() }
                    }.andReturn()
                    .response.contentAsString
            return objectMapper.readValue(responseBody, CreateOrderResponse::class.java).orderId
        }

        private fun placeOrder(orderId: Long) =
            mockMvc.post("/order/place") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(PlaceOrderRequest(orderId))
            }

        private fun orderStatus(orderId: Long): String =
            jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String::class.java, orderId)!!

        private fun stockOf(productId: Long): Long =
            jdbcTemplate.queryForObject("SELECT stock FROM products WHERE id = ?", Long::class.java, productId)!!

        private fun pointOf(userId: Long): Long =
            jdbcTemplate.queryForObject("SELECT amount FROM points WHERE user_id = ?", Long::class.java, userId)!!
    }
