package com.nba.standings.repository;

import com.nba.standings.model.entity.Game;
import com.nba.standings.model.entity.Team;
import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class GameRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameRepository gameRepository;

    private Team celtics;
    private Team lakers;

    @BeforeEach
    void setUp() {
        celtics = new Team(1, "Boston Celtics", "BOS", Division.ATLANTIC, Conference.EASTERN);
        lakers = new Team(2, "Los Angeles Lakers", "LAL", Division.PACIFIC, Conference.WESTERN);
        
        entityManager.persist(celtics);
        entityManager.persist(lakers);
        entityManager.flush();
    }

    @Test
    void testFindByGameDateBetween() {
        LocalDate date1 = LocalDate.of(2025, 10, 20);
        LocalDate date2 = LocalDate.of(2025, 10, 22);
        LocalDate date3 = LocalDate.of(2025, 10, 25);
        
        Game game1 = new Game(100L, date1, celtics, lakers, 110, 105);
        Game game2 = new Game(101L, date2, lakers, celtics, 100, 95);
        Game game3 = new Game(102L, date3, celtics, lakers, 115, 110);
        
        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();
        
        List<Game> result = gameRepository.findByGameDateBetween(date1, date2);
        
        assertEquals(2, result.size());
    }

    @Test
    void testFindByGameDateLessThanEqual() {
        LocalDate date1 = LocalDate.of(2025, 10, 20);
        LocalDate date2 = LocalDate.of(2025, 10, 22);
        LocalDate date3 = LocalDate.of(2025, 10, 25);
        
        Game game1 = new Game(100L, date1, celtics, lakers, 110, 105);
        Game game2 = new Game(101L, date2, lakers, celtics, 100, 95);
        Game game3 = new Game(102L, date3, celtics, lakers, 115, 110);
        
        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();
        
        List<Game> result = gameRepository.findByGameDateLessThanEqual(date2);
        
        assertEquals(2, result.size());
    }

    @Test
    void testExistsByNbaGameId() {
        Game game = new Game(100L, LocalDate.of(2025, 10, 20), celtics, lakers, 110, 105);
        entityManager.persist(game);
        entityManager.flush();
        
        assertTrue(gameRepository.existsByNbaGameId(100L));
        assertFalse(gameRepository.existsByNbaGameId(999L));
    }

    @Test
    void testFindMostRecentGameDate() {
        LocalDate date1 = LocalDate.of(2025, 10, 20);
        LocalDate date2 = LocalDate.of(2025, 10, 25);
        
        Game game1 = new Game(100L, date1, celtics, lakers, 110, 105);
        Game game2 = new Game(101L, date2, lakers, celtics, 100, 95);
        
        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.flush();
        
        LocalDate result = gameRepository.findMostRecentGameDate();
        
        assertEquals(date2, result);
    }

    @Test
    void testFindMostRecentGameDate_NoGames() {
        LocalDate result = gameRepository.findMostRecentGameDate();
        
        assertNull(result);
    }
}
