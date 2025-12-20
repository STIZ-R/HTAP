# ===== STAGE 1 : build Maven =====
FROM maven:3.9-eclipse-temurin-17 AS build

# Copier le pom et les sources
WORKDIR /build
COPY pom.xml .
COPY src ./src

# Build du jar avec dépendances (adapter le goal si tu n'utilises pas assembly)
RUN mvn clean package -DskipTests

# ===== STAGE 2 : image runtime légère =====
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copier le jar généré depuis le stage build
# Adapter le nom du jar si nécessaire
COPY --from=build /build/target/sink-1.0-SNAPSHOT-jar-with-dependencies.jar /app/sink.jar

ENTRYPOINT ["java", "-jar", "sink.jar"]
