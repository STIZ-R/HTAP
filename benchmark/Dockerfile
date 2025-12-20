# Étape build
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

COPY ./pom.xml ./benchmark/pom.xml
COPY ./src ./benchmark/src

RUN mvn -f benchmark/pom.xml clean package -DskipTests

# Étape runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copier le jar du benchmark
COPY --from=build /app/benchmark/target/benchmarks-1.0-SNAPSHOT.jar ./benchmarks.jar

# Commande par défaut : lancer ton runner mixte
CMD ["java", "-cp", "benchmarks.jar", "com.poc.meta.HTAPRunner"]
