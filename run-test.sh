#!/bin/bash
# Build and run a specific test with full logging

docker run --rm \
  --network nba_nba-network \
  -v "$(pwd)":/app \
  -w /app \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/nba_standings \
  -e SPRING_DATASOURCE_USERNAME=nba_user \
  -e SPRING_DATASOURCE_PASSWORD=nba_password \
  gradle:8.5-jdk21 \
  gradle test --tests "StandingsControllerIntegrationTest.testGetStandings_ByDivision_Success" --info 2>&1 | grep -E "(TEST:|CONTROLLER:|SEASON_UTIL:|MockHttpServletRequest|Status|Handler)"
