# Etapa 1: compilar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn --batch-mode --no-transfer-progress package -DskipTests

# Etapa 2: ejecutar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/mecania-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
