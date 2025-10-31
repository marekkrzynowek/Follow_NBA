package com.nba.standings.service;

import com.nba.standings.model.entity.Game;
import com.nba.standings.model.entity.Team;
import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import com.nba.standings.service.StandingsCalculator.TeamStanding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StandingsCalculatorTest {

    private StandingsCalculator calculator;
    private Team celtics;
    private Team lakers;
    private Team warriors;

    @BeforeEach
    void setUp() throws Exception {
        calculator = new StandingsCalculator();
        
        // Create test teams with IDs set via reflection
        celtics = createTeamWithId(1L, 1, "Boston Celtics", "BOS", Division.ATLANTIC, Conference.EASTERN);
        lakers = createTeamWithId(2L, 2, "Los Angeles Lakers", "LAL", Division.PACIFIC, Conference.WESTERN);
        warriors = createTeamWithId(3L, 3, "Golden State Warriors", "GSW", Division.PACIFIC, Conference.WESTERN);
    }
    
    private Team createTeamWithId(Long id, Integer nbaTeamId, String name, String abbr, Division division, Conference conference) throws Exception {
        Team team = new Team(nbaTeamId, name, abbr, division, conference);
        Field idField = Team.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(team, id);
        return team;
    }

    @Test
    void testCalculateStandings_WithNoGames() {
        List<Game> games = new ArrayList<>();
        List<Team> teams = List.of(celtics, lakers, warriors);
        
        Map<Long, TeamStanding> standings = calculator.calculateStandings(games, teams);
        
        assertEquals(3, standings.size());
        for (TeamStanding standing : standings.values()) {
            assertEquals(0, standing.getWins());
            assertEquals(0, standing.getLosses());
            assertEquals(BigDecimal.ZERO, standing.getWinPct());
        }
    }

    @Test
    void testCalculateStandings_WithSingleGame() {
        Game game = new Game(1L, LocalDate.now(), celtics, lakers, 110, 105);
        List<Game> games = List.of(game);
        List<Team> teams = List.of(celtics, lakers, warriors);
        
        Map<Long, TeamStanding> standings = calculator.calculateStandings(games, teams);
        
        TeamStanding celticsStanding = standings.get(1L);
        TeamStanding lakersStanding = standings.get(2L);
        
        assertEquals(1, celticsStanding.getWins());
        assertEquals(0, celticsStanding.getLosses());
        assertEquals(0, new BigDecimal("1.000").compareTo(celticsStanding.getWinPct()));
        
        assertEquals(0, lakersStanding.getWins());
        assertEquals(1, lakersStanding.getLosses());
        assertEquals(0, BigDecimal.ZERO.compareTo(lakersStanding.getWinPct()));
    }

    @Test
    void testCalculateStandings_WithMultipleGames() {
        List<Game> games = List.of(
            new Game(1L, LocalDate.now(), celtics, lakers, 110, 105),
            new Game(2L, LocalDate.now(), warriors, celtics, 115, 110),
            new Game(3L, LocalDate.now(), lakers, warriors, 100, 95)
        );
        List<Team> teams = List.of(celtics, lakers, warriors);
        
        Map<Long, TeamStanding> standings = calculator.calculateStandings(games, teams);
        
        TeamStanding celticsStanding = standings.get(1L);
        TeamStanding lakersStanding = standings.get(2L);
        TeamStanding warriorsStanding = standings.get(3L);
        
        assertEquals(1, celticsStanding.getWins());
        assertEquals(1, celticsStanding.getLosses());
        assertEquals(new BigDecimal("0.500"), celticsStanding.getWinPct());
        
        assertEquals(1, lakersStanding.getWins());
        assertEquals(1, lakersStanding.getLosses());
        assertEquals(new BigDecimal("0.500"), lakersStanding.getWinPct());
        
        assertEquals(1, warriorsStanding.getWins());
        assertEquals(1, warriorsStanding.getLosses());
        assertEquals(new BigDecimal("0.500"), warriorsStanding.getWinPct());
    }

    @Test
    void testAssignDivisionRanks() {
        List<Game> games = List.of(
            new Game(1L, LocalDate.now(), lakers, warriors, 110, 105),
            new Game(2L, LocalDate.now(), lakers, warriors, 100, 95)
        );
        List<Team> teams = List.of(celtics, lakers, warriors);
        
        Map<Long, TeamStanding> standings = calculator.calculateStandings(games, teams);
        calculator.assignDivisionRanks(standings);
        
        TeamStanding lakersStanding = standings.get(2L);
        TeamStanding warriorsStanding = standings.get(3L);
        
        assertEquals(1, lakersStanding.getDivisionRank());
        assertEquals(2, warriorsStanding.getDivisionRank());
    }

    @Test
    void testAssignConferenceRanks() {
        List<Game> games = List.of(
            new Game(1L, LocalDate.now(), lakers, warriors, 110, 105),
            new Game(2L, LocalDate.now(), lakers, warriors, 100, 95)
        );
        List<Team> teams = List.of(celtics, lakers, warriors);
        
        Map<Long, TeamStanding> standings = calculator.calculateStandings(games, teams);
        calculator.assignConferenceRanks(standings);
        
        TeamStanding lakersStanding = standings.get(2L);
        TeamStanding warriorsStanding = standings.get(3L);
        
        assertEquals(1, lakersStanding.getConferenceRank());
        assertEquals(2, warriorsStanding.getConferenceRank());
    }

    @Test
    void testTeamStandingComparison_ByWinPct() {
        TeamStanding standing1 = new TeamStanding(celtics);
        standing1.setWins(10);
        standing1.setLosses(5);
        standing1.calculateWinPct();
        
        TeamStanding standing2 = new TeamStanding(lakers);
        standing2.setWins(8);
        standing2.setLosses(7);
        standing2.calculateWinPct();
        
        assertTrue(standing1.compareTo(standing2) < 0);
    }

    @Test
    void testTeamStandingComparison_ByWinsWhenTied() {
        TeamStanding standing1 = new TeamStanding(celtics);
        standing1.setWins(10);
        standing1.setLosses(10);
        standing1.calculateWinPct();
        
        TeamStanding standing2 = new TeamStanding(lakers);
        standing2.setWins(8);
        standing2.setLosses(8);
        standing2.calculateWinPct();
        
        assertTrue(standing1.compareTo(standing2) < 0);
    }
}
