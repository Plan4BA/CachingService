# Cachingservice
Microservice to download lectures from Campus Dual and fetch new plans every night
## deployment
### requirements
- Docker (Linux Kernel 3.10 oder Hyper-V Virtualization under Windows)[https://docs.docker.com/install/]
- recommendation: docker-compose[https://docs.docker.com/compose/install/]
- running DBService(https://github.com/Plan4BA/DBService)
### docker run 

``
docker run -d --rm --name cachingservice -e DBSERVICE_ENDPOINT=http://dbservice:8080 -e MAX_PARALLEL_JOBS=5 -e TIMESPAN_START=23:00 -e TIMESPAN_END=02:00 cachingservice 
``
### docker compose

```yaml
version: 3
services:
    cachingservice:
        container_name: cachingservice
        environment:
            - 'DBSERVICE_ENDPOINT=http://dbservice:8080'
            - 'MAX_PARALLEL_JOBS=5'
            - 'TIMESPAN_START=23:00'
            - 'TIMESPAN_END=02:00'
        image: cachingservice
```
## development
### requirements
- Docker & Docker compose(see deployment)
- Java(OpenJDK > 11)
- Maven 3
### build
build the executable JAR-Files
```bash
mvn clean package
```
building docker container:
```bash
docker build -t cachingservice .
```
### environment variables
- DBSERVICE_ENDPOINT
    - HTTP-URL on which the DBService(https://github.com/Plan4BA/DBService) is accessible
- MAX_PARALLEL_JOBS
    - how many parallel instances can download from Campus Dual
- TIMESPAN_START
    - starttime to download all lectures from Campus Dual and update all existing data
    - notation: HH:mm
    - Notice: the time is based on the time of the container --> usually UTC
- TIMESPAN_END
    - end to download all lectures from Campus Dual and update all existing data
    - notation: HH:mm
    - Notice: the time is based on the time of the container --> usually UTC
    




