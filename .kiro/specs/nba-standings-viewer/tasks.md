# Implementation Plan

- [x] 1. Set up project structure and configuration
  - [x] 1.1 Initialize Spring Boot project with Gradle and Java 21
    - Create Spring Boot project structure with Gradle build configuration
    - Configure build.gradle with dependencies: Spring Boot 3.x, Spring Data JPA, PostgreSQL driver, Flyway, WebClient
    - Set up application.properties with database and NBA API configuration placeholders
    - _Requirements: 4.4, 4.5_
  
  - [x] 1.2 Create Docker configuration for development environment
    - Write Dockerfile for Spring Boot backend with multi-stage build (Gradle build + Java 21 runtime)
    - Create docker-compose.yml with PostgreSQL service and backend service
    - Configure volume mounts and network settings
    - Add health check configuration
    - _Requirements: 4.1_
  
  - [x] 1.3 Set up Flyway migration structure
    - Create db/migration directory structure
    - Configure Flyway in application.properties
    - _Requirements: 2.3_

- [x] 2. Create domain models and enums
  - [x] 2.1 Implement Conference and Division enums
    - Create Conference enum with EASTERN and WESTERN values
    - Create Division enum with all six NBA divisions
    - _Requirements: 3.1, 3.2_
  
  - [x] 2.2 Implement Team entity
    - Create Team entity with id, nbaTeamId, teamName, abbreviation, division, conference
    - Use @Enumerated annotation for division and conference fields
    - Add appropriate JPA annotations and constraints
    - _Requirements: 2.2, 3.1, 3.2_
  
  - [x] 2.3 Implement Game entity
    - Create Game entity with id, nbaGameId, gameDate, homeTeam, awayTeam, homeScore, awayScore
    - Set up ManyToOne relationships with Team entity
    - Add appropriate indexes for date and team queries
    - _Requirements: 2.2, 2.5_
  
  - [x] 2.4 Implement StandingsSnapshot entity
    - Create StandingsSnapshot entity with id, snapshotDate, team, wins, losses, winPct, divisionRank, conferenceRank
    - Set up ManyToOne relationship with Team entity
    - Add unique constraint on (snapshotDate, team)
    - Add appropriate indexes
    - _Requirements: 2.3_

- [x] 3. Create database migrations
  - [x] 3.1 Create V1__create_tables.sql migration
    - Write SQL to create teams, games, and standings_snapshots tables
    - Include all indexes and constraints from design
    - _Requirements: 2.2, 2.3, 2.5_
  
  - [x] 3.2 Create V2__seed_nba_teams.sql migration
    - Write SQL to insert all 30 NBA teams with their divisions, conferences, and NBA API team IDs
    - Include team names, abbreviations, and proper enum values
    - _Requirements: 2.2_

- [x] 4. Implement repository layer
  - [x] 4.1 Create TeamRepository interface
    - Extend JpaRepository for Team entity
    - Add query methods: findByDivision, findByConference, findByNbaTeamId
    - _Requirements: 2.2_
  
  - [x] 4.2 Create GameRepository interface
    - Extend JpaRepository for Game entity
    - Add query methods: findByGameDateBetween, findByGameDateLessThanEqual, existsByNbaGameId, findMostRecentGameDate
    - _Requirements: 2.2, 2.5_
  
  - [x] 4.3 Create StandingsSnapshotRepository interface
    - Extend JpaRepository for StandingsSnapshot entity
    - Add query methods: findBySnapshotDate, existsBySnapshotDate, findBySnapshotDateAndTeam_Division, findBySnapshotDateAndTeam_Conference
    - _Requirements: 2.3, 2.4_

- [x] 5. Implement NBA API integration
  - [x] 5.1 Create NBA API client configuration and DTOs
    - Create DTO classes for NBA API responses (game data, team data)
    - Configure WebClient bean for NBA API calls
    - Set up base URL and headers from application.properties
    - NO retry logic (to respect free tier rate limits)
    - _Requirements: 2.1, 2.4_
  
  - [x] 5.2 Create NBADataService
    - Implement method to fetch games for a date range from NBA API using NBAApiClient
    - Implement method to map external team IDs to internal Team entities
    - Transform API responses to Game entities
    - Handle API errors (fail fast, no retries)
    - Save games to database (skip duplicates using existsByNbaGameId)
    - _Requirements: 2.1, 2.4_

