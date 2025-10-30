# Requirements Document

## Introduction

The NBA Standings Viewer is a web application that allows users to view NBA league standings (by Division and Conference) as they existed on a specific date. The system is designed for users who watch NBA games on delay and want to avoid spoilers by only viewing standings up to the date of games they have watched. The application will fetch live NBA data, store it in a database, calculate historical standings, and highlight specific teams of interest.

## Glossary

- **NBA Standings Viewer**: The complete web application system including frontend, backend, and database
- **Backend API**: The Spring Boot REST API that handles data fetching, storage, and standings calculation
- **Frontend Application**: The React-based user interface
- **NBA Data Service**: External NBA API service that provides game results and team information
- **Database**: PostgreSQL database that stores NBA games, teams, and standings data
- **Historical Standings**: Team standings calculated based on games played up to and including a specific date

- **Division Standings**: Team rankings within their respective NBA divisions
- **Conference Standings**: Team rankings within their respective NBA conferences (Eastern/Western)

## Requirements

### Requirement 1

**User Story:** As an NBA fan who watches games on delay, I want to select a specific date and view standings as they existed on that date, so that I can avoid seeing results from games I haven't watched yet.

#### Acceptance Criteria

1. WHEN the Frontend Application loads, THE NBA Standings Viewer SHALL display a date input field and a submit button without showing any standings data
2. WHEN the user enters a date and clicks the submit button, THE Backend API SHALL retrieve all games played up to and including that date in the current NBA season
3. WHEN the Backend API receives a date request, THE Backend API SHALL calculate Division Standings and Conference Standings based on games up to and including the specified date
4. WHEN the Backend API completes the standings calculation, THE Frontend Application SHALL display the calculated standings organized by Division and Conference
5. IF the user enters a date before the current NBA season started, THEN THE Backend API SHALL return an error message indicating no data is available for that date

### Requirement 2

**User Story:** As a user, I want the application to fetch and store NBA game data on-demand when I request a specific date, so that I can view accurate standings information without unnecessary background processing.

#### Acceptance Criteria

1. WHEN the user submits a date request, THE Backend API SHALL fetch game results from the NBA Data Service for all games up to and including that date
2. WHEN the Backend API fetches game data, THE Database SHALL store game results including date, teams, scores, and outcomes
3. WHEN the Backend API fetches game data, THE Database SHALL store pre-calculated standings for each date from the season start up to the requested date
4. WHEN the user requests a date for which standings are already cached in the Database, THE Backend API SHALL return the cached standings without fetching from the NBA Data Service
5. THE Backend API SHALL fetch team information from the NBA Data Service including team names, divisions, and conferences

### Requirement 3

**User Story:** As a user, I want to see standings organized by both Division and Conference, so that I can understand team performance in different contexts.

#### Acceptance Criteria

1. THE Frontend Application SHALL display standings for all six NBA divisions (Atlantic, Central, Southeast, Northwest, Pacific, Southwest)
2. THE Frontend Application SHALL display standings for both NBA conferences (Eastern Conference, Western Conference)
3. WHEN displaying Division Standings, THE Frontend Application SHALL show team name, wins, losses, and winning percentage for each team
4. WHEN displaying Conference Standings, THE Frontend Application SHALL show team name, wins, losses, winning percentage, and games behind the leader for each team
5. THE Frontend Application SHALL sort teams within each division and conference by winning percentage in descending order

### Requirement 4

**User Story:** As a developer, I want the application to be deployable on AWS, so that it can be accessed reliably over the internet.

#### Acceptance Criteria

1. THE Backend API SHALL be packaged as a deployable JAR file compatible with AWS Elastic Beanstalk or AWS ECS
2. THE Frontend Application SHALL be built as static files deployable to AWS S3 or served by the Backend API
3. THE Database SHALL be configurable to connect to AWS RDS PostgreSQL instances
4. THE Backend API SHALL expose configuration options for database connection strings, API keys, and environment-specific settings through environment variables
5. THE NBA Standings Viewer SHALL include deployment documentation for AWS infrastructure setup
