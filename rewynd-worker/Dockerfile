FROM codeberg.org/kensand/openjdk-21:latest

RUN apt update && \
    apt upgrade -y && \
    apt install -y ffmpeg

COPY ./build/libs/rewynd-worker-all.jar /rewynd/worker.jar

WORKDIR /rewynd

CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=85", "-jar", "/rewynd/worker.jar"]
