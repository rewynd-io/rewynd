# Rewynd
![Woodpecker CI pipeline status badge](https://woodpecker-codeberg.kensand.net/api/badges/7/status.svg)![License badge](https://img.shields.io/badge/License-AGPL--v3-blue)![Release badge](https://img.shields.io/gitea/v/release/rewynd-io/rewynd?gitea_url=https%3A%2F%2Fcodeberg.org
)

<div style="background: yellow;">
<h2 style="color: darkred"><strong>Warning:</strong> Rewynd is in ALPHA state. Things will break.</h2>
</div>

Rewynd is a self-hosted media streaming server akin to Jellyfin, Plex, or Emby. Rewynd is scalabe and supports multiple implementations of  databases and caches.

An Android app and web UI is supported, and an iOS app is planned.
  
## Running

### Docker Omni Image
This is the quickest way to evaluate Rewynd, but is not horizontally scalable. It uses SQLite and an in-memory cache.
```shell
docker run -v "./rewynd-db.sqlite:/rewynd/rewynd-db.sqlite" codeberg.org/rewynd-io/rewynd-omni:latest
```

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