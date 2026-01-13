# Étape build
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY proxy-core ./proxy-core
COPY utils ./utils
COPY executors ./executors
COPY proxy-api ./proxy-api
COPY proxy-pgwire ./proxy-pgwire

RUN mvn clean package -DskipTests

# Étape runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*

# Copier le JAR Spring Boot construit par proxy-api
COPY --from=build /app/proxy-api/target/proxy-api-1.0-SNAPSHOT.jar ./app.jar

# Script wait-for
COPY wait-for-postgres.sh /wait-for-postgres.sh
RUN chmod +x /wait-for-postgres.sh

ENV POSTGRES_HOST=postgres
ENV POSTGRES_PORT=5432

# Lancer Spring Boot après avoir attendu Postgres
CMD ["/bin/sh", "/wait-for-postgres.sh"]