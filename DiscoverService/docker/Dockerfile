FROM java:8
ADD /ce-discover-0.0.1-SNAPSHOT.jar //
ENTRYPOINT ["java", "-jar", "/ce-discover-0.0.1-SNAPSHOT.jar"]

HEALTHCHECK --interval=10s --timeout=3s CMD curl -f http://localhost:8761/health || exit 1
