plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "mcp-server"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(26)
	}
}

repositories {
	mavenCentral()
}

extra["springAiVersion"] = "2.0.0"

dependencies {
	// Persistence, MCP transport, OAuth authorization/resource servers, and MCP-specific OAuth integration.
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
	implementation("org.springaicommunity:mcp-server-security:0.1.13")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
