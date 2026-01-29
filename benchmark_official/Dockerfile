FROM eclipse-temurin:17-jdk

WORKDIR /bench

# HTAPBench fat jar
COPY benchmark_official/HTAPBench-master/target/*jar-with-dependencies.jar ./htapbench.jar
COPY benchmark_official/HTAPBench-master ./HTAPBench-master

# Proxy deps (déjà compilées)
COPY proxy/proxy-jdbc/target/classes ./proxy-jdbc
COPY proxy/proxy-core/target/classes ./proxy-core
COPY proxy/utils/target/classes ./utils

CMD ["sleep", "infinity"]
