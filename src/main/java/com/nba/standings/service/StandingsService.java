package com.nba.standings.service;

import com.nba.standings.model.entity.Game;
import com.nba.standings.model.entity.StandingsSnapshot;
import com.nba.standings.model.entity.Team;
import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import com.nba.standings.model.enums.GroupBy;
import com.nba.standings.repository.GameRepository;
import com.nba.standings.repository.StandingsSnapshotRepository;
import com.nba.standings.repository.TeamRepository;
import com.nba.standings.service.StandingsCalculator.TeamStanding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving NBA standings for a specific date.
 * Coordinates between data fetching, calculation, and caching layers.
 */
@Service
public class StandingsService {
    
    private static final Logger logger = LoggerFactory.getLogger(StandingsService.class);
    
    private final StandingsSnapshotRepository standingsSnapshotRepository;
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final NBADataService nbaDataService;
    private final StandingsCalculator standingsCalculator;
    
    public StandingsService(StandingsSnapshotRepository standingsSnapshotRepository,
                           GameRepository gameRepository,
                           TeamRepository teamRepository,
                           NBADataService nbaDataService,
                           StandingsCalculator standingsCalculator) {
        this.standingsSnapshotRepository = standingsSnapshotRepository;
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
        this.nbaDataService = nbaDataService;
        this.standingsCalculator = standingsCalculator;
    }
    
    /**
     * Get standings for a specific date grouped by division or conference.
     * 
     * @param date the date to retrieve standings for
     * @param groupBy how to group the standings (DIVISION or CONFERENCE)
     * @return map of group name to list of team standings
     */
    @Transactional
    public Map<String, List<TeamStanding>> getStandings(LocalDate date, GroupBy groupBy) {
        logger.info("Getting standings for date {} grouped by {}", date, groupBy);
        
        // Check if standings exist in cache
        boolean standingsExist = standingsSnapshotRepository.existsBySnapshotDate(date);
        
        if (!standingsExist) {
            logger.info("Standings not cached for {}. Calculating...", date);
            
            // Determine the fetch start date (most recent game date or season start)
            LocalDate fetchStartDate = determineFetchStartDate(date);
            logger.info("Fetching games from {} to {}", fetchStartDate, date);
            
            // Fetch only new games from the fetch start date to requested date
            nbaDataService.fetchAndSaveGames(fetchStartDate, date);
            
            // Get all games up to the requested date for standings calculation
            List<Game> games = gameRepository.findByGameDateLessThanEqual(date);
            logger.info("Found {} games up to {}", games.size(), date);
            
            // Get all teams
            List<Team> allTeams = teamRepository.findAll();
            
            // Calculate standings
            Map<Long, TeamStanding> standings = standingsCalculator.calculateStandings(games, allTeams);
            
            // Calculate division and conference rankings (modifies standings in place)
            standingsCalculator.assignDivisionRanks(standings);
            standingsCalculator.assignConferenceRanks(standings);
            
            // Save standings snapshots
            saveStandingsSnapshots(date, standings);
            
            logger.info("Standings calculated and cached for {}", date);
        } else {
            logger.info("Standings found in cache for {}", date);
        }
        // Retrieve standings from cache based on groupBy parameter
        return retrieveStandingsFromCache(date, groupBy);
    }
    
    /**
     * Determine the fetch start date for retrieving games from the NBA API.
     * This optimizes API calls by only fetching games we don't already have.
     * 
     * Strategy:
     * - If we have games in the database, start from the most recent game date
     *   (including that date, in case not all games were complete on the previous fetch)
     * - If we have no games, start from the season start date
     * 
     * @param requestedDate the date the user is requesting standings for
     * @return the date to start fetching games from
     */
    private LocalDate determineFetchStartDate(LocalDate requestedDate) {
        // Check if we have any games in the database
        LocalDate mostRecentGameDate = gameRepository.findMostRecentGameDate();
        
        if (mostRecentGameDate != null) {
            // We have games - start from the most recent game date
            // Include that date in case not all games were complete on the previous fetch
            logger.info("Most recent game date in database: {}", mostRecentGameDate);
            return mostRecentGameDate;
        } else {
            // No games in database - start from season start
            LocalDate seasonStart = determineSeasonStart(requestedDate);
            logger.info("No games in database. Starting from season start: {}", seasonStart);
            return seasonStart;
        }
    }
    
