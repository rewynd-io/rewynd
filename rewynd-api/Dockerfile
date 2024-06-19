FROM codeberg.org/kensand/openjdk-21:latest

RUN apt update && \
    apt upgrade -y

COPY ./build/libs/rewynd-api-all.jar /rewynd/api.jar

WORKDIR /rewynd

CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=85", "-jar", "/rewynd/api.jar"]
