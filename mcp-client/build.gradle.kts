plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}
val springAiVersion by extra("2.0.0")

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "mcp-client"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web/UI, OAuth client, MCP transport, AI model, and MCP-specific dynamic OAuth support.
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    implementation("org.springaicommunity:mcp-client-security:0.1.13")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
