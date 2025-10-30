# Implementation Plan

- [x] 1. Set up project structure and configuration
  - [x] 1.1 Initialize Spring Boot project with Gradle and Java 21
    - Create Spring Boot project structure with Gradle build configuration
    - Configure build.gradle with dependencies: Spring Boot 3.x, Spring Data JPA, PostgreSQL driver, Flyway, WebClient
    - Set up application.properties with database and NBA API configuration placeholders
    - _Requirements: 5.3, 5.4_
  
  - [x] 1.2 Create Docker configuration for development environment
    - Write Dockerfile for Spring Boot backend with multi-stage build (Gradle build + Java 21 runtime)
    - Create docker-compose.yml with PostgreSQL service and backend service
    - Configure volume mounts and network settings
    - Add health check configuration
    - _Requirements: 5.1_
  
  - [x] 1.3 Set up Flyway migration structure
    - Create db/migration directory structure
    - Configure Flyway in application.properties
    - _Requirements: 2.3_

- [x] 2. Create domain models and enums
  - [x] 2.1 Implement Conference and Division enums
    - Create Conference enum with EASTERN and WESTERN values
    - Create Division enum with all six NBA divisions
    - _Requirements: 4.1, 4.2_
  
  - [x] 2.2 Implement Team entity
    - Create Team entity with id, nbaTeamId, teamName, abbreviation, division, conference
    - Use @Enumerated annotation for division and conference fields
    - Add appropriate JPA annotations and constraints
    - _Requirements: 2.2, 4.1, 4.2_
  
  - [x] 2.3 Implement Game entity
    - Create Game entity with id, nbaGameId, gameDate, homeTeam, awayTeam, homeScore, awayScore
    - Set up ManyToOne relationships with Team entity
    - Add appropriate indexes for date and team queries
    - _Requirements: 2.2, 2.5_
  
  - [x] 2.4 Implement StandingsSnapshot entity
    - Create StandingsSnapshot entity with id, snapshotDate, team, wins, losses, winPct, gamesBack, divisionRank, conferenceRank
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

- [ ] 4. Implement repository layer
  - [x] 4.1 Create TeamRepository interface
    - Extend JpaRepository for Team entity
    - Add query methods: findByDivision, findByConference, findByNbaTeamId
    - _Requirements: 2.2_
  
  - [x] 4.2 Create GameRepository interface
    - Extend JpaRepository for Game entity
    - Add query methods: findByGameDateBetween, findByGameDateLessThanEqual, existsByNbaGameId
    - _Requirements: 2.2, 2.5_
  
  - [ ] 4.3 Create StandingsSnapshotRepository interface
    - Extend JpaRepository for StandingsSnapshot entity
    - Add query methods: findBySnapshotDate, existsBySnapshotDate, findBySnapshotDateAndTeam_Division, findBySnapshotDateAndTeam_Conference
    - _Requirements: 2.3, 2.4_

- [ ] 5. Implement NBA API integration
  - [ ] 5.1 Create NBA API client configuration and DTOs
    - Create DTO classes for NBA API responses (game data, team data)
    - Configure WebClient bean for NBA API calls
    - Set up base URL and headers from application.properties
    - Implement retry logic with exponential backoff (3 attempts)
    - _Requirements: 2.1, 2.4_
  
  - [ ] 5.2 Create NBADataService
    - Implement method to fetch games for a date range from NBA API
    - Implement method to map external team IDs to internal Team entities
    - Transform API responses to Game entities
    - Handle API errors and implement retry logic
    - _Requirements: 2.1, 2.4_

- [ ] 6. Implement standings calculation logic
  - [ ] 6.1 Create StandingsCalculator service
    - Implement method to calculate win-loss records from games
    - Implement method to compute winning percentages
    - Implement method to calculate games behind leader
    - Implement method to sort teams by standings rules (win percentage descending)
    - Implement method to assign ranks within groups
    - _Requirements: 1.3, 3.3, 3.4, 3.5_
  
  - [ ] 6.2 Implement grouping logic
    - Implement method to group teams by division
    - Implement method to group teams by conference
    - _Requirements: 3.1, 3.2_

- [ ] 7. Implement core business logic
  - [ ] 7.1 Create custom exception classes
    - Create InvalidDateException for date validation failures
    - Create InvalidGroupByException for groupBy parameter validation failures
    - Create NBAApiException for NBA API communication failures
    - _Requirements: 1.5, 2.4_
  
  - [ ] 7.2 Create StandingsService
    - Implement method to check if standings exist in cache for a date
    - Implement method to fetch and store games from NBA API
    - Implement method to calculate and cache standings for date range
    - Implement method to retrieve cached standings grouped by division or conference
    - Coordinate between NBADataService, StandingsCalculator, and repositories
    - Handle transaction management
    - _Requirements: 1.2, 1.3, 2.1, 2.2, 2.3, 2.4_

