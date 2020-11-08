FROM openjdk:11
ADD target/configservice-0.0.1-SNAPSHOT.jar configservice-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "/configservice-0.0.1-SNAPSHOT.jar"]

HEALTHCHECK --interval=10s --timeout=3s CMD curl -f http://localhost:8888/health || exit 1
