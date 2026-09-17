# Etapa 1: compilar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn --batch-mode --no-transfer-progress package -DskipTests

# Etapa 2: ejecutar
FROM eclipse-temurin:21-jre
WORKDIR /app
# curl para el HEALTHCHECK del contenedor (Render ya hace su propio health check;
# este es para que el propio contenedor sepa si está vivo).
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
# La app no corre como root: se crea un usuario sin privilegios.
RUN useradd -r -m -u 1001 mecania
COPY --from=build /app/target/mecania-*.jar app.jar
RUN chown -R mecania:mecania /app
USER mecania
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=8s --start-period=120s --retries=3 \
  CMD curl -fsS http://localhost:8080/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
