---
inclusion: always
---

# NBA Standings Viewer - Project Standards

## Project Overview

The NBA Standings Viewer displays historical NBA standings for a specific date. Users select a date and grouping (division/conference) to view standings as they existed on that date, avoiding spoilers for unwatched games.

**Tech Stack:**
- Backend: Java 21, Spring Boot 3.x, PostgreSQL, Gradle, Flyway, Docker
- Frontend: React 18+, TypeScript, Vite, Tailwind CSS, shadcn/ui
- Deployment: AWS (ECS/Elastic Beanstalk, RDS, S3, CloudFront)

## Architecture Principles

**Three-Tier Architecture:**
1. Presentation: React + TypeScript frontend
2. Business Logic: Spring Boot REST API  
3. Data: PostgreSQL database

**Key Design Decisions:**
- On-demand data fetching (no background jobs)
- Intelligent caching (games and standings cached in DB)
- Read-only team data (seeded via Flyway migration)
- Enum-based grouping (Conference and Division as Java enums)

## Code Organization

**Backend Package Structure:**
```
com.nba.standings/
├── controller/     # REST controllers
├── service/        # Business logic
├── repository/     # Spring Data JPA
├── model/
│   ├── entity/    # JPA entities
│   └── enums/     # Conference, Division
├── dto/           # Data Transfer Objects
├── exception/     # Custom exceptions
├── config/        # Configuration
└── client/        # NBA API client
```

**Frontend Structure:**
```
src/
├── components/    # React components
├── services/      # API service layer
├── types/         # TypeScript interfaces
├── hooks/         # Custom hooks
└── utils/         # Utilities
```

## Coding Standards

### Java/Spring Boot

**Naming:**
- Classes: PascalCase (StandingsService)
- Methods: camelCase (calculateStandings)
- Constants: UPPER_SNAKE_CASE (MAX_RETRY_ATTEMPTS)

**Entity Guidelines:**
- Use @Entity and @Table annotations
- Specify column names: @Column(name = "snake_case")
- Use @Enumerated(EnumType.STRING) for enums
- Use BigDecimal for decimals, LocalDate for dates

**Service Layer:**
- Keep services focused and single-purpose
- Use @Transactional for data modifications
- NO retry logic for NBA API calls (to respect rate limits)

**Repository Layer:**
- Extend JpaRepository<Entity, ID>
- Use Spring Data JPA query methods

**Exception Handling:**
- Custom exceptions extend RuntimeException
- Use @RestControllerAdvice for global handling
- Return appropriate HTTP status codes

### TypeScript/React

**Naming:**
- Components: PascalCase (StandingsTable)
- Functions/Variables: camelCase (fetchStandings)
- Types/Interfaces: PascalCase (TeamStanding)

**Component Guidelines:**
- Use functional components with hooks
- Keep components small and focused
- Use TypeScript interfaces for props

**Styling:**
- Use Tailwind CSS utility classes
- Leverage shadcn/ui components

## Database Standards

**Schema Conventions:**
- Tables: lowercase, plural (teams, games)
- Columns: lowercase, snake_case (game_date)
- Primary keys: id (BIGSERIAL)
- Foreign keys: {table}_id (team_id)

**Migration Guidelines:**
- Use Flyway for all schema changes
- Naming: V{version}__{description}.sql
- Never modify existing migrations

**Indexing:**
- Index all foreign keys
- Index columns in WHERE clauses
- Create composite indexes for common queries

## API Integration

**NBA API Guidelines:**
- Use WebClient for non-blocking calls
- **CRITICAL: NO retry logic** - Free tier limits to 5 requests/minute
- Fail fast on API errors to avoid rate limit violations
- Rely on aggressive database caching to minimize API calls
- Handle rate limiting gracefully with user-friendly error messages
- Map external team IDs to internal entities

## Testing

**IMPORTANT - Running Tests:**
- All tests must be run inside Docker containers
- Backend tests: `docker-compose exec backend ./gradlew test`
- Frontend tests: `docker-compose exec frontend npm test`
- Do NOT run test commands directly on the host machine

**Backend:**
- Unit tests for service layer
- MockMvc for controller tests
- Test containers for integration tests
- Aim for 85%+ coverage

**Frontend:**
- Test component rendering
- Test user interactions
- Mock API calls

## Docker & Deployment

**IMPORTANT - Local Development:**
- The application runs entirely in Docker containers locally
- ALL commands (build, test, run) must be executed inside Docker containers
- Use `docker-compose up` to start the application
- Use `docker-compose exec <service>` to run commands inside containers
- Do NOT run Gradle, Java, or npm commands directly on the host machine
- Example: `docker-compose exec backend ./gradlew build`
- Example: `docker-compose exec backend ./gradlew test`

**Docker Best Practices:**
- Multi-stage builds for smaller images
- Use specific version tags
- Set health checks
- Use environment variables
- Volume mounts for development hot reload

**AWS Deployment:**
- RDS for PostgreSQL
- ECS/Elastic Beanstalk for backend
- S3 + CloudFront for frontend
- Store secrets in AWS Secrets Manager

## Performance

**Backend:**
- Use database indexes effectively
- Batch insert operations
- Cache standings in database

**Frontend:**
- Use React.memo for expensive components
- Show loading indicators
- Optimize bundle size

## Security

- Validate all user inputs
- Use parameterized queries
- Don't expose sensitive info in errors
- Use HTTPS in production
- Store API keys securely

## Common Patterns

**Calculating Standings:**
1. Fetch games up to requested date
2. Calculate wins/losses per team
3. Compute win percentage: wins / (wins + losses)
4. Sort by win percentage
5. Assign ranks

**Caching Strategy:**
1. Check if standings exist for date
2. If exists, return from cache
3. If not: fetch games, calculate standings, cache, return

**Error Response Format:**
```json
{
  "error": "Error type",
  "message": "User-friendly message"
}
```

## Reference Files

- Requirements: `.kiro/specs/nba-standings-viewer/requirements.md`
- Design: `.kiro/specs/nba-standings-viewer/design.md`
- Tasks: `.kiro/specs/nba-standings-viewer/tasks.md`
