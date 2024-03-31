FROM forgejo.kensand.net/public/debian:latest
RUN apt update && \
    apt install -y ca-certificates-java && \
    apt install -y openjdk-17-jre-headless ffmpeg

COPY ./build/libs/rewynd-server-all.jar /rewynd/server.jar

WORKDIR /rewynd

CMD ["java", "-jar", "/rewynd/server.jar"]
