FROM library/openjdk:10-jre
ARG CACHINGSERVICE_VERSION=1.3

COPY target/CachingService-${CACHINGSERVICE_VERSION}-jar-with-dependencies.jar /app/CachingService.jar
WORKDIR /app

EXPOSE 8080
CMD ["java", "-jar", "/app/CachingService.jar"]