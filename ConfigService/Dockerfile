FROM java:8
ADD /ce-config-0.0.1-SNAPSHOT.jar //
ENTRYPOINT ["java", "-jar", "/ce-config-0.0.1-SNAPSHOT.jar"]

HEALTHCHECK --interval=10s --timeout=3s CMD curl -f http://localhost:8888/health || exit 1
