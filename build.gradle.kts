plugins {
    id("org.springframework.boot") version "3.2.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    id("jacoco")
}

group = "com.cacheiq"
version = "4.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-validation:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-actuator:3.2.0")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    
    // PostgreSQL + pgvector
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("org.springframework.boot:spring-boot-starter-jdbc:3.2.0")
    
    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime:1.19.2")
    
    // WebClient for Groq
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.2.0")
    
    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // Micrometer (Prometheus)
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.2")
    
    // JTokkit for token counting
    implementation("com.knuddels:jtokkit:1.1.0")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("io.mockk:mockk:1.13.9")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<Jar>("jar") {
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
}