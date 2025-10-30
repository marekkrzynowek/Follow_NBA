# Design Document

## Overview

The NBA Standings Viewer is a full-stack web application built with Spring Boot (backend), PostgreSQL (database), and React (frontend). The system fetches NBA game data on-demand when users request standings for a specific date, calculates standings from game results, and caches both game data and pre-calculated standings for performance optimization.

The application follows a three-tier architecture with clear separation between presentation (React), business logic (Spring Boot), and data persistence (PostgreSQL).

## Architecture

### High-Level Architecture

```
┌─────────────────┐
│  React Frontend │
│   (Port 3000)   │
└────────┬────────┘
         │ HTTP/REST
         ▼
┌─────────────────┐
│  Spring Boot    │
│   Backend API   │
│   (Port 8080)   │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌──────────────┐
│  NBA   │ │  PostgreSQL  │
│  API   │ │   Database   │
└────────┘ └──────────────┘
```

### Technology Stack

**Backend:**
- Java 21
- Spring Boot 3.x
- Spring Data JPA (includes Hibernate)
- Spring Web (REST)
- PostgreSQL JDBC Driver
- WebClient for NBA API calls
- Gradle for dependency management
- Flyway for database migrations
- Docker for containerization

**Frontend:**
- React 18+
- TypeScript
- Vite for build tooling
- Axios for HTTP requests
- Tailwind CSS for styling
- shadcn/ui + Radix UI for component library

**Database:**
- PostgreSQL 14+

**Development:**
- Docker Compose for local development environment
- PostgreSQL container for local database

**Deployment:**
- Docker containers (Backend)
- AWS ECS or Elastic Beanstalk with Docker (Backend)
- AWS RDS PostgreSQL (Database)
- AWS S3 + CloudFront (Frontend static files)

## Components and Interfaces

### Backend Components

#### 1. Controller Layer

**StandingsController**
- Endpoint: `GET /api/standings?date={yyyy-MM-dd}&groupBy={division|conference}`
- Accepts date parameter (required)
- Accepts groupBy parameter (required): "division" or "conference"
- Returns standings grouped by the specified grouping
- Handles validation and error responses

```java
@RestController
@RequestMapping("/api/standings")
public class StandingsController {
    @GetMapping
    public ResponseEntity<StandingsResponse> getStandings(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam String groupBy
    );
}
```

#### 2. Service Layer

**StandingsService**
- Orchestrates the standings retrieval process
- Checks cache for existing standings
- Triggers data fetching if needed
- Calculates standings from game data
- Coordinates with NBADataService and repository layers

**NBADataService**
- Interfaces with external NBA API
- Fetches game results for date ranges
- Implements retry logic for failed requests
- Transforms external API responses to internal models
- Maps external team IDs to internal team entities

**StandingsCalculator**
- Calculates win-loss records from game data
- Computes winning percentages
- Calculates games behind leader
- Sorts teams by standings rules
- Groups teams by division and conference

#### 3. Repository Layer

**GameRepository** (Spring Data JPA)
- CRUD operations for Game entities
- Query games by date range
- Query games by team and date

**TeamRepository** (Spring Data JPA)
- Read-only operations for Team entities
- Query teams by division/conference
- Teams are seeded via Flyway migration (no CRUD needed)

**StandingsSnapshotRepository** (Spring Data JPA)
- CRUD operations for cached standings
- Query standings by date
- Check if standings exist for a date

### Frontend Components

#### 1. App Component
- Root component
- Manages application state
- Coordinates child components

#### 2. DateSelector Component
- Renders date input field
- Renders submit button
- Validates date input
- Triggers API call on submission

#### 3. StandingsDisplay Component
- Receives standings data from parent
- Renders division standings tables
- Renders conference standings tables
- Handles loading and error states

#### 4. StandingsTable Component
- Reusable table component
- Displays team standings with columns: Rank, Team, W, L, PCT, GB
- Accepts data and configuration as props

### API Contracts

#### GET /api/standings

**Request (Division Grouping):**
```
GET /api/standings?date=2025-10-24&groupBy=division
```

**Response (Success - 200 OK):**
```json
{
  "date": "2025-10-24",
  "groupBy": "division",
  "standings": {
    "Atlantic": [
      {"rank": 1, "teamName": "Boston Celtics", "wins": 3, "losses": 0, "winPct": 1.000, "gamesBack": 0.0}
    ],
    "Central": [...],
    "Southeast": [...],
    "Northwest": [...],
    "Pacific": [...],
    "Southwest": [...]
  }
}
```

**Request (Conference Grouping):**
```
GET /api/standings?date=2025-10-24&groupBy=conference
```

