# -------------------------
# Étape 1 : Build proxy modules (inchangé)
# -------------------------
FROM maven:3.9.3-eclipse-temurin-17 AS proxy-build
WORKDIR /proxy
COPY proxy/pom.xml ./pom.xml
COPY proxy/proxy-core ./proxy-core
COPY proxy/executors ./executors
COPY proxy/proxy-api ./proxy-api
COPY proxy/proxy-pgwire ./proxy-pgwire
COPY proxy/utils ./utils
COPY proxy/proxy-jdbc ./proxy-jdbc
RUN mvn clean package -DskipTests -pl utils,proxy-jdbc -am

# -------------------------
# Étape 2 : Build benchmark (inchangé)
# -------------------------
FROM maven:3.9.3-eclipse-temurin-17 AS benchmark-build
WORKDIR /app
COPY --from=proxy-build /proxy/utils/target/utils-1.0-SNAPSHOT.jar ./libs/utils.jar
COPY --from=proxy-build /proxy/proxy-jdbc/target/proxy-jdbc-1.0-SNAPSHOT.jar ./libs/proxy-jdbc.jar
COPY benchmark /app
RUN mvn clean package -DskipTests
RUN ls -l target/

# -------------------------
# Étape 3 : Runtime + CLASS PATH CORRIGÉ
# -------------------------
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=benchmark-build /app/target/benchmark.jar ./benchmark.jar
COPY --from=benchmark-build /app/libs/ ./libs/
ENV JDBC_URL="jdbc:htap:http://htap-proxy:8080"
ENTRYPOINT ["sh", "-c", "java -cp 'benchmark.jar:libs/*' com.poc.meta.HTAPRunner"]
