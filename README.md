# Rewynd
![Woodpecker CI pipeline status badge](https://woodpecker-codeberg.kensand.net/api/badges/7/status.svg)

Rewynd is a self-hosted media streaming server akin to Jellyfin, Plex, or Emby. It consists of one or more workers and one or more api servers, which coordinate work using a Database and a Cache. 

An Android app and web UI is provided, and support for IOS is planned.

## Development

### Prerequisites
1. `JDK >= 17` (Required)
2. `Docker` (Optional. Used for building containers, deploying dev harness)

### Deploying Dev Harness
Check that docker is running
```shell
docker ps
```

Initialize a docker swarm if not already initialized
```shell
docker swarm init
```

Deploy the dev harness as a swarm stack named `rewynd-harness`
```shell
docker stack deploy -c ./docker/docker-compose.dev.yaml rewynd-harness
```
This will create a docker stack with two containers a PostgreSQL database and a Redis single-node cache with persistence disabled. Rewynd will connect to these by default without additional configuration.

When you are done with the harness, you may delete the stack
```shell
docker stack rm rewynd-harness
```

### Building
From package root directory
```shell
./gradlew clean build
```

### Running API
```shell
./gradlew ':rewynd-api:run'
```

### Running Worker
```shell
./gradlew ':rewynd-worker:run'
```