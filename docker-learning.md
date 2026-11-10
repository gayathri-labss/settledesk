
# Docker Learning Notes

## Table of Contents

1. What is Docker?
2. Docker Images and Containers
3. Docker Architecture
4. Docker Registry
5. Volumes and Persistent Storage
6. Docker Networking
7. Dockerfile
8. Image Building, Layers and Caching
9. Container Lifecycle
10. Docker Compose
11. Production Docker Practices
12. Useful Docker Commands
13. Applying Docker to SettleDesk

---

# 1. What is Docker?

Docker is a platform used to package and run applications in containers.

An application usually depends on:
- Programming language runtime
- Libraries and dependencies
- Configuration
- Supporting services

Different machines may have different versions of these dependencies. This can cause the common problem:

"It works on my machine, but not on yours."

Docker helps solve this by packaging an application and its required runtime dependencies into an image and running it inside a container.

### Why Docker?

- Consistent development and deployment environments.
- Application isolation.
- Easy application distribution.
- Reproducible deployments.
- Efficient resource utilization.
- Simplified management of multiple services.

# 2. Docker Images and Containers

These are two fundamental Docker concepts.

## Docker Image

An image is a read-only template used to create containers.

It contains:
- Application files
- Runtime dependencies
- Filesystem content
- Image configuration

Examples:

- `eclipse-temurin:25-jre`
- `mcr.microsoft.com/mssql/server:2022-latest`
- `apache/kafka:4.4.0`

## Docker Container

A container is an instance created from an image.

It provides an isolated environment in which the application runs.

A container has a writable layer on top of the image's read-only layers.

### Image vs Container

| Image | Container |
|---|---|
| Template or blueprint | Instance created from an image |
| Read-only image layers | Image layers plus writable layer |
| Used to create containers | Runs an application |
| Can be pushed to a registry | Not what docker push uploads |

One image can create multiple containers.

### Basic Flow

```text
Dockerfile
    |
    | docker build
    v
Docker Image
    |
    | docker run
    v
Docker Container
```

# 3. Docker Architecture

Docker follows a client-server architecture.

It mainly consists of:

## 3.1 Docker Client

The Docker Client is the command-line interface through which we interact with Docker.

Examples:

```bash
docker ps
docker build -t settledesk .
docker run nginx
docker stop my-container
```

The client sends requests to the Docker Daemon through the Docker API.

## 3.2 Docker Daemon

The Docker Daemon (`dockerd`) is a background service responsible for managing Docker operations.

Responsibilities:

- Building images.
- Creating containers.
- Starting and stopping containers.
- Managing networks.
- Managing volumes.

The client sends requests, and the daemon performs the actual operations.

## 3.3 Docker Registry

A Docker Registry stores and distributes Docker images.

Docker Hub is a popular public registry.

Organizations can also use private registries.

## 3.4 Docker Objects

Common Docker objects include:

- Images
- Containers
- Networks
- Volumes

### What happens when we execute docker run nginx?

1. We enter the command in the terminal.
2. Docker Client sends a request to the Docker Daemon.
3. Daemon checks whether the image exists locally.
4. If the image is missing, Docker pulls it from a registry.
5. Docker creates a container from the image.
6. Docker starts the container.

### Docker run vs Docker create vs Docker start

- `docker run`: Creates and starts a new container.
- `docker create`: Creates a container without starting it.
- `docker start`: Starts an existing container.

### Docker on macOS

Linux containers require a Linux kernel.

Docker Desktop on macOS runs Linux containers inside a lightweight Linux virtual machine.

The Docker CLI on the Mac communicates with the Docker environment managed by Docker Desktop.

# 4. Docker Registry

A Docker Registry stores and distributes Docker images.

Typical workflow:

1. Build an image locally.
2. Tag the image.
3. Push the image to a registry.
4. Another developer pulls the image.
5. They create a container from the downloaded image.

### Important Commands

```bash
# Download an image
docker pull nginx

# Build an image
docker build -t settledesk:latest .

# Tag an image
docker tag settledesk:latest username/settledesk:latest

# Push an image
docker push username/settledesk:latest

# Pull the image on another machine
docker pull username/settledesk:latest
```

