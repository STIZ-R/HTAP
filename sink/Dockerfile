FROM eclipse-temurin:17-jre

COPY target/sink-1.0-SNAPSHOT-jar-with-dependencies.jar /app/sink.jar

WORKDIR /app

ENTRYPOINT ["java", "-jar", "sink.jar"]
