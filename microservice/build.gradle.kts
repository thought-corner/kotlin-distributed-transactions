// 멀티모듈 루트: 플러그인 버전만 중앙에서 고정하고, 실제 적용은 각 서비스 모듈에서 한다.
plugins {
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.spring") version "2.3.21" apply false
    kotlin("plugin.jpa") version "2.3.21" apply false
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}
