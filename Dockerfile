# 1단계 : 빌드
FROM eclipse-temurin:17-jdk-alpine as builder

WORKDIR /app

# 종속성 캐시 최적화를 위해 gradle 관련 파일 먼저 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

# 전체 소스 복사 후 다시 빌드
COPY . .
RUN ./gradlew clean build -x test --no-daemon

# 2단계 : 런타임
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/dolpin-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]