- [x] 6. Implement standings calculation logic
  - [x] 6.1 Create StandingsCalculator service
    - Implement method to calculate win-loss records from games for all teams
    - Implement method to compute winning percentages: wins / (wins + losses)
    - Implement method to sort teams by win percentage descending
    - Implement method to assign division ranks and conference ranks
    - Create internal TeamStanding class to hold calculated standings data
    - Add fromSnapshot factory method to convert StandingsSnapshot to TeamStanding
    - _Requirements: 1.3, 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 7. Implement core business logic
  - [x] 7.1 Create custom exception classes
    - Create InvalidDateException for date validation failures (extends RuntimeException)
    - Create NBAApiException for API communication failures
    - Place in com.nba.standings.exception package
    - _Requirements: 1.5, 2.4_
  
  - [x] 7.2 Create DTOs for API responses
    - Create TeamStandingDTO with rank, teamName, wins, losses, winPct fields
    - Create StandingsResponseDTO with date, groupBy, and standings (Map<String, List<TeamStandingDTO>>)
    - Add factory methods for type-safe division and conference grouping
    - Place in com.nba.standings.dto package
    - _Requirements: 1.4_
  
  - [x] 7.3 Create StandingsService
    - Implement getStandings(LocalDate date, GroupBy groupBy) method
    - Determine season start date (October 1st of the appropriate year based on requested date)
    - Optimize fetch start date (use most recent game date or season start)
    - Check if standings exist in cache using StandingsSnapshotRepository.existsBySnapshotDate
    - If not cached: fetch games via NBADataService, calculate standings via StandingsCalculator, save StandingsSnapshot entities
    - If cached: retrieve from StandingsSnapshotRepository by date and grouping
    - Return standings grouped by division or conference based on groupBy parameter
    - Use @Transactional for data modifications
    - _Requirements: 1.2, 1.3, 2.1, 2.2, 2.3, 2.4_

