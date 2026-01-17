# PetBuddy Backend Services - Docker Setup

Complete Docker Compose setup for running all PetBuddy microservices locally.

## Quick Start

```bash
# 1. Copy environment file
cp .env.example .env

# 2. Start infrastructure only (recommended first)
docker-compose up -d postgres redis rabbitmq minio

# 3. Wait for infrastructure to be healthy (check logs)
docker-compose logs -f postgres redis rabbitmq minio

# 4. Start all services (builds and runs everything)
docker-compose up -d --build
```

## Service URLs

| Service                       | Port  | URL                    |
| ----------------------------- | ----- | ---------------------- |
| **Auth Service**              | 8081  | http://localhost:8081  |
| **User Profile Service**      | 8082  | http://localhost:8082  |
| **Feed Distribution Service** | 8083  | http://localhost:8083  |
| **Interaction Service**       | 8084  | http://localhost:8084  |
| **Social Feed Service**       | 8085  | http://localhost:8085  |
| **PostgreSQL**                | 5432  | localhost:5432         |
| **Redis**                     | 6379  | localhost:6379         |
| **RabbitMQ (AMQP)**           | 5672  | localhost:5672         |
| **RabbitMQ (Management)**     | 15672 | http://localhost:15672 |
| **MinIO (API)**               | 9000  | http://localhost:9000  |
| **MinIO (Console)**           | 9001  | http://localhost:9001  |
| **Prometheus**                | 9090  | http://localhost:9090  |
| **Grafana**                   | 3000  | http://localhost:3000  |

## Default Credentials

| Service    | Username   | Password      |
| ---------- | ---------- | ------------- |
| PostgreSQL | postgres   | postgres123   |
| RabbitMQ   | admin      | password123   |
| MinIO      | minioadmin | minioadmin123 |
| Grafana    | admin      | admin123      |

## Useful Commands

```bash
# View logs for a specific service
docker-compose logs -f auth-service

# Restart a service
docker-compose restart auth-service

# Rebuild a specific service
docker-compose up -d --build auth-service

# Stop all services
docker-compose down

# Stop and remove all data (fresh start)
docker-compose down -v

# Check service health
docker-compose ps
```

## Database Access

```bash
# Connect to PostgreSQL
docker exec -it petbuddy-postgres psql -U postgres

# List all databases
docker exec -it petbuddy-postgres psql -U postgres -c "\l"

# Connect to a specific database
docker exec -it petbuddy-postgres psql -U postgres -d auth_db
```

## Memory Optimization

The docker-compose is configured with memory limits for low-resource environments:

| Service           | Memory Limit           |
| ----------------- | ---------------------- |
| Each Java Service | 512MB (256MB reserved) |
| PostgreSQL        | 256MB                  |
| Redis             | 150MB                  |
| RabbitMQ          | 256MB                  |
| MinIO             | 256MB                  |
| Prometheus        | 256MB                  |
| Grafana           | 200MB                  |

**Total estimated memory: ~4GB** for all services running.

## Troubleshooting

### Services fail to start

```bash
# Check if infrastructure is healthy
docker-compose ps

# Check specific service logs
docker-compose logs auth-service

# Increase start_period in healthcheck if builds are slow
```

### Database connection issues

```bash
# Ensure postgres is running and healthy
docker-compose logs postgres

# Verify databases were created
docker exec -it petbuddy-postgres psql -U postgres -c "\l"
```

### MinIO/S3 issues

```bash
# Verify MinIO is running
docker-compose logs minio

# Check buckets were created
docker exec -it petbuddy-minio mc ls local/
```




STEPS FAILED

1. $env:DOCKER_BUILDKIT=1
or 
1. $env:COMPOSE_PARALLEL_LIMIT=1