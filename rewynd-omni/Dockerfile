FROM codeberg.org/kensand/openjdk-21:latest

RUN apt update && \
    apt upgrade -y && \
    apt install -y ffmpeg

COPY ./build/libs/rewynd-omni-all.jar /rewynd/omni.jar

WORKDIR /rewynd

CMD ["java", "-jar", "/rewynd/omni.jar"]
