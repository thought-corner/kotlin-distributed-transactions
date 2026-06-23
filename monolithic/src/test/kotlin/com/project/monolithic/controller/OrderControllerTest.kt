package com.project.monolithic.controller

import tools.jackson.databind.ObjectMapper
import com.project.monolithic.controller.dto.CreateOrderRequest
import com.project.monolithic.controller.dto.PlaceOrderRequest
import com.project.monolithic.exception.BusinessException
import com.project.monolithic.facade.OrderFacade
import com.project.monolithic.service.OrderService
import com.project.monolithic.service.dto.CreateOrderResult
import com.project.monolithic.service.dto.PlaceOrderCommand
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

/**
 * 컨트롤러 레이어(웹 슬라이스) 테스트.
 * @WebMvcTest 가 OrderController 와 관련 웹 인프라(자동 구성된 Jackson, MockMvc, 예외 처리)만 올린다.
 * 협력 빈(OrderService, OrderFacade)은 @MockitoBean 으로 대체해 컨트롤러의 책임만 격리 검증한다.
 * (Spring Boot 4 에서 @MockBean 이 제거되어 @MockitoBean 을 사용한다. mockito-kotlin 으로 스텁/검증.)
 */
@Tag("controller")
@WebMvcTest(OrderController::class)
class OrderControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper,
    ) {
        @MockitoBean
        private lateinit var orderService: OrderService

        @MockitoBean
        private lateinit var orderFacade: OrderFacade

        @Test
        fun `POST order 는 생성된 주문 id 를 응답으로 반환한다`() {
            // given: 서비스가 주문 id 42 를 생성한다고 가정
            whenever(orderService.createOrder(any())).thenReturn(CreateOrderResult(orderId = 42))
            val body =
                objectMapper.writeValueAsString(
                    CreateOrderRequest(listOf(CreateOrderRequest.OrderItem(productId = 10, quantity = 2))),
                )

            // when & then: POST /order 호출 → 200, 응답 body 의 orderId 가 42
            mockMvc
                .post("/order") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.orderId") { value(42) }
                }
        }

        @Test
        fun `POST order place 는 요청 orderId 를 커맨드로 변환해 파사드에 위임한다`() {
            // given: orderId 7 요청 body
            val body = objectMapper.writeValueAsString(PlaceOrderRequest(orderId = 7))

            // when: POST /order/place 호출
            mockMvc
                .post("/order/place") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isOk() }
                }

            // then: 파사드에 PlaceOrderCommand(7) 가 정확히 1회 위임됨
            verify(orderFacade).placeOrder(PlaceOrderCommand(7))
        }

        @Test
        fun `파사드가 BusinessException 을 던지면 409 CONFLICT 를 반환한다`() {
            // given: 락 획득 실패 등 비즈니스 예외
            doThrow(BusinessException("락획득에 실패하였습니다."))
                .whenever(orderFacade).placeOrder(any())
            val body = objectMapper.writeValueAsString(PlaceOrderRequest(orderId = 1))

            // when & then: 전역 핸들러가 409 로 매핑
            mockMvc.post("/order/place") {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isConflict() }
            }
        }

        @Test
        fun `도메인 불변식 위반(IllegalArgumentException)은 400 BAD REQUEST 를 반환한다`() {
            // given: 재고/포인트 부족 등 도메인 검증 실패
            doThrow(IllegalArgumentException("재고가 부족합니다"))
                .whenever(orderFacade).placeOrder(any())
            val body = objectMapper.writeValueAsString(PlaceOrderRequest(orderId = 1))

            // when & then: 전역 핸들러가 400 으로 매핑
            mockMvc.post("/order/place") {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }
