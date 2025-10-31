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
import com.nba.standings.util.SeasonDateUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StandingsServiceTest {

    @Mock
    private StandingsSnapshotRepository standingsSnapshotRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private NBADataService nbaDataService;

    @Mock
    private StandingsCalculator standingsCalculator;

    @Mock
    private SeasonDateUtility seasonDateUtility;

    @InjectMocks
    private StandingsService standingsService;

    private Team celtics;
    private Team lakers;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        celtics = new Team(1, "Boston Celtics", "BOS", Division.ATLANTIC, Conference.EASTERN);
        lakers = new Team(2, "Los Angeles Lakers", "LAL", Division.PACIFIC, Conference.WESTERN);
        testDate = LocalDate.of(2025, 10, 24);
    }

    @Test
    void testGetStandings_WhenCached() {
        when(standingsSnapshotRepository.existsBySnapshotDate(testDate)).thenReturn(true);
        
        StandingsSnapshot snapshot1 = new StandingsSnapshot(testDate, celtics, 3, 0, new BigDecimal("1.000"), 1, 1);
        when(standingsSnapshotRepository.findBySnapshotDateAndTeam_Division(testDate, Division.ATLANTIC))
                .thenReturn(List.of(snapshot1));
        
        Map<String, List<TeamStanding>> result = standingsService.getStandings(testDate, GroupBy.DIVISION);
        
        assertNotNull(result);
        verify(nbaDataService, never()).fetchAndSaveGames(any(), any());
        verify(standingsCalculator, never()).calculateStandings(any(), any());
    }

    @Test
    void testGetStandings_WhenNotCached() {
        when(standingsSnapshotRepository.existsBySnapshotDate(testDate)).thenReturn(false);
        when(gameRepository.findMostRecentGameDate()).thenReturn(null);
        when(seasonDateUtility.determineSeasonStart(testDate)).thenReturn(LocalDate.of(2025, 10, 1));
        
        List<Game> games = new ArrayList<>();
        when(gameRepository.findByGameDateLessThanEqual(testDate)).thenReturn(games);
        
        List<Team> teams = List.of(celtics, lakers);
        when(teamRepository.findAll()).thenReturn(teams);
        
        Map<Long, TeamStanding> standings = new HashMap<>();
        TeamStanding celticsStanding = new TeamStanding(celtics);
        standings.put(celtics.getId(), celticsStanding);
        when(standingsCalculator.calculateStandings(games, teams)).thenReturn(standings);
        
        StandingsSnapshot snapshot = new StandingsSnapshot(testDate, celtics, 0, 0, BigDecimal.ZERO, 1, 1);
        when(standingsSnapshotRepository.findBySnapshotDateAndTeam_Division(testDate, Division.ATLANTIC))
                .thenReturn(List.of(snapshot));
        
        Map<String, List<TeamStanding>> result = standingsService.getStandings(testDate, GroupBy.DIVISION);
        
        assertNotNull(result);
        verify(nbaDataService).fetchAndSaveGames(any(), eq(testDate));
        verify(standingsCalculator).calculateStandings(games, teams);
        verify(standingsCalculator).assignDivisionRanks(standings);
        verify(standingsCalculator).assignConferenceRanks(standings);
        verify(standingsSnapshotRepository).saveAll(anyList());
    }

    @Test
    void testGetStandings_GroupByConference() {
        when(standingsSnapshotRepository.existsBySnapshotDate(testDate)).thenReturn(true);
        
        StandingsSnapshot snapshot1 = new StandingsSnapshot(testDate, celtics, 3, 0, new BigDecimal("1.000"), 1, 1);
        when(standingsSnapshotRepository.findBySnapshotDateAndTeam_Conference(testDate, Conference.EASTERN))
                .thenReturn(List.of(snapshot1));
        when(standingsSnapshotRepository.findBySnapshotDateAndTeam_Conference(testDate, Conference.WESTERN))
                .thenReturn(new ArrayList<>());
        
        Map<String, List<TeamStanding>> result = standingsService.getStandings(testDate, GroupBy.CONFERENCE);
        
        assertNotNull(result);
        assertTrue(result.containsKey("EASTERN"));
        assertTrue(result.containsKey("WESTERN"));
    }
}