- [x] 8. Implement REST API layer
  - [x] 8.1 Create StandingsController
    - Implement GET /api/standings endpoint with @RequestParam date and groupBy (GroupBy enum type)
    - Use @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) for date parameter
    - Validate date parameter (must be within current season, throw InvalidDateException)
    - Call StandingsService.getStandings(date, groupBy)
    - Transform TeamStanding objects to TeamStandingDTO objects
    - Build StandingsResponseDTO with date, groupBy, and transformed standings
    - Return ResponseEntity<StandingsResponseDTO>
    - Place in com.nba.standings.controller package
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [x] 8.2 Create GlobalExceptionHandler
    - Use @RestControllerAdvice annotation
    - Implement @ExceptionHandler for InvalidDateException (return 400 Bad Request with ProblemDetail)
    - Implement @ExceptionHandler for MethodArgumentTypeMismatchException (return 400 Bad Request for invalid enum values)
    - Implement @ExceptionHandler for NBAApiException (return 500 Internal Server Error with ProblemDetail)
    - Implement @ExceptionHandler for generic Exception (return 500 Internal Server Error)
    - Use Spring's ProblemDetail for RFC 7807 compliant error responses
    - Include type, title, status, detail, and instance fields in error responses
    - Place in com.nba.standings.exception package
    - _Requirements: 1.5, 2.4_
  
  - [x] 8.3 Configure CORS for frontend integration
    - Create com.nba.standings.config.CorsConfig class
    - Use @Configuration and implement WebMvcConfigurer
    - Override addCorsMappings to allow frontend origin (http://localhost:3000 for dev)
    - Allow GET, POST, PUT, DELETE methods
    - Allow all headers
    - _Requirements: 4.5_

- [ ] 9. Initialize React frontend with TypeScript
  - [ ] 9.1 Create React project with Vite and TypeScript
    - Run `npm create vite@latest frontend -- --template react-ts` to initialize project
    - Configure tsconfig.json for strict type checking
    - Create directory structure: src/components, src/services, src/types, src/hooks
    - Add frontend service to docker-compose.yml with Node.js image
    - Configure volume mounts for hot reload
    - _Requirements: 1.1, 1.4_
  
  - [ ] 9.2 Set up Tailwind CSS and shadcn/ui
    - Install Tailwind CSS: `npm install -D tailwindcss postcss autoprefixer`
    - Initialize Tailwind: `npx tailwindcss init -p`
    - Configure tailwind.config.js with content paths
    - Add Tailwind directives to index.css
    - Install shadcn/ui: `npx shadcn-ui@latest init`
    - Install required components: Table, Button, Input, Card
    - _Requirements: 1.4_
  
  - [ ] 9.3 Install and configure Axios
    - Install Axios: `npm install axios`
    - Create src/services/api.ts with Axios instance configured with base URL
    - Create .env file with VITE_API_BASE_URL=http://localhost:8080
    - Set up TypeScript types for API responses in src/types
    - _Requirements: 1.2, 4.2_

- [ ] 10. Implement frontend components
  - [ ] 10.1 Create TypeScript types for API data
    - Create src/types/standings.ts
    - Define TeamStanding interface with rank, teamName, wins, losses, winPct
    - Define StandingsResponse interface with date, groupBy, standings (Record<string, TeamStanding[]>)
    - Define ErrorResponse interface with error and message
    - _Requirements: 1.4_
  
  - [ ] 10.2 Create API service layer
    - Create src/services/standingsService.ts
    - Implement fetchStandings(date: string, groupBy: string): Promise<StandingsResponse>
    - Use Axios instance from api.ts
    - Handle API errors and throw with error messages
    - _Requirements: 1.2_
  
  - [ ] 10.3 Create DateSelector component
    - Create src/components/DateSelector.tsx
    - Use shadcn/ui Input component for date input (type="date")
    - Use shadcn/ui RadioGroup for groupBy selection (division/conference)
    - Use shadcn/ui Button for submit
    - Accept onSubmit callback prop: (date: string, groupBy: string) => void
    - Add basic date validation (not empty)
    - _Requirements: 1.1_
  
  - [ ] 10.4 Create StandingsTable component
    - Create src/components/StandingsTable.tsx
    - Use shadcn/ui Table component
    - Display columns: Rank, Team, W, L, PCT
    - Accept props: title (string), standings (TeamStanding[])
    - Format winPct to 3 decimal places
    - Style with Tailwind CSS
    - _Requirements: 3.3, 3.4_
  
  - [ ] 10.5 Create StandingsDisplay component
    - Create src/components/StandingsDisplay.tsx
    - Accept props: standings (Record<string, TeamStanding[]>), loading (boolean), error (string | null)
    - Render loading spinner when loading is true
    - Render error message when error is not null
    - Render StandingsTable for each group in standings
    - Handle empty state (no standings data)
    - _Requirements: 1.4, 3.1, 3.2_
  
  - [ ] 10.6 Create App component
    - Update src/App.tsx
    - Use useState for: date, groupBy, standings, loading, error
    - Implement handleSubmit function that calls fetchStandings
    - Render DateSelector with onSubmit handler
    - Render StandingsDisplay with standings, loading, error props
    - Initially show only DateSelector (standings is null)
    - _Requirements: 1.1, 1.4_

- [ ] 11. Create deployment documentation
  - [ ] 11.1 Write AWS deployment guide
    - Create AWS_DEPLOYMENT.md in project root
    - Document AWS RDS PostgreSQL setup (instance type, security groups, connection string)
    - Document backend deployment to AWS ECS with Docker (task definition, service, load balancer)
    - Document frontend build and deployment to S3 + CloudFront (bucket config, distribution)
    - Document environment variables for backend (DB_URL, DB_USERNAME, DB_PASSWORD, NBA_API_KEY)
    - Document CORS configuration for production frontend URL
    - Include security group rules and VPC networking
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [x] 11.2 Update local development setup guide
    - Update README.md with local development instructions
    - Document prerequisites (Docker, Docker Compose)
    - Document how to start application: `docker-compose up`
    - Document how to run backend tests: `docker-compose exec backend ./gradlew test`
    - Document how to access application (backend: http://localhost:8080, frontend: http://localhost:3000)
    - Document environment variable configuration (.env.example)
    - _Requirements: 4.1_

- [ ]* 12. Optional enhancements
  - [x] 12.1 Add comprehensive unit tests
    - Write unit tests for StandingsCalculator logic
    - Write unit tests for service layer methods
    - Write unit tests for repository query methods
    - _Requirements: All_
  
  - [x] 12.2 Add integration tests
    - Write integration tests for REST endpoints with MockMvc
    - Write integration tests for database operations
    - Write integration tests for NBA API client with WireMock
    - _Requirements: All_
  
  - [ ]* 12.3 Add frontend component tests
    - Write tests for DateSelector component
    - Write tests for StandingsDisplay component
    - Write tests for StandingsTable component
    - _Requirements: 1.1, 1.4_