| Command | Purpose |
|---|---|
| docker build | Builds an image |
| docker tag | Adds an image tag |
| docker push | Uploads an image |
| docker pull | Downloads an image |
| docker run | Creates and starts a container |

Important: Docker push and pull operate on images, not running containers.

# 5. Volumes and Persistent Storage

Containers are replaceable. Database data should not depend on the lifetime of a particular container.

Imagine SQL Server is running inside a Docker container.

If database files are stored only in the container's writable layer, removing that container can remove the data too.

## What is a Docker Volume?

A Docker Volume is persistent storage managed by Docker.

It allows data to survive container removal.

### Volume Commands

```bash
# Create a volume
docker volume create sqlserver-data

# List volumes
docker volume ls

# Inspect a volume
docker volume inspect sqlserver-data
```

### Mount a Volume

```bash
docker run \
  --name sqlserver \
  -v sqlserver-data:/var/opt/mssql \
  mcr.microsoft.com/mssql/server:2022-latest
```

The target path must match the database image's documented data directory.

### What happens when a container is removed?

1. Remove the old container.
2. Keep the named volume.
3. Create a new compatible container.
4. Mount the same volume.
5. Access the previously stored data.

### Container Writable Layer vs Volume

| Writable Layer | Docker Volume |
|---|---|
| Belongs to a container | Managed separately |
| Removed with container | Can survive container removal |
| Suitable for temporary changes | Suitable for persistent data |

**Important:** Commands such as `docker volume rm` and `docker compose down -v` can delete persistent database data.

# 6. Docker Networking

Docker networking allows containers to communicate with each other and with external clients.

For SettleDesk, Spring Boot needs to communicate with:

- SQL Server
- Kafka

## 6.1 Types of Docker Networks

| Network | Purpose |
|---|---|
| Bridge | Connects containers on the same Docker host |
| Host | Shares the host network namespace |
| None | Disables normal container networking |

For SettleDesk, we will primarily use a user-defined bridge network.

### Network Commands

```bash
# Create a network
docker network create settledesk-network

# List networks
docker network ls

# Inspect a network
docker network inspect settledesk-network

# Connect a container to a network
docker network connect settledesk-network my-container
```

Containers on the same user-defined bridge network can communicate using their container names or network aliases.

## 6.2 What is Localhost?

This is one of the most important networking concepts.

`localhost` refers to the network namespace of the process making the connection.

### Scenario 1: Spring Boot runs directly on the Mac

SQL Server runs inside Docker, and its port is published to the Mac.

Spring Boot connects using:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433
```

Kafka:

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

### Scenario 2: Spring Boot runs inside Docker

Now Spring Boot runs in its own container.

If it connects to:

```text
localhost:1433
```

It looks for SQL Server inside the Spring Boot container itself.

But SQL Server is running in a different container.

Therefore, use the destination service name:

```properties
spring.datasource.url=jdbc:sqlserver://database:1433
spring.kafka.bootstrap-servers=kafka:9092
```

Here, `database` and `kafka` are example Docker Compose service names.

Both containers must share a Docker network.

## 6.3 What is Port Mapping?

Port mapping allows us to expose a container port to the host machine.

Syntax:

```text
HOST_PORT:CONTAINER_PORT
```

Example:

```bash
docker run -p 1434:1433 sqlserver-image
```

- First 1434: Host port.
- Second 1433: Container port.

The Mac can connect through:

```text
localhost:1434
```

Another container on the shared network can connect through:

```text
database:1433
```

Important:

`EXPOSE` in a Dockerfile documents a port. It does not publish that port to the host.

For publishing, use `-p` or Compose `ports`.

# 7. Dockerfile

A Dockerfile is a text file containing instructions used to build a Docker image.

It tells Docker:

- Which base image to use.
- Where to place application files.
- Which commands to execute during building.
- Which application command to execute when the container starts.

### Example Dockerfile

```dockerfile
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY target/settledesk.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

The JAR filename must match the actual build output.

## Important Dockerfile Instructions

