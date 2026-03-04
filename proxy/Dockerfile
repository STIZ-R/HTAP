# Étape build
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY proxy-core ./proxy-core
COPY utils ./utils
COPY executors ./executors
COPY proxy-api ./proxy-api
COPY proxy-jdbc ./proxy-jdbc

RUN mvn clean package -DskipTests

# Télécharge l'agent OpenTelemetry
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/otel-agent.jar

# Étape runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/proxy-api/target/proxy-api-1.0-SNAPSHOT.jar ./app.jar
# Copie l'agent depuis l'étape build
COPY --from=build /app/otel-agent.jar ./otel-agent.jar

COPY wait-for-postgres.sh /wait-for-postgres.sh
RUN chmod +x /wait-for-postgres.sh

ENV POSTGRES_HOST=postgres
ENV POSTGRES_PORT=5432
ENV JAVA_TOOL_OPTIONS="-javaagent:/app/otel-agent.jar"

CMD ["/bin/sh", "/wait-for-postgres.sh"]
