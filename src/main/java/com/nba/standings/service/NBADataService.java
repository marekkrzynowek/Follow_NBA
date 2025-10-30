package com.nba.standings.service;

import com.nba.standings.client.NBAApiClient;
import com.nba.standings.dto.NBAGameDTO;
import com.nba.standings.dto.NBAGamesResponse;
import com.nba.standings.exception.NBAApiException;
import com.nba.standings.model.entity.Game;
import com.nba.standings.model.entity.Team;
import com.nba.standings.repository.GameRepository;
import com.nba.standings.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for fetching and storing NBA game data from the external NBA API.
 * Handles data transformation and duplicate prevention.
 */
@Service
public class NBADataService {
    
    private static final Logger logger = LoggerFactory.getLogger(NBADataService.class);
    
    private final NBAApiClient nbaApiClient;
    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    
    public NBADataService(NBAApiClient nbaApiClient, 
                         TeamRepository teamRepository,
                         GameRepository gameRepository) {
        this.nbaApiClient = nbaApiClient;
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
    }
    
    /**
     * Fetches games for a date range from the NBA API and saves them to the database.
     * Skips games that already exist in the database.
     * 
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @return list of newly saved games
     * @throws NBAApiException (unchecked) if the API call fails
     */
    @Transactional
    public List<Game> fetchAndSaveGames(LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching games from {} to {}", startDate, endDate);
        
        // Fetch games from NBA API (this is the only part that can throw API-related exceptions)
        NBAGamesResponse response;
        try {
            response = nbaApiClient.getAllGames(startDate, endDate)
                    .block(); // Block to get the result synchronously
        } catch (Exception e) {
            logger.error("Failed to fetch games from NBA API", e);
            throw new NBAApiException("Failed to fetch games from NBA API: " + e.getMessage(), e);
        }
        
        if (response == null || response.getData() == null) {
            throw new NBAApiException("NBA API returned null response");
        }
        
        logger.info("Received {} games from NBA API", response.getData().size());
        
        // Build team ID mapping cache
        Map<Long, Team> teamIdMap = buildTeamIdMap();
        
        // Transform and save games
        List<Game> savedGames = new ArrayList<>();
        for (NBAGameDTO gameDTO : response.getData()) {
            // Skip if game already exists
            if (gameRepository.existsByNbaGameId(gameDTO.getId())) {
                logger.debug("Game {} already exists, skipping", gameDTO.getId());
                continue;
            }
            
            // Skip games that are not finished (only save games with "Final" status)
            if (!gameDTO.isFinal()) {
                logger.debug("Game {} is not finished yet (status: {}), skipping", gameDTO.getId(), gameDTO.getStatus());
                continue;
            }
            
            // Transform to Game entity
            Game game = transformToGame(gameDTO, teamIdMap);
            if (game != null) {
                savedGames.add(game);
            }
        }
        
        // Batch save all new games
        if (!savedGames.isEmpty()) {
            savedGames = gameRepository.saveAll(savedGames);
            logger.info("Saved {} new games to database", savedGames.size());
        } else {
            logger.info("No new games to save");
        }
        
        return savedGames;
    }
    
    /**
     * Builds a map of NBA team IDs to internal Team entities for quick lookup.
     * 
     * @return map of NBA team ID to Team entity
     */
    private Map<Long, Team> buildTeamIdMap() {
        List<Team> allTeams = teamRepository.findAll();
        Map<Long, Team> teamIdMap = new HashMap<>();
        
        for (Team team : allTeams) {
            teamIdMap.put(team.getNbaTeamId().longValue(), team);
        }
        
        logger.debug("Built team ID map with {} teams", teamIdMap.size());
        return teamIdMap;
    }
    
    /**
     * Transforms an NBA API game DTO to a Game entity.
     * Maps external team IDs to internal Team entities.
     * 
     * @param gameDTO the NBA API game DTO
     * @param teamIdMap map of NBA team IDs to Team entities
     * @return Game entity, or null if teams cannot be mapped
     */
    private Game transformToGame(NBAGameDTO gameDTO, Map<Long, Team> teamIdMap) {
        // Map home team
        Team homeTeam = teamIdMap.get(gameDTO.getHomeTeam().getId());
        if (homeTeam == null) {
            logger.warn("Could not find home team with NBA ID {}", gameDTO.getHomeTeam().getId());
            return null;
        }
        
        // Map away team (visitor team)
        Team awayTeam = teamIdMap.get(gameDTO.getVisitorTeam().getId());
        if (awayTeam == null) {
            logger.warn("Could not find away team with NBA ID {}", gameDTO.getVisitorTeam().getId());
            return null;
        }
        
        // Create Game entity
        return new Game(
                gameDTO.getId(),
                gameDTO.getDate(),
                homeTeam,
                awayTeam,
                gameDTO.getHomeTeamScore(),
                gameDTO.getVisitorTeamScore()
        );
    }
}
