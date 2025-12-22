# -------- Этап 1: Сборка JAR --------
FROM eclipse-temurin:23-jdk AS builder
WORKDIR /app

# Копируем Maven Wrapper
COPY .mvn/ .mvn/
COPY mvnw mvnw
WORKDIR /app

# Копируем pom.xml и код
COPY pom.xml .
COPY src/ ./src/

# Собираем JAR без тестов
RUN ./mvnw clean package -DskipTests


# -------- Этап 2: Запуск приложения --------
FROM eclipse-temurin:23-jre

WORKDIR /app

# Копируем JAR из этапа builder
COPY --from=builder /app/target/SpringBootReactiveWebOnlineStore-0.0.1-SNAPSHOT.jar app.jar

# Открываем порт
EXPOSE 8080

ENTRYPOINT ["./app.jar"]