    /**
     * Determine the season start date based on the requested date.
     * NBA season starts on October 1st.
     * If the requested date is before October, it belongs to the previous season.
     * 
     * @param date the requested date
     * @return the season start date (October 1st of the appropriate year)
     */
    private LocalDate determineSeasonStart(LocalDate date) {
        int year = date.getYear();
        
        // If the date is before October (months 1-9), the season started in the previous year
        if (date.getMonth().getValue() < Month.OCTOBER.getValue()) {
            year = year - 1;
        }
        
        return LocalDate.of(year, Month.OCTOBER, 1);
    }
    
    /**
     * Save standings snapshots to the database for caching.
     * 
     * @param snapshotDate the date of the snapshot
     * @param standings map of team ID to team standing
     */
    private void saveStandingsSnapshots(LocalDate snapshotDate, Map<Long, TeamStanding> standings) {
        List<StandingsSnapshot> snapshots = new ArrayList<>();
        
        for (TeamStanding standing : standings.values()) {
            snapshots.add(StandingsSnapshot.fromTeamStanding(snapshotDate, standing));
        }
        
        standingsSnapshotRepository.saveAll(snapshots);
        logger.info("Saved {} standings snapshots for {}", snapshots.size(), snapshotDate);
    }
    
    /**
     * Retrieve standings from cache and group them according to the groupBy parameter.
     * 
     * @param date the date to retrieve standings for
     * @param groupBy how to group the standings (DIVISION or CONFERENCE)
     * @return map of group name to list of team standings
     */
    private Map<String, List<TeamStanding>> retrieveStandingsFromCache(LocalDate date, GroupBy groupBy) {
        Map<String, List<TeamStanding>> result = new HashMap<>();
        
        if (groupBy == GroupBy.DIVISION) {
            // Retrieve standings grouped by division
            for (Division division : Division.values()) {
                List<StandingsSnapshot> snapshots = standingsSnapshotRepository
                        .findBySnapshotDateAndTeam_Division(date, division);
                
                List<TeamStanding> standings = convertSnapshotsToStandings(snapshots, groupBy);
                result.put(division.name(), standings);
            }
        } else {
            // Retrieve standings grouped by conference
            for (Conference conference : Conference.values()) {
                List<StandingsSnapshot> snapshots = standingsSnapshotRepository
                        .findBySnapshotDateAndTeam_Conference(date, conference);
                
                List<TeamStanding> standings = convertSnapshotsToStandings(snapshots, groupBy);
                result.put(conference.name(), standings);
            }
        }
        
        return result;
    }
    
    /**
     * Convert StandingsSnapshot entities to TeamStanding objects and sort by rank.
     * 
     * @param snapshots list of standings snapshots
     * @param groupBy how the standings are grouped (determines which rank to sort by)
     * @return list of team standings sorted by appropriate rank
     */
    private List<TeamStanding> convertSnapshotsToStandings(List<StandingsSnapshot> snapshots, GroupBy groupBy) {
        List<TeamStanding> standings = new ArrayList<>();
        
        for (StandingsSnapshot snapshot : snapshots) {
            standings.add(TeamStanding.fromSnapshot(snapshot));
        }
        
        // Sort by the appropriate rank based on grouping
        if (groupBy == GroupBy.DIVISION) {
            standings.sort(Comparator.comparing(TeamStanding::getDivisionRank));
        } else {
            standings.sort(Comparator.comparing(TeamStanding::getConferenceRank));
        }
        
        return standings;
    }
}
