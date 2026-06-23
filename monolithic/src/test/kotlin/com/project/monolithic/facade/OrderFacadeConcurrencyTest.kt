package com.project.monolithic.facade

import com.project.monolithic.exception.BusinessException
import com.project.monolithic.service.OrderService
import com.project.monolithic.service.RedisLockService
import com.project.monolithic.service.dto.PlaceOrderCommand
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * "주문이 정확히 1번만 처리되는가" 의 동시성 축 검증.
 * 실제 Redis 분산락(RedisLockService)을 사용하므로 Redis 가 떠 있어야 한다.
 * OrderService 는 호출 횟수만 세는 mock 으로 대체해, 락이 동시 진입을 1회로 막는지를 본다.
 */
@Tag("integration")
@Tag("concurrency")
@SpringBootTest
class OrderFacadeConcurrencyTest
    @Autowired
    constructor(
        private val redisLockService: RedisLockService,
        private val stringRedisTemplate: StringRedisTemplate,
    ) {
        private val orderId = 1L
        private val lockKey = "order:monolithic:$orderId"

        @BeforeEach
        fun cleanUp() {
            // 이전 실행에서 남았을 수 있는 락 키 제거
            stringRedisTemplate.delete(lockKey)
        }

        @Test
        fun `동시에 같은 주문을 여러 번 요청해도 실제 주문 처리는 정확히 1번만 일어난다`() {
            // given: 처리 시간(200ms)을 흉내내는 주문 서비스 + 실제 Redis 락
            val processedCount = AtomicInteger(0)
            val orderService = mockk<OrderService>()
            every { orderService.placeOrder(any()) } answers {
                processedCount.incrementAndGet()
                Thread.sleep(200) // 락 점유 구간: 이 사이 다른 스레드는 진입 못 해야 한다
            }
            val facade = OrderFacade(orderService, redisLockService)

            val threadCount = 10
            val executor = Executors.newFixedThreadPool(threadCount)
            val ready = CountDownLatch(threadCount)
            val start = CountDownLatch(1)
            val done = CountDownLatch(threadCount)
            val success = AtomicInteger(0)
            val lockFailed = AtomicInteger(0)

            // when: 10개 스레드가 동시에 같은 orderId 로 주문 처리를 요청
            repeat(threadCount) {
                executor.submit {
                    ready.countDown()
                    start.await() // 모두 출발선에 모일 때까지 대기 → 진짜 동시 호출
                    try {
                        facade.placeOrder(PlaceOrderCommand(orderId))
                        success.incrementAndGet()
                    } catch (e: BusinessException) {
                        lockFailed.incrementAndGet() // 락 획득 실패
                    } finally {
                        done.countDown()
                    }
                }
            }
            ready.await()
            start.countDown() // 동시 출발 신호
            done.await()
            executor.shutdown()

            // then: 락을 잡은 1개만 실제 처리, 나머지 9개는 락 획득에 실패
            assertEquals(1, processedCount.get(), "실제 주문 처리는 정확히 1번이어야 한다")
            assertEquals(1, success.get())
            assertEquals(threadCount - 1, lockFailed.get())
        }
    }