| Instruction | Purpose |
|---|---|
| FROM | Specifies the base image |
| WORKDIR | Sets working directory |
| COPY | Copies files into the image |
| ADD | Copies files and supports additional features |
| RUN | Executes commands during image building |
| ENV | Defines environment variables |
| ARG | Defines build-time variables |
| EXPOSE | Documents an expected container port |
| CMD | Provides the default command or arguments |
| ENTRYPOINT | Defines the main executable |
| USER | Sets the user for subsequent operations/runtime |
| VOLUME | Declares a mount point |

## RUN vs CMD vs ENTRYPOINT

These three are frequently confused.

### RUN

Executes during image building.

```dockerfile
RUN echo "Preparing application image"
```

### CMD

Provides the default command or arguments when the container starts.

### ENTRYPOINT

Defines the main executable that runs when the container starts.

Example:

```dockerfile
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY target/settledesk.jar app.jar

RUN echo "Preparing image"

ENTRYPOINT ["java", "-jar", "app.jar"]
```

The RUN command executes during the build.

The Java command executes when the container starts.

## EXPOSE vs Port Publishing

```dockerfile
EXPOSE 8080
```

This documents that the application listens on port 8080.

It does not publish the port to the host.

To publish it:

```bash
docker run -p 8080:8080 settledesk:latest
```

# 8. Image Building, Layers and Caching

## 8.1 Build Context

When executing:

```bash
docker build -t settledesk .
```

The dot (`.`) represents the current directory as the build context.

Docker makes files from this context available to Dockerfile instructions such as COPY and ADD, subject to `.dockerignore`.

Keeping the build context small improves build efficiency.

## 8.2 .dockerignore

`.dockerignore` excludes files and directories from the build context.

Example:

```text
.git
.idea
.vscode
*.log
.env
```

This prevents unnecessary files from being included in the build context.

Do not rely on `.dockerignore` as the only protection for secrets.

## 8.3 Docker Image Layers

Docker images consist of layers.

Filesystem-changing build instructions commonly create layers.

Some instructions change image configuration rather than creating filesystem layers.

Images can share common layers, reducing storage and download overhead.

Conceptually:

```text
Application Image
    |
    | Application files
    |
    | Runtime dependencies
    |
    | Base image
```

## 8.4 Docker Layer Caching

Docker can reuse cached results from previous builds when instructions and their relevant inputs have not changed.

If only the application JAR changes, Docker may reuse earlier unchanged layers.

Benefits:

- Faster builds.
- Less unnecessary work.
- Efficient development workflow.

Example:

```bash
docker build -t settledesk:latest .
```

To build without using the cache:

```bash
docker build --no-cache -t settledesk:latest .
```

Use `--no-cache` only when needed because it makes builds slower.

# 9. Container Lifecycle

Containers go through different states during their lifetime.

Typical lifecycle:

1. Create.
2. Start.
3. Run application.
4. Stop or restart.
5. Remove when no longer needed.

## Important Commands

### Create

```bash
docker create --name my-nginx nginx
```

Creates a container without starting it.

### Start

```bash
docker start my-nginx
```

Starts an existing container.

### Run

```bash
docker run --name another-nginx nginx
```

Creates and starts a new container.

### Stop

```bash
docker stop my-nginx
```

Gracefully stops the container.

### Restart

```bash
docker restart my-nginx
```

Stops and starts the existing container again.

### Remove

```bash
docker rm my-nginx
```

Removes a stopped container.

Force-remove a running container:

```bash
docker rm -f my-nginx
```

Removing a container does not automatically remove its image or separately managed named volumes.

## Inspect and Troubleshoot

```bash
# Running containers
docker ps

# All containers
docker ps -a

# Container logs
docker logs my-container

# Follow live logs
docker logs -f my-container

# Inspect container configuration
docker inspect my-container

# Resource usage
docker stats

# Processes inside container
docker top my-container
```

## Docker Exec

`docker exec` executes a new process inside an already-running container.

It does not create another container.

```bash
docker exec -it my-container sh
```

Or, if Bash is installed:

```bash
docker exec -it my-container bash
```

Flags:

- `-i`: Keeps standard input open.
- `-t`: Allocates a terminal.
- `sh` or `bash`: Opens a shell inside the container.

Exit using:

```bash
exit
```

# 10. Docker Compose

Docker Compose is a tool used to define and manage multiple containers using a YAML configuration file.

For SettleDesk, we need:

