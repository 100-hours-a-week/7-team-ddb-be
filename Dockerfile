# 런타임
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Jenkins에서 빌드한 JAR 파일 복사
COPY build/libs/dolpin-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
