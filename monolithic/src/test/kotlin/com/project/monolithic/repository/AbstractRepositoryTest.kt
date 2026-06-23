package com.project.monolithic.repository

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * 리포지토리 슬라이스 테스트의 공통 베이스.
 *
 * - @DataJpaTest 는 JPA 리포지토리/엔티티만 올리는 슬라이스이며, 각 테스트는 트랜잭션으로 감싸져 자동 롤백된다.
 * - 파생 쿼리(메서드 이름 → 쿼리)와 MySQL 방언을 실제로 검증해야 하므로, 기본 임베디드 DB 대체를 끄고
 *   (@AutoConfigureTestDatabase(replace = NONE)) Testcontainers 의 실제 MySQL 에 @ServiceConnection 으로 붙는다.
 * - 컨테이너는 static 으로 한 번만 띄워 모든 하위 테스트가 공유한다(Docker 필요).
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class AbstractRepositoryTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val mysql = MySQLContainer(DockerImageName.parse("mysql:8.0"))
    }
}
