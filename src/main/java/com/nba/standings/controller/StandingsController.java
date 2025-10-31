package com.nba.standings.controller;

import com.nba.standings.dto.StandingsResponseDTO;
import com.nba.standings.dto.TeamStandingDTO;
import com.nba.standings.model.enums.GroupBy;
import com.nba.standings.service.StandingsCalculator.TeamStanding;
import com.nba.standings.service.StandingsService;
import com.nba.standings.util.SeasonDateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for NBA standings endpoints.
 * Provides API for retrieving historical standings data.
 */
@RestController
@RequestMapping("/api/standings")
public class StandingsController {
    
    private static final Logger logger = LoggerFactory.getLogger(StandingsController.class);
    
    private final StandingsService standingsService;
    private final SeasonDateUtility seasonDateUtility;
    
    public StandingsController(StandingsService standingsService, SeasonDateUtility seasonDateUtility) {
        this.standingsService = standingsService;
        this.seasonDateUtility = seasonDateUtility;
    }
    
    /**
     * Get NBA standings for a specific date grouped by division or conference.
     * 
     * @param date the date to retrieve standings for (format: yyyy-MM-dd)
     * @param groupBy how to group the standings (DIVISION or CONFERENCE)
     * @return ResponseEntity containing the standings response
     */
    @GetMapping
    public ResponseEntity<StandingsResponseDTO> getStandings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam GroupBy groupBy) {
        
        logger.info("========== CONTROLLER: getStandings called ==========");
        logger.info("CONTROLLER: Received date parameter: {}", date);
        logger.info("CONTROLLER: Date class: {}", date.getClass().getName());
        logger.info("CONTROLLER: Date toString: {}", date.toString());
        logger.info("CONTROLLER: Date year: {}, month: {}, day: {}", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        logger.info("CONTROLLER: Received groupBy parameter: {}", groupBy);
        
        // Validate date is within current season
        logger.info("CONTROLLER: Validating date is within current season...");
        seasonDateUtility.validateDateWithinCurrentSeason(date);
        logger.info("CONTROLLER: Date validation passed");
        
        // Get standings from service
        Map<String, List<TeamStanding>> standings = standingsService.getStandings(date, groupBy);
        
        // Transform TeamStanding objects to TeamStandingDTO objects
        Map<String, List<TeamStandingDTO>> standingDTOs = transformToDTO(standings, groupBy);
        
        // Build response
        StandingsResponseDTO response = new StandingsResponseDTO(date, groupBy, standingDTOs);
        
        logger.info("Successfully retrieved standings for date={}, groupBy={}", date, groupBy);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Transform TeamStanding objects to TeamStandingDTO objects.
     * Assigns the appropriate rank based on the groupBy parameter.
     * 
     * @param standings map of group name to list of team standings
     * @param groupBy how the standings are grouped (determines which rank to use)
     * @return map of group name to list of team standing DTOs
     */
    private Map<String, List<TeamStandingDTO>> transformToDTO(
            Map<String, List<TeamStanding>> standings, 
            GroupBy groupBy) {
        
        return standings.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(standing -> convertToDTO(standing, groupBy))
                                .collect(Collectors.toList())
                ));
    }
    
    /**
     * Convert a single TeamStanding to TeamStandingDTO.
     * Uses the appropriate rank based on the groupBy parameter.
     * 
     * @param standing the team standing to convert
     * @param groupBy how the standings are grouped (determines which rank to use)
     * @return the team standing DTO
     */
    private TeamStandingDTO convertToDTO(TeamStanding standing, GroupBy groupBy) {
        Integer rank = (groupBy == GroupBy.DIVISION) 
                ? standing.getDivisionRank() 
                : standing.getConferenceRank();
        
        return new TeamStandingDTO(
                rank,
                standing.getTeam().getTeamName(),
                standing.getWins(),
                standing.getLosses(),
                standing.getWinPct()
        );
    }
}
