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

**Error Response Format (RFC 7807 Compliant):**
```json
{
  "type": "about:blank",
  "title": "Error Title",
  "status": 400,
  "detail": "User-friendly error message",
  "instance": "/api/standings"
}
```

**Error Handling Standards:**
- Use Spring's `ProblemDetail` class for all error responses
- Follow RFC 7807 (Problem Details for HTTP APIs) standard
- Include type, title, status, detail, and instance fields
- Leverage Spring Boot 3.x built-in RFC 7807 support

## Backend API Contract

**CRITICAL: The backend is fully implemented and functional. Frontend MUST match these exact contracts.**

### Standings Endpoint

**URL:** `GET /api/standings`

**Query Parameters:**
- `date` (required): ISO date format (yyyy-MM-dd), e.g., "2024-12-15"
- `groupBy` (required): Enum value - either "DIVISION" or "CONFERENCE"

**Example Request:**
```
GET /api/standings?date=2024-12-15&groupBy=DIVISION
```

**Success Response (200 OK):**
```json
{
  "date": "2024-12-15",
  "groupBy": "DIVISION",
  "standings": {
    "ATLANTIC": [
      {
        "rank": 1,
        "teamName": "Boston Celtics",
        "wins": 25,
        "losses": 5,
        "winPct": 0.833
      }
    ],
    "CENTRAL": [...],
    "SOUTHEAST": [...],
    "NORTHWEST": [...],
    "PACIFIC": [...],
    "SOUTHWEST": [...]
  }
}
```

**Error Response (RFC 7807 ProblemDetail):**
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Date must be within the current NBA season (on or after 2024-10-01)",
  "instance": "/api/standings?date=2024-09-15&groupBy=DIVISION"
}
```

### TypeScript Type Definitions (Frontend)

**IMPORTANT: Use these exact types to match the backend contract:**

```typescript
// Enums
type GroupBy = 'DIVISION' | 'CONFERENCE';
type Division = 'ATLANTIC' | 'CENTRAL' | 'SOUTHEAST' | 'NORTHWEST' | 'PACIFIC' | 'SOUTHWEST';
type Conference = 'EASTERN' | 'WESTERN';

// DTOs
interface TeamStandingDTO {
  rank: number;
  teamName: string;
  wins: number;
  losses: number;
  winPct: number; // BigDecimal from backend, comes as number in JSON
}

interface StandingsResponseDTO {
  date: string; // ISO date string (yyyy-MM-dd)
  groupBy: GroupBy;
  standings: Record<string, TeamStandingDTO[]>; // Key is Division or Conference enum name
}

// Error response (RFC 7807)
interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
}
```

### Season Date Validation Rules

**CRITICAL: Frontend must validate dates before sending to backend:**
- Date cannot be in the future (must be <= today)
- Date must be within current NBA season (>= October 1st of current season year)
- Season year logic: If current month is Jan-Sep, season started previous year's Oct 1st
- Example: In March 2025, valid dates are from 2024-10-01 to today

### CORS Configuration

Backend allows requests from `http://localhost:3000` with credentials enabled.

### Data Structure Notes

1. **Standings Map Keys**: The `standings` object uses enum names as string keys:
   - For DIVISION grouping: "ATLANTIC", "CENTRAL", "SOUTHEAST", "NORTHWEST", "PACIFIC", "SOUTHWEST"
   - For CONFERENCE grouping: "EASTERN", "WESTERN"

2. **Win Percentage**: Backend calculates as `wins / (wins + losses)` with 3 decimal precision

3. **Ranking**: 
   - When groupBy=DIVISION, rank is within division
   - When groupBy=CONFERENCE, rank is within conference
   - Teams are sorted by win percentage (descending)

## Frontend Development Guidelines

**API Service Layer:**
- Create a dedicated API service file (e.g., `src/services/api.ts`)
- Use `fetch` or `axios` for HTTP requests
- Base URL should come from environment variable: `import.meta.env.VITE_API_BASE_URL`
- Handle both success and error responses
- Parse RFC 7807 error responses for user-friendly messages

**Error Handling:**
- Display user-friendly error messages from `ProblemDetail.detail`
- Handle network errors gracefully
- Show loading states during API calls

**Date Handling:**
- Use native JavaScript Date or a library like date-fns
- Format dates as ISO strings (yyyy-MM-dd) for API calls
- Validate dates on frontend before API calls to provide immediate feedback
- Display dates in user-friendly format in UI

**State Management:**
- Use React hooks (useState, useEffect) for component state
- Consider custom hooks for API calls (e.g., `useStandings`)
- Handle loading, error, and success states

## Reference Files

- Requirements: `.kiro/specs/nba-standings-viewer/requirements.md`
- Design: `.kiro/specs/nba-standings-viewer/design.md`
- Tasks: `.kiro/specs/nba-standings-viewer/tasks.md`