**Response (Success - 200 OK):**
```json
{
  "date": "2025-10-24",
  "groupBy": "conference",
  "standings": {
    "Eastern": [
      {"rank": 1, "teamName": "Boston Celtics", "wins": 3, "losses": 0, "winPct": 1.000, "gamesBack": 0.0}
    ],
    "Western": [...]
  }
}
```

**Response (Error - 400 Bad Request - Invalid Date):**
```json
{
  "error": "Invalid date",
  "message": "Date must be within the current NBA season"
}
```

**Response (Error - 400 Bad Request - Invalid GroupBy):**
```json
{
  "error": "Invalid groupBy parameter",
  "message": "groupBy must be either 'division' or 'conference'"
}
```

**Response (Error - 500 Internal Server Error):**
```json
{
  "error": "Data fetch failed",
  "message": "Unable to retrieve NBA data from external service"
}
```

## Data Models

### Database Schema

#### Teams Table
```sql
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    nba_team_id INTEGER UNIQUE NOT NULL,
    team_name VARCHAR(100) NOT NULL,
    abbreviation VARCHAR(3) NOT NULL,
    division VARCHAR(50) NOT NULL,
    conference VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Games Table
```sql
CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    nba_game_id VARCHAR(50) UNIQUE NOT NULL,
    game_date DATE NOT NULL,
    home_team_id BIGINT REFERENCES teams(id),
    away_team_id BIGINT REFERENCES teams(id),
    home_score INTEGER NOT NULL,
    away_score INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_game_date (game_date),
    INDEX idx_home_team (home_team_id),
    INDEX idx_away_team (away_team_id)
);
```

#### Standings Snapshots Table
```sql
CREATE TABLE standings_snapshots (
    id BIGSERIAL PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    team_id BIGINT REFERENCES teams(id),
    wins INTEGER NOT NULL,
    losses INTEGER NOT NULL,
    win_pct DECIMAL(5,3) NOT NULL,
    games_back DECIMAL(4,1),
    division_rank INTEGER,
    conference_rank INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(snapshot_date, team_id),
    INDEX idx_snapshot_date (snapshot_date),
    INDEX idx_team_date (team_id, snapshot_date)
);
```

### Java Entity Models

**Conference Enum**
```java
public enum Conference {
    EASTERN,
    WESTERN
}
```

**Division Enum**
```java
public enum Division {
    ATLANTIC,
    CENTRAL,
    SOUTHEAST,
    NORTHWEST,
    PACIFIC,
    SOUTHWEST
}
```

**Team Entity**
```java
@Entity
@Table(name = "teams")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nba_team_id", unique = true, nullable = false)
    private Integer nbaTeamId;
    
    @Column(name = "team_name", unique = true, nullable = false)
    private String teamName;
    
    @Column(nullable = false, unique = true, length = 3)
    private String abbreviation;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Division division;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Conference conference;
    
    // getters, setters, constructors
}
```

**Game Entity**
```java
@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nba_game_id", unique = true, nullable = false)
    private String nbaGameId;
    
    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;
    
    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;
    
    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;
    
    @Column(name = "home_score", nullable = false)
    private Integer homeScore;
    
    @Column(name = "away_score", nullable = false)
    private Integer awayScore;
    
    // getters, setters, constructors
}
```

**StandingsSnapshot Entity**
```java
@Entity
@Table(name = "standings_snapshots")
public class StandingsSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
    
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
    
    @Column(nullable = false)
    private Integer wins;
    
    @Column(nullable = false)
    private Integer losses;
    
    @Column(name = "win_pct", nullable = false, precision = 5, scale = 3)
    private BigDecimal winPct;
    
    @Column(name = "games_back", precision = 4, scale = 1)
    private BigDecimal gamesBack;
    
    @Column(name = "division_rank")
    private Integer divisionRank;
    
    @Column(name = "conference_rank")
    private Integer conferenceRank;
    
    // getters, setters, constructors
}
```

## Error Handling

### Backend Error Handling

**Exception Hierarchy:**
- `NBADataException` - Base exception for NBA data-related errors
  - `NBAApiException` - External API communication failures
  - `InvalidDateException` - Date validation failures
  - `DataNotFoundException` - Requested data not available

**Global Exception Handler:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidDateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDate(InvalidDateException ex);
    
    @ExceptionHandler(NBAApiException.class)
    public ResponseEntity<ErrorResponse> handleNBAApiError(NBAApiException ex);
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex);
}
```

**Rate Limiting Strategy:**
- NBA API free tier limits requests to 5 per minute
- NO retry logic implemented to minimize API requests
- Failed requests throw NBAApiException immediately
- Rely on database caching to reduce API calls

### Frontend Error Handling

- Display user-friendly error messages
- Show loading spinner during API calls
- Handle network errors gracefully
- Provide retry option for failed requests

## Testing Strategy

### Backend Testing