- Spring Boot
- SQL Server
- Kafka

Instead of running separate Docker commands for each service, we can define everything in one Compose file.

Usually named:

`compose.yaml`

or

`docker-compose.yml`

## Basic Compose Example

This is an illustrative structure, not a complete production-ready configuration.

```yaml
services:
  app:
    image: settledesk:latest
    ports:
      - "8080:8080"

  database:
    image: mcr.microsoft.com/mssql/server:2022-latest
    ports:
      - "1433:1433"

  kafka:
    image: apache/kafka:4.4.0
    ports:
      - "9092:9092"
```

## Important Compose Keywords

| Keyword | Purpose |
|---|---|
| services | Defines services/containers |
| image | Specifies image |
| build | Builds an image using Dockerfile |
| ports | Maps host and container ports |
| environment | Defines environment variables |
| env_file | Loads variables from a file |
| volumes | Mounts persistent storage |
| networks | Connects services |
| depends_on | Defines startup dependencies |
| healthcheck | Defines health checks |
| restart | Configures restart behavior |

## Services

A service defines how a container should run.

Example:

```yaml
services:
  app:
    image: settledesk:latest

  database:
    image: mcr.microsoft.com/mssql/server:2022-latest
```

Here, `app` and `database` are service names.

Containers in the same Compose network can generally communicate using these service names.

For example:

```properties
spring.datasource.url=jdbc:sqlserver://database:1433
```

## Volumes in Compose

```yaml
services:
  database:
    image: mcr.microsoft.com/mssql/server:2022-latest
    volumes:
      - sqlserver-data:/var/opt/mssql

volumes:
  sqlserver-data:
```

This creates a named volume for persistent SQL Server data.

## Environment Variables

Never hardcode sensitive database credentials in your application image.

Example:

```yaml
services:
  database:
    image: mcr.microsoft.com/mssql/server:2022-latest
    environment:
      ACCEPT_EULA: "Y"
      MSSQL_SA_PASSWORD: ${DB_PASSWORD}
```

Compose can read the variable from the shell environment or an environment file.

Example `.env`:

```dotenv
DB_USERNAME=sa
DB_PASSWORD=replace-with-a-strong-local-secret
```

Keep real `.env` files out of Git.

A `.env` file is convenient configuration, not a complete production secrets-management solution.

## depends_on

Example:

```yaml
services:
  app:
    depends_on:
      - database

  database:
    image: mcr.microsoft.com/mssql/server:2022-latest
```

This controls startup order.

Important: `depends_on` alone does not guarantee SQL Server is ready to accept connections.

For readiness-aware startup, configure a valid health check and use a supported `service_healthy` condition.

Applications should also handle temporary dependency failures.

## Important Compose Commands

```bash
# Start services in background
docker compose up -d

# Build and start services
docker compose up -d --build

# List services
docker compose ps

# View logs
docker compose logs

# Follow live logs
docker compose logs -f

# Stop services without removing containers
docker compose stop

# Stop and remove containers and networks
docker compose down
```

Important:

```bash
docker compose down -v
```

This also removes declared named and attached anonymous volumes. It can delete persistent database data.

Use it carefully.

# 11. Production Docker Practices

A working local container is not automatically production-ready.

We need to consider image size, security, configuration and reliability.

## 11.1 Multi-stage Builds

A multi-stage Dockerfile uses multiple FROM instructions.

For Java applications:

- Build stage uses Maven and JDK to compile and package the application.
- Runtime stage uses a smaller Java runtime image.
- Only the final JAR is copied into the runtime image.

Example:

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

This example assumes the project builds from the copied files and produces a JAR in `target/`.

Skipping tests during the Docker build is not a substitute for running tests in CI.

## 11.2 Non-root Users

Many containers run as root by default.

Running the application as a non-root user reduces security risks.

Example:

```dockerfile
USER 10001
```

The exact user creation process depends on the base image. Ensure required files and directories have appropriate permissions.

## 11.3 Secrets Management

Never hardcode:

- Database passwords.
- API keys.
- Access tokens.
- Private keys.

Avoid storing secrets in Dockerfiles, source code, image layers or committed Compose files.

Provide secrets at runtime using a suitable secrets-management approach.

