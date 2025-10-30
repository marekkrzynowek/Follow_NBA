# Docker Setup Guide

## Prerequisites

- Docker Desktop installed and running
- Docker Compose v2.0 or higher

## Quick Start

### Production Build

Start the application with production-ready Docker images:

```bash
docker-compose up --build
```

This will:
- Build the Spring Boot application using multi-stage Docker build
- Start PostgreSQL database
- Start the backend API on http://localhost:8080
- Run Flyway migrations automatically

### Development Mode

For development with hot reload support:

```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

This enables:
- Spring Boot DevTools for automatic restart
- Source code volume mounts for live updates
- Gradle cache for faster rebuilds

## Services

### PostgreSQL Database
- **Port**: 5432
- **Database**: nba_standings
- **Username**: nba_user
- **Password**: nba_password
- **Health Check**: Automatic with retry logic

### Backend API
- **Port**: 8080
- **Health Check**: http://localhost:8080/actuator/health
- **API Base**: http://localhost:8080/api

## Common Commands

### Start services
```bash
docker-compose up
```

### Start in detached mode
```bash
docker-compose up -d
```

### Stop services
```bash
docker-compose down
```

### Stop and remove volumes (clean slate)
```bash
docker-compose down -v
```

### View logs
```bash
docker-compose logs -f backend
docker-compose logs -f postgres
```

### Execute commands inside containers
```bash
# Run Gradle commands
docker-compose exec backend ./gradlew test

# Access PostgreSQL
docker-compose exec postgres psql -U nba_user -d nba_standings

# Access backend shell
docker-compose exec backend bash
```

### Rebuild specific service
```bash
docker-compose up --build backend
```

## Environment Variables

Update the following in `docker-compose.yml` for your environment:

- `NBA_API_KEY`: Your NBA API key (required for production)
- `NBA_API_BASE_URL`: NBA API endpoint
- Database credentials (if different from defaults)

## Health Checks

Both services include health checks:

- **PostgreSQL**: Checks database readiness every 10s
- **Backend**: Checks Spring Boot actuator health endpoint every 30s

The backend service waits for PostgreSQL to be healthy before starting.

## Troubleshooting

### Backend fails to start
- Check if PostgreSQL is healthy: `docker-compose ps`
- View backend logs: `docker-compose logs backend`
- Verify database connection settings

### Port conflicts
If ports 5432 or 8080 are already in use, modify the port mappings in `docker-compose.yml`:
```yaml
ports:
  - "5433:5432"  # Use 5433 on host instead
```

### Database connection issues
Ensure the backend uses the service name `postgres` as the hostname, not `localhost`.

### Hot reload not working
Make sure you're using the dev configuration:
```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up
```

## Volume Management

### View volumes
```bash
docker volume ls
```

### Remove all volumes (WARNING: deletes all data)
```bash
docker-compose down -v
```

### Backup database
```bash
docker-compose exec postgres pg_dump -U nba_user nba_standings > backup.sql
```

### Restore database
```bash
docker-compose exec -T postgres psql -U nba_user nba_standings < backup.sql
```

## Network

All services run on the `nba-network` bridge network, allowing them to communicate using service names as hostnames.

## Production Deployment

For AWS deployment:
1. Build the production image: `docker build -t nba-standings-backend .`
2. Push to ECR or Docker Hub
3. Deploy to ECS or Elastic Beanstalk
4. Configure environment variables for production
5. Use AWS RDS for PostgreSQL instead of containerized database