- [ ] 8. Implement REST API layer
  - [ ] 8.1 Create DTOs for API responses
    - Create TeamStandingDTO with rank, teamName, wins, losses, winPct, gamesBack
    - Create StandingsResponseDTO with date, groupBy, and standings map
    - Create ErrorResponseDTO with error and message fields
    - _Requirements: 1.4_
  
  - [ ] 8.2 Create StandingsController
    - Implement GET /api/standings endpoint with date and groupBy parameters
    - Validate date parameter (must be within current season)
    - Validate groupBy parameter (must be "division" or "conference")
    - Call StandingsService to get standings
    - Transform entities to DTOs
    - Return appropriate HTTP responses
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [ ] 8.3 Create GlobalExceptionHandler
    - Implement exception handlers for InvalidDateException, InvalidGroupByException, NBAApiException
    - Return appropriate error responses with status codes
    - Log errors appropriately
    - _Requirements: 1.5, 2.4_

- [ ] 9. Initialize React frontend with TypeScript
  - [ ] 9.1 Create React project with Vite and TypeScript
    - Initialize Vite project with React and TypeScript template in frontend directory
    - Configure tsconfig.json for strict type checking
    - Set up project structure (components, services, types directories)
    - Update docker-compose to include frontend service
    - _Requirements: 1.1, 1.4_
  
  - [ ] 9.2 Set up Tailwind CSS and shadcn/ui
    - Install and configure Tailwind CSS
    - Initialize shadcn/ui with Radix UI components
    - Configure theme and styling
    - _Requirements: 1.4_
  
  - [ ] 9.3 Install and configure Axios
    - Install Axios for HTTP requests
    - Create API client configuration with base URL
    - Set up TypeScript types for API responses
    - _Requirements: 1.2_

- [ ] 10. Implement frontend components
  - [ ] 10.1 Create TypeScript types for API data
    - Define TeamStanding interface
    - Define StandingsResponse interface
    - Define ErrorResponse interface
    - _Requirements: 1.4_
  
  - [ ] 10.2 Create API service layer
    - Implement fetchStandings function that calls backend API
    - Handle API errors and return typed responses
    - _Requirements: 1.2_
  
  - [ ] 10.3 Create DateSelector component
    - Implement date input field using shadcn/ui components
    - Implement groupBy radio buttons (division/conference)
    - Implement submit button
    - Add date validation
    - Handle form submission
    - _Requirements: 1.1_
  
  - [ ] 10.4 Create StandingsTable component
    - Create reusable table component using shadcn/ui Table
    - Display columns: Rank, Team, W, L, PCT, GB
    - Accept data and title as props
    - Style with Tailwind CSS
    - _Requirements: 3.3, 3.4_
  
  - [ ] 10.5 Create StandingsDisplay component
    - Render multiple StandingsTable components based on groupBy selection
    - Handle loading state with spinner
    - Handle error state with error message
    - Handle empty state (no data for date)
    - _Requirements: 1.4, 3.1, 3.2_
  
  - [ ] 10.6 Create App component
    - Manage application state (date, groupBy, standings data, loading, error)
    - Render DateSelector component
    - Render StandingsDisplay component conditionally
    - Coordinate data fetching on form submission
    - Initially show only DateSelector (no standings)
    - _Requirements: 1.1, 1.4_

- [ ] 11. Configure CORS and integration
  - [ ] 11.1 Configure CORS in Spring Boot
    - Add CORS configuration class to allow frontend origin
    - Configure allowed methods and headers
    - _Requirements: 4.5_
  
  - [ ] 11.2 Update frontend API base URL
    - Configure environment-specific API URLs
    - Set up .env files for development and production
    - _Requirements: 4.2_

- [ ] 12. Create deployment documentation
  - [ ] 12.1 Write AWS deployment guide
    - Document AWS RDS PostgreSQL setup
    - Document backend deployment to AWS ECS or Elastic Beanstalk with Docker
    - Document frontend deployment to S3 + CloudFront
    - Document environment variable configuration
    - Include security group and networking setup
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [ ] 12.2 Update local development setup guide
    - Document Docker Compose setup steps for both backend and frontend
    - Document how to run backend and frontend locally
    - Document environment variable configuration
    - _Requirements: 4.1_

- [ ] 13. Build and verify application
  - [ ] 13.1 Verify Docker setup
    - Build backend Docker image
    - Build frontend Docker image
    - Test Docker Compose setup locally
    - Verify database migrations run successfully
    - _Requirements: 4.1_
  
  - [ ] 13.2 End-to-end verification
    - Start application with Docker Compose
    - Test date selection and standings retrieval
    - Test both division and conference grouping
    - Test error cases (invalid date, invalid groupBy)
    - Verify caching behavior (second request for same date is faster)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.3, 2.4, 3.1, 3.2_

- [ ] 14. Optional enhancements
  - [ ]* 14.1 Add comprehensive unit tests
    - Write unit tests for StandingsCalculator logic
    - Write unit tests for service layer methods
    - Write unit tests for repository query methods
    - _Requirements: All_
  
  - [ ]* 14.2 Add integration tests
    - Write integration tests for REST endpoints with MockMvc
    - Write integration tests for database operations
    - Write integration tests for NBA API client with WireMock
    - _Requirements: All_
  
  - [ ]* 14.3 Add frontend component tests
    - Write tests for DateSelector component
    - Write tests for StandingsDisplay component
    - Write tests for StandingsTable component
    - _Requirements: 1.1, 1.4_