**Unit Tests:**
- Test StandingsCalculator logic with mock game data
- Test date validation in StandingsService
- Test entity mappings and relationships
- Test repository query methods

**Integration Tests:**
- Test REST endpoints with MockMvc
- Test database operations with test containers
- Test NBA API integration with WireMock
- Test end-to-end standings calculation flow

**Test Coverage Goals:**
- Service layer: 90%+
- Controller layer: 85%+
- Repository layer: 80%+

### Frontend Testing

**Component Tests:**
- Test DateSelector input validation
- Test StandingsDisplay rendering with mock data
- Test error state rendering
- Test loading state rendering

**Integration Tests:**
- Test API integration with mock server
- Test user interaction flows

### Manual Testing

- Test with various dates (season start, mid-season, current)
- Test with dates before season start (error case)
- Test with dates in the future (error case)
- Test caching behavior (first request vs. cached request)
- Test AWS deployment configuration

## NBA API Integration

### API Selection

We will use the **balldontlie.io** API (free tier available) or **NBA Stats API** (stats.nba.com).

**Key Endpoints Needed:**
1. Get all games for a date range
2. Get team information

### Data Fetching Strategy

**On User Request:**
1. Check if standings exist in cache for requested date
2. If cached, return immediately
3. If not cached:
   - Determine season start date
   - Fetch all games from season start to requested date
   - Store games in database (skip duplicates)
   - Calculate standings for each date in range
   - Store standings snapshots
   - Return standings for requested date

**Optimization:**
- Only fetch games newer than the latest game in database
- Batch insert games and standings for performance
- Use database transactions for consistency

## Docker Configuration

### Development Environment

**docker-compose.yml:**
- PostgreSQL service for local database
- Backend service (Spring Boot app)
- Volume mounts for hot reload
- Network configuration for service communication

**Dockerfile (Backend):**
- Multi-stage build
- Stage 1: Gradle build with Java 21
- Stage 2: Runtime with Java 21 JRE
- Expose port 8080
- Health check configuration

### Benefits of Docker Approach

- Consistent development environment across team
- Easy local setup (docker-compose up)
- Production-ready containers for AWS deployment
- Simplified CI/CD pipeline

## Database Migrations

### Flyway Configuration

**Migration Strategy:**
- Version-controlled SQL migration files
- Migrations run automatically on application startup
- Naming convention: V{version}__{description}.sql
- Example: V1__create_teams_table.sql, V2__seed_nba_teams.sql

**Migration Files Location:**
- src/main/resources/db/migration/

**Team Data Management:**
- All 30 NBA teams seeded via Flyway migration (V2__seed_nba_teams.sql)
- Includes team name, abbreviation, division, conference, and NBA API team ID
- Team changes (rare) handled via new migration files
- No runtime CRUD operations for teams - read-only access

**Flyway Benefits:**
- Track schema changes in version control
- Automatic migration on deployment
- Rollback support
- Database version tracking
- Simplified team data management

## Deployment Architecture

### AWS Infrastructure

**Backend (ECS with Docker or Elastic Beanstalk):**
- Docker container deployment
- Java 21 runtime
- Environment variables for database connection
- Auto-scaling configuration (min 1, max 3 instances)
- Health check endpoint: `/actuator/health`

**Database (RDS PostgreSQL):**
- PostgreSQL 14.x
- db.t3.micro instance (can scale up)
- Multi-AZ for production
- Automated backups enabled

**Frontend (S3 + CloudFront):**
- React build artifacts in S3 bucket
- CloudFront distribution for CDN
- Custom domain (optional)

**Configuration:**
- Environment-specific application.properties
- Secrets stored in AWS Secrets Manager
- CORS configuration for frontend-backend communication

### Environment Variables

```properties
# Database
DB_HOST=<rds-endpoint>
DB_PORT=5432
DB_NAME=nba_standings
DB_USERNAME=<username>
DB_PASSWORD=<password>

# NBA API
NBA_API_BASE_URL=https://api.balldontlie.io/v1
NBA_API_KEY=<api-key>

# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
```

## Performance Considerations

**Caching Strategy:**
- Cache standings snapshots in database
- First request for a date: slower (fetch + calculate)
- Subsequent requests: fast (database lookup)

**Database Indexing:**
- Index on game_date for fast date range queries
- Index on team_id for fast team lookups
- Composite index on (snapshot_date, team_id) for standings queries

**API Rate Limiting:**
- NBA API free tier: 5 requests per minute
- NO retry logic to avoid hitting rate limits
- Aggressive database caching to minimize API calls
- Cache team data (rarely changes)
- Fail fast on API errors rather than retry

**Frontend Optimization:**
- Lazy load standings tables
- Show loading indicators
- Minimize re-renders with React.memo