## 11.4 Health Checks

A container can be running even when the application is not ready to serve requests.

Health checks help detect service health.

Example:

```dockerfile
HEALTHCHECK --interval=30s --timeout=5s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

This requires:

- Spring Boot Actuator configured.
- The health endpoint available.
- curl installed in the final image.

A health check must use a command available in the image and test the intended readiness.

## 11.5 Image Optimization

Useful practices:

- Use multi-stage builds.
- Choose maintained base images.
- Keep build context small.
- Use `.dockerignore`.
- Avoid unnecessary packages.
- Use Docker layer caching.
- Keep build and runtime dependencies separate.
- Use explicit image versions.
- Update base images deliberately.

## 11.6 Security and Best Practices

- Run containers as non-root where possible.
- Keep base images and dependencies updated.
- Scan images for known vulnerabilities.
- Avoid embedding secrets.
- Use least privilege.
- Set appropriate CPU and memory limits.
- Configure logging and monitoring.
- Use health checks.
- Configure restart policies.
- Avoid blindly relying on `latest` tags.

# 12. Useful Docker Commands

## Images

```bash
docker images
docker pull nginx
docker build -t my-app:latest .
docker tag my-app:latest username/my-app:latest
docker push username/my-app:latest
docker rmi my-app:latest
```

## Containers

```bash
docker ps
docker ps -a
docker create --name my-container nginx
docker run --name my-container nginx
docker start my-container
docker stop my-container
docker restart my-container
docker rm my-container
docker logs my-container
docker logs -f my-container
docker exec -it my-container sh
docker inspect my-container
docker stats
```

## Networks

```bash
docker network ls
docker network create my-network
docker network inspect my-network
docker network connect my-network my-container
```

## Volumes

```bash
docker volume ls
docker volume create my-volume
docker volume inspect my-volume
docker volume rm my-volume
```

## Docker Compose

```bash
docker compose up -d
docker compose up -d --build
docker compose ps
docker compose logs -f
docker compose stop
docker compose down
```

# 13. Applying Docker to SettleDesk

SettleDesk uses:

- Java 25
- Spring Boot
- SQL Server
- Kafka

Our target architecture:

```text
                 SettleDesk
                     |
              Docker Network
                     |
       +-------------+-------------+
       |             |             |
       v             v             v
  Spring Boot     SQL Server      Kafka
   Container      Container     Container
                     |
                     v
              Docker Volume
             Persistent Data
```

## Configuration Principles

1. Build a Docker image for the Spring Boot application.
2. Run SQL Server and Kafka as separate services.
3. Connect services through a shared Docker network.
4. Use service names for internal communication.
5. Use a named volume for SQL Server data.
6. Supply database credentials through protected runtime configuration.
7. Configure health checks and readiness handling.
8. Publish only required ports.
9. Verify Kafka listener configuration for both internal and external clients.

## Host vs Container Connection

| Application location | SQL Server | Kafka |
|---|---|---|
| Spring Boot on Mac | localhost:1433 | localhost:9092 |
| Spring Boot inside Docker | database:1433 | kafka:9092 |

The service names must match the actual Compose service names or network aliases.

## Implementation Roadmap

1. Inspect the existing project and Docker infrastructure.
2. Create the Spring Boot Dockerfile.
3. Build and run the application image.
4. Configure Docker Compose.
5. Configure SQL Server and Kafka networking.
6. Configure persistent database storage.
7. Configure environment variables and secrets.
8. Add health checks.
9. Verify application, database and Kafka connectivity.
10. Verify the complete business event flow.

---

# Final Reminders

- Image = Template.
- Container = Running instance of an image.
- Registry = Stores and distributes images.
- Volume = Persistent storage.
- Docker Client = Sends Docker commands.
- Docker Daemon = Manages Docker operations.
- Docker Network = Enables container communication.
- Dockerfile = Instructions to build an image.
- Docker Compose = Manages multiple services.
- `localhost` = The machine or network namespace making the connection.
- `EXPOSE` documents a port; `-p` publishes a port.
- `RUN` executes during build; `CMD` and `ENTRYPOINT` configure startup.
- `depends_on` startup order does not guarantee readiness.
- `docker compose down -v` can delete persistent data.



