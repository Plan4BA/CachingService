FROM library/openjdk:12
ARG CACHINGSERVICE_VERSION=2.1

COPY target/CachingService-${CACHINGSERVICE_VERSION}-jar-with-dependencies.jar /app/CachingService.jar
WORKDIR /app

EXPOSE 8080
CMD ["java", "-jar", "/app/CachingService.jar"]