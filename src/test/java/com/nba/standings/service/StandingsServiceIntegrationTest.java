package com.nba.standings.service;

import com.nba.standings.client.NBAApiClient;
import com.nba.standings.dto.NBAGameDTO;
import com.nba.standings.dto.NBAGamesResponse;
import com.nba.standings.dto.NBATeamDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for StandingsService.
 * Tests the full service workflow with real database operations and mocked NBA API.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class StandingsServiceIntegrationTest {

    @Autowired
    private StandingsService standingsService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private StandingsSnapshotRepository standingsSnapshotRepository;

    @MockBean
    private NBAApiClient nbaApiClient;

    private Team celtics;
    private Team heat;
    private Team lakers;
    private Team nuggets;

    @BeforeEach
    void setUp() {
        // Clean up database
        standingsSnapshotRepository.deleteAll();
        gameRepository.deleteAll();
        teamRepository.deleteAll();

        // Create test teams
        celtics = teamRepository.save(new Team(1, "Boston Celtics", "BOS", Division.ATLANTIC, Conference.EASTERN));
        heat = teamRepository.save(new Team(2, "Miami Heat", "MIA", Division.SOUTHEAST, Conference.EASTERN));
        lakers = teamRepository.save(new Team(3, "Los Angeles Lakers", "LAL", Division.PACIFIC, Conference.WESTERN));
        nuggets = teamRepository.save(new Team(4, "Denver Nuggets", "DEN", Division.NORTHWEST, Conference.WESTERN));
    }

    @Test
    void testGetStandings_FirstRequest_FetchesAndCalculates() {
        LocalDate requestDate = LocalDate.of(2024, 10, 22);

        // Mock NBA API response
        NBAGamesResponse mockResponse = createMockApiResponse();
        when(nbaApiClient.getAllGames(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Mono.just(mockResponse));

        // Get standings
        Map<String, List<TeamStanding>> standings = standingsService.getStandings(requestDate, GroupBy.DIVISION);

        // Verify API was called
        verify(nbaApiClient, times(1)).getAllGames(any(LocalDate.class), any(LocalDate.class));

        // Verify standings were calculated
        assertNotNull(standings);
        assertTrue(standings.containsKey("ATLANTIC"));
        assertTrue(standings.containsKey("PACIFIC"));

        // Verify games were saved
        List<Game> savedGames = gameRepository.findAll();
        assertEquals(2, savedGames.size());

        // Verify standings snapshots were saved
        assertTrue(standingsSnapshotRepository.existsBySnapshotDate(requestDate));
        List<StandingsSnapshot> snapshots = standingsSnapshotRepository.findBySnapshotDate(requestDate);
        assertEquals(4, snapshots.size());
    }

    @Test
    void testGetStandings_CachedRequest_DoesNotFetchAgain() {
        LocalDate requestDate = LocalDate.of(2024, 10, 22);

        // Create existing games and snapshots
        gameRepository.save(new Game(100L, requestDate, celtics, heat, 110, 105));
        gameRepository.save(new Game(101L, requestDate, lakers, nuggets, 115, 108));

        TeamStanding celticsStanding = new TeamStanding(celtics);
        celticsStanding.setWins(1);
        celticsStanding.setLosses(0);
        celticsStanding.calculateWinPct();
        celticsStanding.setDivisionRank(1);
        celticsStanding.setConferenceRank(1);
        standingsSnapshotRepository.save(StandingsSnapshot.fromTeamStanding(requestDate, celticsStanding));

        TeamStanding heatStanding = new TeamStanding(heat);
        heatStanding.setWins(0);
        heatStanding.setLosses(1);
        heatStanding.calculateWinPct();
        heatStanding.setDivisionRank(2);
        heatStanding.setConferenceRank(2);
        standingsSnapshotRepository.save(StandingsSnapshot.fromTeamStanding(requestDate, heatStanding));

        TeamStanding lakersStanding = new TeamStanding(lakers);
        lakersStanding.setWins(1);
        lakersStanding.setLosses(0);
        lakersStanding.calculateWinPct();
        lakersStanding.setDivisionRank(1);
        lakersStanding.setConferenceRank(1);
        standingsSnapshotRepository.save(StandingsSnapshot.fromTeamStanding(requestDate, lakersStanding));

        TeamStanding nuggetsStanding = new TeamStanding(nuggets);
        nuggetsStanding.setWins(0);
        nuggetsStanding.setLosses(1);
        nuggetsStanding.calculateWinPct();
        nuggetsStanding.setDivisionRank(2);
        nuggetsStanding.setConferenceRank(2);
        standingsSnapshotRepository.save(StandingsSnapshot.fromTeamStanding(requestDate, nuggetsStanding));

        // Get standings
        Map<String, List<TeamStanding>> standings = standingsService.getStandings(requestDate, GroupBy.CONFERENCE);

        // Verify API was NOT called
        verify(nbaApiClient, never()).getAllGames(any(LocalDate.class), any(LocalDate.class));

        // Verify standings were retrieved from cache
        assertNotNull(standings);
        assertTrue(standings.containsKey("EASTERN"));
        assertTrue(standings.containsKey("WESTERN"));
        assertEquals(2, standings.get("EASTERN").size());
        assertEquals(2, standings.get("WESTERN").size());
    }

    @Test
    void testGetStandings_ByDivision_CorrectGrouping() {
        LocalDate requestDate = LocalDate.of(2024, 10, 22);

        // Mock NBA API response
        NBAGamesResponse mockResponse = createMockApiResponse();
        when(nbaApiClient.getAllGames(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Mono.just(mockResponse));

        // Get standings by division
        Map<String, List<TeamStanding>> standings = standingsService.getStandings(requestDate, GroupBy.DIVISION);

        // Verify division grouping
        assertNotNull(standings);
        assertTrue(standings.containsKey("ATLANTIC"));
        assertTrue(standings.containsKey("SOUTHEAST"));
        assertTrue(standings.containsKey("PACIFIC"));
        assertTrue(standings.containsKey("NORTHWEST"));

        // Verify teams are in correct divisions
        List<TeamStanding> atlanticStandings = standings.get("ATLANTIC");
        assertEquals(1, atlanticStandings.size());
        assertEquals("Boston Celtics", atlanticStandings.get(0).getTeam().getTeamName());
    }

    @Test
    void testGetStandings_ByConference_CorrectGrouping() {
        LocalDate requestDate = LocalDate.of(2024, 10, 22);

        // Mock NBA API response
        NBAGamesResponse mockResponse = createMockApiResponse();
        when(nbaApiClient.getAllGames(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Mono.just(mockResponse));

        // Get standings by conference
        Map<String, List<TeamStanding>> standings = standingsService.getStandings(requestDate, GroupBy.CONFERENCE);

        // Verify conference grouping
        assertNotNull(standings);
        assertTrue(standings.containsKey("EASTERN"));
        assertTrue(standings.containsKey("WESTERN"));

        // Verify teams are in correct conferences
        List<TeamStanding> easternStandings = standings.get("EASTERN");
        assertEquals(2, easternStandings.size());

        List<TeamStanding> westernStandings = standings.get("WESTERN");
        assertEquals(2, westernStandings.size());
    }

    @Test
    void testGetStandings_CorrectRankings() {
        LocalDate requestDate = LocalDate.of(2024, 10, 24);

        // Create games with clear win-loss records
        gameRepository.save(new Game(100L, LocalDate.of(2024, 10, 22), celtics, heat, 110, 105));
        gameRepository.save(new Game(101L, LocalDate.of(2024, 10, 23), celtics, lakers, 115, 108));
        gameRepository.save(new Game(102L, LocalDate.of(2024, 10, 24), heat, nuggets, 105, 100));

        // Mock empty API response (no new games to fetch)
        NBAGamesResponse emptyResponse = new NBAGamesResponse();
        emptyResponse.setData(List.of());
        when(nbaApiClient.getAllGames(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Mono.just(emptyResponse));

        // Get standings
        Map<String, List<TeamStanding>> standings = standingsService.getStandings(requestDate, GroupBy.CONFERENCE);

        // Verify Eastern Conference rankings
        List<TeamStanding> easternStandings = standings.get("EASTERN");
        assertEquals("Boston Celtics", easternStandings.get(0).getTeam().getTeamName());
        assertEquals(2, easternStandings.get(0).getWins());
        assertEquals(0, easternStandings.get(0).getLosses());
        assertEquals(1, easternStandings.get(0).getConferenceRank());

        assertEquals("Miami Heat", easternStandings.get(1).getTeam().getTeamName());
        assertEquals(1, easternStandings.get(1).getWins());
        assertEquals(1, easternStandings.get(1).getLosses());
        assertEquals(2, easternStandings.get(1).getConferenceRank());
    }

    @Test
    void testGetStandings_IncrementalFetch_OnlyFetchesNewGames() {
        // Create existing game
        LocalDate existingGameDate = LocalDate.of(2024, 10, 22);
        gameRepository.save(new Game(100L, existingGameDate, celtics, heat, 110, 105));

        // Request standings for a later date
        LocalDate requestDate = LocalDate.of(2024, 10, 24);

        // Mock API response with new games
        NBAGamesResponse mockResponse = createMockApiResponse();
        when(nbaApiClient.getAllGames(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Mono.just(mockResponse));

        // Get standings
        standingsService.getStandings(requestDate, GroupBy.DIVISION);

        // Verify API was called with optimized date range (from most recent game date)
        verify(nbaApiClient, times(1)).getAllGames(eq(existingGameDate), eq(requestDate));
    }

    /**
     * Helper method to create mock NBA API response
     */
    private NBAGamesResponse createMockApiResponse() {
        NBAGamesResponse response = new NBAGamesResponse();

        NBAGameDTO game1 = new NBAGameDTO();
        game1.setId(100L);
        game1.setDate(LocalDate.of(2024, 10, 22));
        game1.setStatus("Final");
        game1.setHomeTeamScore(110);
        game1.setVisitorTeamScore(105);

        NBATeamDTO homeTeam1 = new NBATeamDTO();
        homeTeam1.setId(1L);
        game1.setHomeTeam(homeTeam1);

        NBATeamDTO awayTeam1 = new NBATeamDTO();
        awayTeam1.setId(2L);
        game1.setVisitorTeam(awayTeam1);

        NBAGameDTO game2 = new NBAGameDTO();
        game2.setId(101L);
        game2.setDate(LocalDate.of(2024, 10, 22));
        game2.setStatus("Final");
        game2.setHomeTeamScore(115);
        game2.setVisitorTeamScore(108);

        NBATeamDTO homeTeam2 = new NBATeamDTO();
        homeTeam2.setId(3L);
        game2.setHomeTeam(homeTeam2);

        NBATeamDTO awayTeam2 = new NBATeamDTO();
        awayTeam2.setId(4L);
        game2.setVisitorTeam(awayTeam2);

        response.setData(List.of(game1, game2));
        return response;
    }
}
