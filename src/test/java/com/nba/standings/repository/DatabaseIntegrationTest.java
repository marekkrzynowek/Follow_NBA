package com.nba.standings.repository;

import com.nba.standings.model.entity.Game;
import com.nba.standings.model.entity.StandingsSnapshot;
import com.nba.standings.model.entity.Team;
import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for database operations across multiple repositories.
 * Tests complex queries and relationships between entities.
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class DatabaseIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private StandingsSnapshotRepository standingsSnapshotRepository;

    private Team celtics;
    private Team heat;
    private Team lakers;
    private Team nuggets;
    private Team warriors;
    private Team suns;

    @BeforeEach
    void setUp() {
        // Create teams from different divisions and conferences
        celtics = new Team(1, "Boston Celtics", "BOS", Division.ATLANTIC, Conference.EASTERN);
        heat = new Team(2, "Miami Heat", "MIA", Division.SOUTHEAST, Conference.EASTERN);
        lakers = new Team(3, "Los Angeles Lakers", "LAL", Division.PACIFIC, Conference.WESTERN);
        nuggets = new Team(4, "Denver Nuggets", "DEN", Division.NORTHWEST, Conference.WESTERN);
        warriors = new Team(5, "Golden State Warriors", "GSW", Division.PACIFIC, Conference.WESTERN);
        suns = new Team(6, "Phoenix Suns", "PHX", Division.PACIFIC, Conference.WESTERN);

        entityManager.persist(celtics);
        entityManager.persist(heat);
        entityManager.persist(lakers);
        entityManager.persist(nuggets);
        entityManager.persist(warriors);
        entityManager.persist(suns);
        entityManager.flush();
    }

    @Test
    void testTeamRepository_FindByDivision() {
        List<Team> pacificTeams = teamRepository.findByDivision(Division.PACIFIC);

        assertEquals(3, pacificTeams.size());
        assertTrue(pacificTeams.stream().allMatch(t -> t.getDivision() == Division.PACIFIC));
    }

    @Test
    void testTeamRepository_FindByConference() {
        List<Team> easternTeams = teamRepository.findByConference(Conference.EASTERN);

        assertEquals(2, easternTeams.size());
        assertTrue(easternTeams.stream().allMatch(t -> t.getConference() == Conference.EASTERN));
    }

    @Test
    void testTeamRepository_FindByNbaTeamId() {
        Optional<Team> found = teamRepository.findByNbaTeamId(1);

        assertTrue(found.isPresent());
        assertEquals("Boston Celtics", found.get().getTeamName());
    }

    @Test
    void testGameRepository_ComplexDateRangeQuery() {
        LocalDate date1 = LocalDate.of(2024, 10, 20);
        LocalDate date2 = LocalDate.of(2024, 10, 22);
        LocalDate date3 = LocalDate.of(2024, 10, 24);
        LocalDate date4 = LocalDate.of(2024, 10, 26);

        entityManager.persist(new Game(100L, date1, celtics, heat, 110, 105));
        entityManager.persist(new Game(101L, date2, lakers, nuggets, 115, 108));
        entityManager.persist(new Game(102L, date3, warriors, suns, 120, 118));
        entityManager.persist(new Game(103L, date4, celtics, lakers, 105, 100));
        entityManager.flush();

        List<Game> gamesInRange = gameRepository.findByGameDateBetween(date2, date3);

        assertEquals(2, gamesInRange.size());
        assertTrue(gamesInRange.stream().allMatch(g -> 
            !g.getGameDate().isBefore(date2) && !g.getGameDate().isAfter(date3)));
    }

    @Test
    void testGameRepository_TeamRelationships() {
        LocalDate gameDate = LocalDate.of(2024, 10, 22);
        Game game = new Game(100L, gameDate, celtics, heat, 110, 105);
        entityManager.persist(game);
        entityManager.flush();
        entityManager.clear();

        Game found = gameRepository.findById(game.getId()).orElseThrow();

        assertNotNull(found.getHomeTeam());
        assertNotNull(found.getAwayTeam());
        assertEquals("Boston Celtics", found.getHomeTeam().getTeamName());
        assertEquals("Miami Heat", found.getAwayTeam().getTeamName());
    }

    @Test
    void testStandingsSnapshotRepository_FindBySnapshotDate() {
        LocalDate snapshotDate = LocalDate.of(2024, 10, 22);

        StandingsSnapshot snapshot1 = new StandingsSnapshot(snapshotDate, celtics, 5, 2, new BigDecimal("0.714"), 1, 3);
        StandingsSnapshot snapshot2 = new StandingsSnapshot(snapshotDate, heat, 4, 3, new BigDecimal("0.571"), 2, 5);
        StandingsSnapshot snapshot3 = new StandingsSnapshot(snapshotDate, lakers, 6, 1, new BigDecimal("0.857"), 1, 2);

        entityManager.persist(snapshot1);
        entityManager.persist(snapshot2);
        entityManager.persist(snapshot3);
        entityManager.flush();

        List<StandingsSnapshot> snapshots = standingsSnapshotRepository.findBySnapshotDate(snapshotDate);

        assertEquals(3, snapshots.size());
    }

    @Test
    void testStandingsSnapshotRepository_ExistsBySnapshotDate() {
        LocalDate snapshotDate = LocalDate.of(2024, 10, 22);
        LocalDate otherDate = LocalDate.of(2024, 10, 24);

        StandingsSnapshot snapshot = new StandingsSnapshot(snapshotDate, celtics, 5, 2, new BigDecimal("0.714"), 1, 3);
        entityManager.persist(snapshot);
        entityManager.flush();

        assertTrue(standingsSnapshotRepository.existsBySnapshotDate(snapshotDate));
        assertFalse(standingsSnapshotRepository.existsBySnapshotDate(otherDate));
    }

    @Test
    void testStandingsSnapshotRepository_FindBySnapshotDateAndDivision() {
        LocalDate snapshotDate = LocalDate.of(2024, 10, 22);

        StandingsSnapshot snapshot1 = new StandingsSnapshot(snapshotDate, celtics, 5, 2, new BigDecimal("0.714"), 1, 3);
        StandingsSnapshot snapshot2 = new StandingsSnapshot(snapshotDate, heat, 4, 3, new BigDecimal("0.571"), 1, 5);
        StandingsSnapshot snapshot3 = new StandingsSnapshot(snapshotDate, lakers, 6, 1, new BigDecimal("0.857"), 1, 2);
        StandingsSnapshot snapshot4 = new StandingsSnapshot(snapshotDate, warriors, 5, 2, new BigDecimal("0.714"), 2, 4);

        entityManager.persist(snapshot1);
        entityManager.persist(snapshot2);
        entityManager.persist(snapshot3);
        entityManager.persist(snapshot4);
        entityManager.flush();

        List<StandingsSnapshot> atlanticSnapshots = standingsSnapshotRepository
                .findBySnapshotDateAndTeam_Division(snapshotDate, Division.ATLANTIC);
        List<StandingsSnapshot> pacificSnapshots = standingsSnapshotRepository
                .findBySnapshotDateAndTeam_Division(snapshotDate, Division.PACIFIC);

        assertEquals(1, atlanticSnapshots.size());
        assertEquals("Boston Celtics", atlanticSnapshots.get(0).getTeam().getTeamName());
        assertEquals(2, pacificSnapshots.size());
    }

    @Test
    void testStandingsSnapshotRepository_FindBySnapshotDateAndConference() {
        LocalDate snapshotDate = LocalDate.of(2024, 10, 22);

        StandingsSnapshot snapshot1 = new StandingsSnapshot(snapshotDate, celtics, 5, 2, new BigDecimal("0.714"), 1, 3);
        StandingsSnapshot snapshot2 = new StandingsSnapshot(snapshotDate, heat, 4, 3, new BigDecimal("0.571"), 2, 5);
        StandingsSnapshot snapshot3 = new StandingsSnapshot(snapshotDate, lakers, 6, 1, new BigDecimal("0.857"), 1, 2);

        entityManager.persist(snapshot1);
        entityManager.persist(snapshot2);
        entityManager.persist(snapshot3);
        entityManager.flush();

        List<StandingsSnapshot> easternSnapshots = standingsSnapshotRepository
                .findBySnapshotDateAndTeam_Conference(snapshotDate, Conference.EASTERN);
        List<StandingsSnapshot> westernSnapshots = standingsSnapshotRepository
                .findBySnapshotDateAndTeam_Conference(snapshotDate, Conference.WESTERN);

        assertEquals(2, easternSnapshots.size());
        assertEquals(1, westernSnapshots.size());
    }

    @Test
    void testStandingsSnapshotRepository_UniqueConstraint() {
        LocalDate snapshotDate = LocalDate.of(2024, 10, 22);

        StandingsSnapshot snapshot1 = new StandingsSnapshot(snapshotDate, celtics, 5, 2, new BigDecimal("0.714"), 1, 3);
        entityManager.persist(snapshot1);
        entityManager.flush();

        // Try to insert duplicate (same date and team)
        StandingsSnapshot snapshot2 = new StandingsSnapshot(snapshotDate, celtics, 6, 1, new BigDecimal("0.857"), 1, 1);

        assertThrows(Exception.class, () -> {
            entityManager.persist(snapshot2);
            entityManager.flush();
        });
    }

    @Test
    void testCrossRepositoryIntegration_FullWorkflow() {
        // Create games
        LocalDate gameDate = LocalDate.of(2024, 10, 22);
        Game game1 = new Game(100L, gameDate, celtics, heat, 110, 105);
        Game game2 = new Game(101L, gameDate, lakers, nuggets, 115, 108);
        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.flush();

        // Verify games are saved
        List<Game> games = gameRepository.findByGameDateLessThanEqual(gameDate);
        assertEquals(2, games.size());

        // Create standings snapshots
        StandingsSnapshot snapshot1 = new StandingsSnapshot(gameDate, celtics, 1, 0, new BigDecimal("1.000"), 1, 1);
        StandingsSnapshot snapshot2 = new StandingsSnapshot(gameDate, heat, 0, 1, new BigDecimal("0.000"), 2, 2);
        entityManager.persist(snapshot1);
        entityManager.persist(snapshot2);
        entityManager.flush();

        // Verify snapshots are saved and queryable
        assertTrue(standingsSnapshotRepository.existsBySnapshotDate(gameDate));
        List<StandingsSnapshot> easternSnapshots = standingsSnapshotRepository
                .findBySnapshotDateAndTeam_Conference(gameDate, Conference.EASTERN);
        assertEquals(2, easternSnapshots.size());
    }
}
