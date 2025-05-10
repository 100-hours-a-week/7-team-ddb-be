plugins {
	id("org.springframework.boot") version "3.4.4"
	id("io.spring.dependency-management") version "1.1.4"
	kotlin("jvm") version "1.9.22"
	kotlin("plugin.spring") version "1.9.22"
	kotlin("plugin.jpa") version "1.9.22"
	kotlin("kapt") version "1.9.22"  // 추가: Kotlin Annotation Processing Tool
}

group = "com.dolpin"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

// QueryDSL Q클래스 생성 경로 설정
val querydslGeneratedDir = file("src/main/generated")

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.security:spring-security-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// QueryDSL 의존성 추가
	implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
	kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
	kapt("jakarta.persistence:jakarta.persistence-api")

	// GCS
	implementation("com.google.cloud:google-cloud-storage:2.50.0")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// Database
	implementation("org.postgresql:postgresql")
	implementation("org.hibernate.orm:hibernate-spatial")

	// Lombok (Java 코드용)
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// 테스트
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")

	//환경변수
	implementation("me.paulschwarz:spring-dotenv:3.0.0")
}

// QueryDSL 설정
sourceSets {
	main {
		java.srcDir(querydslGeneratedDir)
	}
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Q클래스 자동 삭제 설정
tasks.named("clean") {
	doLast {
		querydslGeneratedDir.deleteRecursively()
	}
}