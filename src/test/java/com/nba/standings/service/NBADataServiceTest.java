package com.nba.standings.service;

import com.nba.standings.client.NBAApiClient;
import com.nba.standings.dto.NBAGameDTO;
import com.nba.standings.dto.NBAGamesResponse;
import com.nba.standings.dto.NBATeamDTO;
import com.nba.standings.exception.NBAApiException;
import com.nba.standings.model.entity.Game;
import com.nba.standings.model.entity.Team;
import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import com.nba.standings.repository.GameRepository;
import com.nba.standings.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NBADataServiceTest {

    @Mock
    private NBAApiClient nbaApiClient;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private NBADataService nbaDataService;

    private Team celtics;
    private Team lakers;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        celtics = new Team(1, "Boston Celtics", "BOS", Division.ATLANTIC, Conference.EASTERN);
        lakers = new Team(2, "Los Angeles Lakers", "LAL", Division.PACIFIC, Conference.WESTERN);
        startDate = LocalDate.of(2025, 10, 1);
        endDate = LocalDate.of(2025, 10, 24);
    }
    
    private Team createTeamWithId(Long id, Team team) throws Exception {
        Field idField = Team.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(team, id);
        return team;
    }

    @Test
    void testFetchAndSaveGames_Success() throws Exception {
        // Create teams with IDs
        Team celticsWithId = createTeamWithId(1L, celtics);
        Team lakersWithId = createTeamWithId(2L, lakers);
        
        NBATeamDTO homeTeamDTO = new NBATeamDTO();
        homeTeamDTO.setId(1L);
        
        NBATeamDTO awayTeamDTO = new NBATeamDTO();
        awayTeamDTO.setId(2L);
        
        NBAGameDTO gameDTO = new NBAGameDTO();
        gameDTO.setId(100L);
        gameDTO.setDate(LocalDate.of(2025, 10, 24));
        gameDTO.setHomeTeam(homeTeamDTO);
        gameDTO.setVisitorTeam(awayTeamDTO);
        gameDTO.setHomeTeamScore(110);
        gameDTO.setVisitorTeamScore(105);
        gameDTO.setStatus("Final");
        
        NBAGamesResponse response = new NBAGamesResponse();
        response.setData(List.of(gameDTO));
        
        when(nbaApiClient.getAllGames(startDate, endDate)).thenReturn(Mono.just(response));
        when(teamRepository.findAll()).thenReturn(List.of(celticsWithId, lakersWithId));
        when(gameRepository.existsByNbaGameId(100L)).thenReturn(false);
        when(gameRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        List<Game> result = nbaDataService.fetchAndSaveGames(startDate, endDate);
        
        assertEquals(1, result.size());
        verify(gameRepository).saveAll(anyList());
    }

    @Test
    void testFetchAndSaveGames_SkipsDuplicates() throws Exception {
        // Create teams with IDs
        Team celticsWithId = createTeamWithId(1L, celtics);
        Team lakersWithId = createTeamWithId(2L, lakers);
        
        NBATeamDTO homeTeamDTO = new NBATeamDTO();
        homeTeamDTO.setId(1L);
        
        NBATeamDTO awayTeamDTO = new NBATeamDTO();
        awayTeamDTO.setId(2L);
        
        NBAGameDTO gameDTO = new NBAGameDTO();
        gameDTO.setId(100L);
        gameDTO.setDate(LocalDate.of(2025, 10, 24));
        gameDTO.setHomeTeam(homeTeamDTO);
        gameDTO.setVisitorTeam(awayTeamDTO);
        gameDTO.setHomeTeamScore(110);
        gameDTO.setVisitorTeamScore(105);
        gameDTO.setStatus("Final");
        
        NBAGamesResponse response = new NBAGamesResponse();
        response.setData(List.of(gameDTO));
        
        when(nbaApiClient.getAllGames(startDate, endDate)).thenReturn(Mono.just(response));
        when(teamRepository.findAll()).thenReturn(List.of(celticsWithId, lakersWithId));
        when(gameRepository.existsByNbaGameId(100L)).thenReturn(true);
        
        List<Game> result = nbaDataService.fetchAndSaveGames(startDate, endDate);
        
        assertEquals(0, result.size());
        verify(gameRepository, never()).saveAll(anyList());
    }

    @Test
    void testFetchAndSaveGames_SkipsNonFinalGames() throws Exception {
        // Create teams with IDs
        Team celticsWithId = createTeamWithId(1L, celtics);
        Team lakersWithId = createTeamWithId(2L, lakers);
        
        NBATeamDTO homeTeamDTO = new NBATeamDTO();
        homeTeamDTO.setId(1L);
        
        NBATeamDTO awayTeamDTO = new NBATeamDTO();
        awayTeamDTO.setId(2L);
        
        NBAGameDTO gameDTO = new NBAGameDTO();
        gameDTO.setId(100L);
        gameDTO.setDate(LocalDate.of(2025, 10, 24));
        gameDTO.setHomeTeam(homeTeamDTO);
        gameDTO.setVisitorTeam(awayTeamDTO);
        gameDTO.setHomeTeamScore(110);
        gameDTO.setVisitorTeamScore(105);
        gameDTO.setStatus("In Progress");
        
        NBAGamesResponse response = new NBAGamesResponse();
        response.setData(List.of(gameDTO));
        
        when(nbaApiClient.getAllGames(startDate, endDate)).thenReturn(Mono.just(response));
        when(teamRepository.findAll()).thenReturn(List.of(celticsWithId, lakersWithId));
        when(gameRepository.existsByNbaGameId(100L)).thenReturn(false);
        
        List<Game> result = nbaDataService.fetchAndSaveGames(startDate, endDate);
        
        assertEquals(0, result.size());
        verify(gameRepository, never()).saveAll(anyList());
    }

    @Test
    void testFetchAndSaveGames_ThrowsExceptionOnApiFailure() {
        when(nbaApiClient.getAllGames(startDate, endDate))
                .thenReturn(Mono.error(new RuntimeException("API Error")));
        
        assertThrows(NBAApiException.class, () -> {
            nbaDataService.fetchAndSaveGames(startDate, endDate);
        });
    }

    @Test
    void testFetchAndSaveGames_ThrowsExceptionOnNullResponse() {
        when(nbaApiClient.getAllGames(startDate, endDate)).thenReturn(Mono.empty());
        
        assertThrows(NBAApiException.class, () -> {
            nbaDataService.fetchAndSaveGames(startDate, endDate);
        });
    }
}
