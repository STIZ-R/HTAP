FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/proxy-pgwire-1.0-SNAPSHOT.jar /app/proxy-pgwire.jar

EXPOSE 5432

CMD ["java", "-jar", "proxy-pgwire.jar"]
