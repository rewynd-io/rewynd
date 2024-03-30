FROM forgejo.kensand.net/public/debian:latest
RUN apt update && \
    apt install -y ca-certificates-java && \
    apt install -y openjdk-17-jre-headless ffmpeg

COPY ./build/libs/rewynd-worker-all.jar /rewynd/worker.jar

WORKDIR /rewynd

CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=85", "-jar", "/rewynd/worker.jar"]
