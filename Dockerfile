FROM library/openjdk:11-jre
ARG CACHINGSERVICE_VERSION=1.4

COPY target/CachingService-${CACHINGSERVICE_VERSION}-jar-with-dependencies.jar /app/CachingService.jar
WORKDIR /app

EXPOSE 8080
CMD ["java", "-jar", "/app/CachingService.jar"]