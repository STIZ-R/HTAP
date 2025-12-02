# Étape build
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY proxy-core ./proxy-core
COPY utils ./utils
COPY executors ./executors

RUN mvn clean package -DskipTests

# Étape runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*

# Copier le JAR shaded (le bon !)
COPY --from=build /app/proxy-core/target/proxy.jar ./proxy.jar

# Script wait-for
COPY wait-for-postgres.sh /wait-for-postgres.sh
RUN chmod +x /wait-for-postgres.sh

ENV POSTGRES_HOST=postgres
ENV POSTGRES_PORT=5432

CMD ["/wait-for-postgres.sh"]
