# NBA Standings Viewer

A full-stack web application that displays historical NBA standings for any specific date, perfect for fans who watch games on delay and want to avoid spoilers.

## 🏀 Overview

The NBA Standings Viewer allows users to select a date and view NBA standings (by Division or Conference) as they existed on that specific date. The application fetches live NBA data, stores it in a database, and calculates historical standings on-demand.

### Key Features

- **Historical Standings**: View standings as they existed on any date during the NBA season
- **Multiple Views**: Toggle between Division and Conference groupings
- **Spoiler-Free**: Perfect for fans watching games on delay
- **Smart Caching**: Intelligent data caching for fast subsequent requests
- **On-Demand Data**: Fetches NBA data only when needed

## 🛠️ Tech Stack

### Backend
- **Java 21** with **Spring Boot 3.x**
- **PostgreSQL** database
- **Spring Data JPA** (Hibernate)
- **Flyway** for database migrations
- **Gradle** for dependency management
- **Docker** for containerization

### Frontend
- **React 18+** with **TypeScript**
- **Vite** for build tooling
- **Tailwind CSS** for styling
- **shadcn/ui** component library
- **Axios** for HTTP requests

### Infrastructure
- **Docker Compose** for local development
- **AWS ECS/Elastic Beanstalk** for backend deployment
- **AWS RDS** for PostgreSQL database
- **AWS S3 + CloudFront** for frontend hosting

## 🚀 Getting Started

### Prerequisites

- Docker and Docker Compose installed
- NBA API key from [balldontlie.io](https://www.balldontlie.io/) (free tier available)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd nba-standings-viewer
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env and add your NBA_API_KEY
   ```

3. **Start the application with Docker Compose**
   ```bash
   docker-compose up
   ```

   This will start:
   - PostgreSQL database on port 5432
   - Spring Boot backend on port 8080
   - React frontend on port 3000

4. **Access the application**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - API Health Check: http://localhost:8080/actuator/health

### Running Commands in Docker

**Important**: All build, test, and run commands must be executed inside Docker containers.

**Backend commands:**
```bash
# Build the backend
docker-compose exec backend ./gradlew build

# Run tests
docker-compose exec backend ./gradlew test

# Clean build
docker-compose exec backend ./gradlew clean build
```

**Frontend commands:**
```bash
# Install dependencies
docker-compose exec frontend npm install

# Run tests
docker-compose exec frontend npm test

# Build for production
docker-compose exec frontend npm run build
```

## 📁 Project Structure

```
nba-standings-viewer/
├── src/
│   ├── main/
│   │   ├── java/com/nba/standings/
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── service/         # Business logic
│   │   │   ├── repository/      # Spring Data JPA repositories
│   │   │   ├── model/
│   │   │   │   ├── entity/      # JPA entities
│   │   │   │   └── enums/       # Conference, Division enums
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── exception/       # Custom exceptions
│   │   │   ├── config/          # Configuration classes
│   │   │   └── client/          # NBA API client
│   │   └── resources/
│   │       ├── db/migration/    # Flyway SQL migrations
│   │       └── application.properties
│   └── test/                    # Test files
├── frontend/
│   ├── src/
│   │   ├── components/          # React components
│   │   ├── services/            # API service layer
│   │   ├── types/               # TypeScript interfaces
│   │   ├── hooks/               # Custom React hooks
│   │   └── utils/               # Utility functions
│   └── public/
├── docker-compose.yml
├── Dockerfile
└── README.md
```

## 🔌 API Documentation

### Get Standings

**Endpoint:** `GET /api/standings`

**Query Parameters:**
- `date` (required): Date in `YYYY-MM-DD` format
- `groupBy` (required): Either `division` or `conference`

**Example Request:**
```bash
curl "http://localhost:8080/api/standings?date=2025-10-24&groupBy=division"
```

**Example Response:**
```json
{
  "date": "2025-10-24",
  "groupBy": "division",
  "standings": {
    "Atlantic": [
      {
        "rank": 1,
        "teamName": "Boston Celtics",
        "wins": 3,
        "losses": 0,
        "winPct": 1.000,
        "gamesBack": 0.0
      }
    ],
    "Central": [...],
    "Southeast": [...],
    "Northwest": [...],
    "Pacific": [...],
    "Southwest": [...]
  }
}
```

## 🗄️ Database Schema

### Teams Table
Stores NBA team information (seeded via Flyway migration)
- Team name, abbreviation
- Division and Conference
- NBA API team ID mapping

### Games Table
Stores game results
- Game date, teams, scores
- Indexed by date and teams for fast queries

### Standings Snapshots Table
Caches calculated standings
- Wins, losses, win percentage
- Games behind leader
- Division and conference ranks

## 🧪 Testing

### Backend Tests
```bash
# Run all tests
docker-compose exec backend ./gradlew test

# Run specific test class
docker-compose exec backend ./gradlew test --tests StandingsServiceTest

# Run with coverage
docker-compose exec backend ./gradlew test jacocoTestReport
```

### Frontend Tests
```bash
# Run tests
docker-compose exec frontend npm test

# Run tests with coverage
docker-compose exec frontend npm test -- --coverage
```

## 🚢 Deployment

### AWS Deployment Architecture

1. **Backend**: Deployed to AWS ECS or Elastic Beanstalk using Docker containers
2. **Database**: AWS RDS PostgreSQL instance
3. **Frontend**: Static files hosted on S3, served via CloudFront CDN

### Environment Variables for Production

```properties
DB_HOST=<rds-endpoint>
DB_PORT=5432
DB_NAME=nba_standings
DB_USERNAME=<username>
DB_PASSWORD=<password>
NBA_API_BASE_URL=https://api.balldontlie.io/v1
NBA_API_KEY=<your-api-key>
SPRING_PROFILES_ACTIVE=prod
```

### Deployment Steps

1. Build Docker image for backend
2. Push image to AWS ECR
3. Deploy to ECS/Elastic Beanstalk
4. Configure RDS database connection
5. Build frontend static files
6. Upload to S3 bucket
7. Configure CloudFront distribution

## 📊 How It Works

### Data Flow

1. **User Request**: User selects a date and grouping (division/conference)
2. **Cache Check**: Backend checks if standings exist in database for that date
3. **Data Fetch** (if not cached):
   - Fetch all games from season start to requested date from NBA API
   - Store games in database
   - Calculate standings for each date in range
   - Cache standings snapshots
4. **Response**: Return standings to frontend
5. **Display**: Frontend renders standings in organized tables

### Standings Calculation

- **Win Percentage**: `wins / (wins + losses)`
- **Games Behind**: `((leader_wins - team_wins) + (team_losses - leader_losses)) / 2`
- **Ranking**: Teams sorted by win percentage (descending)

### Caching Strategy

- First request for a date: Slower (fetches and calculates)
- Subsequent requests: Fast (database lookup)
- Only fetches new games since last update

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards

- Follow Java naming conventions (PascalCase for classes, camelCase for methods)
- Use TypeScript for all frontend code
- Write tests for new features
- Follow existing code structure and patterns
- Run tests before submitting PR

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- NBA data provided by [balldontlie.io](https://www.balldontlie.io/)
- Built with Spring Boot, React, and PostgreSQL
- UI components from [shadcn/ui](https://ui.shadcn.com/)

## 📧 Contact

For questions or support, please open an issue on GitHub.

---

**Note**: This application is for educational purposes. NBA and team names are trademarks of their respective owners.
