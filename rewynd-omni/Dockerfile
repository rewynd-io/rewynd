FROM forgejo.kensand.net/public/debian:latest
RUN apt update && \
    apt install -y ca-certificates-java && \
    apt install -y openjdk-17-jre-headless ffmpeg

COPY ./build/libs/rewynd-omni-all.jar /rewynd/omni.jar

WORKDIR /rewynd

CMD ["java", "-jar", "/rewynd/omni.jar"]
