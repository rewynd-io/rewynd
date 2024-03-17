FROM forgejo.kensand.net/public/debian:latest
RUN apt update && \
    apt install -y ca-certificates-java && \
    apt install -y openjdk-17-jre-headless

COPY ./build/libs/rewynd-api-all.jar /rewynd/api.jar

WORKDIR /rewynd

CMD ["java", "-jar", "/rewynd/api.jar"]
