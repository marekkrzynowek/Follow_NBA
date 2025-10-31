package com.nba.standings.controller;

import com.nba.standings.client.NBAApiClient;
import com.nba.standings.dto.NBAGamesResponse;
import com.nba.standings.model.entity.Game;
import com.nba.standings.model.entity.Team;
import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import com.nba.standings.repository.GameRepository;
import com.nba.standings.repository.StandingsSnapshotRepository;
import com.nba.standings.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Integration tests for StandingsController REST endpoints.
 * Tests the full request-response cycle with real database operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class StandingsControllerIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(StandingsControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private StandingsSnapshotRepository standingsSnapshotRepository;

    @MockBean
    private NBAApiClient nbaApiClient;

    private Team celtics;
    private Team lakers;
    private Team heat;
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

        // Create test games
        LocalDate gameDate = LocalDate.of(2025, 10, 22);
        gameRepository.save(new Game(100L, gameDate, celtics, heat, 110, 105));
        gameRepository.save(new Game(101L, gameDate, lakers, nuggets, 115, 108));

        // Mock NBA API to return empty response (no new games to fetch)
        NBAGamesResponse emptyResponse = new NBAGamesResponse();
        emptyResponse.setData(List.of());
        when(nbaApiClient.getAllGames(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Mono.just(emptyResponse));
    }

    @Test
    void testGetStandings_ByDivision_Success() throws Exception {
        logger.info("========== TEST START: testGetStandings_ByDivision_Success ==========");
        logger.info("TEST: Sending request with date='2025-10-22' and groupBy='DIVISION'");
        
        LocalDate testDate = LocalDate.of(2025, 10, 22);
        logger.info("TEST: LocalDate object created: {}", testDate);
        logger.info("TEST: Date as string: {}", testDate.toString());
        
        // Check what games exist in the database
        List<Game> existingGames = gameRepository.findAll();
        logger.info("TEST: Found {} games in database before request", existingGames.size());
        for (Game game : existingGames) {
            logger.info("TEST: Game - Date: {}, Home: {} ({}), Away: {} ({}), Score: {}-{}", 
                game.getGameDate(), 
                game.getHomeTeam().getTeamName(), 
                game.getHomeScore(),
                game.getAwayTeam().getTeamName(),
                game.getAwayScore(),
                game.getHomeScore(),
                game.getAwayScore());
        }
        
        MvcResult result = mockMvc.perform(get("/api/standings")
                        .param("date", "2025-10-22")
                        .param("groupBy", "DIVISION"))
                .andDo(print())  // Print full request/response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2025-10-22"))
                .andExpect(jsonPath("$.groupBy").value("DIVISION"))
                .andExpect(jsonPath("$.standings").isMap())
                .andExpect(jsonPath("$.standings.ATLANTIC").isArray())
                .andExpect(jsonPath("$.standings.ATLANTIC[0].teamName").value("Boston Celtics"))
                .andExpect(jsonPath("$.standings.ATLANTIC[0].wins").value(1))
                .andExpect(jsonPath("$.standings.ATLANTIC[0].losses").value(0))
                .andExpect(jsonPath("$.standings.ATLANTIC[0].rank").value(1))
                .andReturn();
        
        logger.info("TEST: Response body: {}", result.getResponse().getContentAsString());
        logger.info("========== TEST END: testGetStandings_ByDivision_Success ==========");
    }

    @Test
    void testGetStandings_ByConference_Success() throws Exception {
        mockMvc.perform(get("/api/standings")
                        .param("date", "2025-10-22")
                        .param("groupBy", "CONFERENCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2025-10-22"))
                .andExpect(jsonPath("$.groupBy").value("CONFERENCE"))
                .andExpect(jsonPath("$.standings").isMap())
                .andExpect(jsonPath("$.standings.EASTERN").isArray())
                .andExpect(jsonPath("$.standings.EASTERN", hasSize(2)))
                .andExpect(jsonPath("$.standings.WESTERN", hasSize(2)));
    }

    @Test
    void testGetStandings_InvalidDate_BadRequest() throws Exception {
        mockMvc.perform(get("/api/standings")
                        .param("date", "2020-01-01")
                        .param("groupBy", "DIVISION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void testGetStandings_InvalidGroupBy_BadRequest() throws Exception {
        mockMvc.perform(get("/api/standings")
                        .param("date", "2025-10-22")
                        .param("groupBy", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testGetStandings_MissingDate_BadRequest() throws Exception {
        mockMvc.perform(get("/api/standings")
                        .param("groupBy", "division"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetStandings_MissingGroupBy_BadRequest() throws Exception {
        mockMvc.perform(get("/api/standings")
                        .param("date", "2025-10-22"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetStandings_CachedData_Success() throws Exception {
        // First request - calculates and caches
        mockMvc.perform(get("/api/standings")
                        .param("date", "2025-10-22")
                        .param("groupBy", "DIVISION"))
                .andExpect(status().isOk());

        // Second request - should use cache
        mockMvc.perform(get("/api/standings")
                        .param("date", "2025-10-22")
                        .param("groupBy", "CONFERENCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2025-10-22"))
                .andExpect(jsonPath("$.groupBy").value("CONFERENCE"));
    }

    @Test
    void testGetStandings_CorrectRankings() throws Exception {
        // Add more games to create clear rankings
        LocalDate gameDate2 = LocalDate.of(2025, 10, 24);
        gameRepository.save(new Game(102L, gameDate2, celtics, lakers, 120, 110));
        gameRepository.save(new Game(103L, gameDate2, heat, nuggets, 105, 100));

        mockMvc.perform(get("/api/standings")
                        .param("date", "2025-10-24")
                        .param("groupBy", "CONFERENCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standings.EASTERN[0].teamName").value("Boston Celtics"))
                .andExpect(jsonPath("$.standings.EASTERN[0].wins").value(2))
                .andExpect(jsonPath("$.standings.EASTERN[0].losses").value(0))
                .andExpect(jsonPath("$.standings.EASTERN[0].rank").value(1))
                .andExpect(jsonPath("$.standings.EASTERN[1].teamName").value("Miami Heat"))
                .andExpect(jsonPath("$.standings.EASTERN[1].wins").value(1))
                .andExpect(jsonPath("$.standings.EASTERN[1].losses").value(1))
                .andExpect(jsonPath("$.standings.EASTERN[1].rank").value(2));
    }
}
