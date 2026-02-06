FROM eclipse-temurin:17-jdk

WORKDIR /bench

# HTAPBench
COPY benchmark_official/HTAPBench-master/target/*jar-with-dependencies.jar ./lib/htapbench.jar
COPY benchmark_official/HTAPBench-master ./HTAPBench-master

# Proxy (classes compilées)
# Remplacez ligne COPY proxy-jdbc
# Ligne 11-13 →
COPY proxy/proxy-jdbc/target/classes/ ./lib/proxy-jdbc/
COPY proxy/proxy-core/target/classes/ ./lib/proxy-core/
COPY proxy/utils/target/classes/ ./lib/utils/

CMD ["sleep", "infinity"]
