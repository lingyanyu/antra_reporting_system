FROM openjdk:11
ADD target/discoverservice-0.0.1-SNAPSHOT.jar discoverservice-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "/discoverservice-0.0.1-SNAPSHOT.jar"]

HEALTHCHECK --interval=10s --timeout=3s CMD curl -f http://localhost:8761/health || exit 1
