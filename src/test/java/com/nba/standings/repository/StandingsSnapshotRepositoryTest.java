package com.nba.standings.repository;

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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class StandingsSnapshotRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StandingsSnapshotRepository standingsSnapshotRepository;

    private Team celtics;
    private Team lakers;
    private Team warriors;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        celtics = new Team(1, "Boston Celtics", "BOS", Division.ATLANTIC, Conference.EASTERN);
        lakers = new Team(2, "Los Angeles Lakers", "LAL", Division.PACIFIC, Conference.WESTERN);
        warriors = new Team(3, "Golden State Warriors", "GSW", Division.PACIFIC, Conference.WESTERN);
        
        entityManager.persist(celtics);
        entityManager.persist(lakers);
        entityManager.persist(warriors);
        entityManager.flush();
        
        testDate = LocalDate.of(2025, 10, 24);
    }

    @Test
    void testFindBySnapshotDate() {
        StandingsSnapshot snapshot1 = new StandingsSnapshot(testDate, celtics, 3, 0, new BigDecimal("1.000"), 1, 1);
        StandingsSnapshot snapshot2 = new StandingsSnapshot(testDate, lakers, 2, 1, new BigDecimal("0.667"), 1, 2);
        
        entityManager.persist(snapshot1);
        entityManager.persist(snapshot2);
        entityManager.flush();
        
        List<StandingsSnapshot> result = standingsSnapshotRepository.findBySnapshotDate(testDate);
        
        assertEquals(2, result.size());
    }

    @Test
    void testExistsBySnapshotDate() {
        StandingsSnapshot snapshot = new StandingsSnapshot(testDate, celtics, 3, 0, new BigDecimal("1.000"), 1, 1);
        entityManager.persist(snapshot);
        entityManager.flush();
        
        assertTrue(standingsSnapshotRepository.existsBySnapshotDate(testDate));
        assertFalse(standingsSnapshotRepository.existsBySnapshotDate(LocalDate.of(2025, 10, 25)));
    }

    @Test
    void testFindBySnapshotDateAndTeam_Division() {
        StandingsSnapshot snapshot1 = new StandingsSnapshot(testDate, celtics, 3, 0, new BigDecimal("1.000"), 1, 1);
        StandingsSnapshot snapshot2 = new StandingsSnapshot(testDate, lakers, 2, 1, new BigDecimal("0.667"), 1, 2);
        StandingsSnapshot snapshot3 = new StandingsSnapshot(testDate, warriors, 1, 2, new BigDecimal("0.333"), 2, 3);
        
        entityManager.persist(snapshot1);
        entityManager.persist(snapshot2);
        entityManager.persist(snapshot3);
        entityManager.flush();
        
        List<StandingsSnapshot> atlanticResult = standingsSnapshotRepository
                .findBySnapshotDateAndTeam_Division(testDate, Division.ATLANTIC);
        List<StandingsSnapshot> pacificResult = standingsSnapshotRepository
                .findBySnapshotDateAndTeam_Division(testDate, Division.PACIFIC);
        
        assertEquals(1, atlanticResult.size());
        assertEquals("Boston Celtics", atlanticResult.get(0).getTeam().getTeamName());
        
        assertEquals(2, pacificResult.size());
    }

    @Test
    void testFindBySnapshotDateAndTeam_Conference() {
        StandingsSnapshot snapshot1 = new StandingsSnapshot(testDate, celtics, 3, 0, new BigDecimal("1.000"), 1, 1);
        StandingsSnapshot snapshot2 = new StandingsSnapshot(testDate, lakers, 2, 1, new BigDecimal("0.667"), 1, 2);
        StandingsSnapshot snapshot3 = new StandingsSnapshot(testDate, warriors, 1, 2, new BigDecimal("0.333"), 2, 3);
        
        entityManager.persist(snapshot1);
        entityManager.persist(snapshot2);
        entityManager.persist(snapshot3);
        entityManager.flush();
        
        List<StandingsSnapshot> easternResult = standingsSnapshotRepository
                .findBySnapshotDateAndTeam_Conference(testDate, Conference.EASTERN);
        List<StandingsSnapshot> westernResult = standingsSnapshotRepository
                .findBySnapshotDateAndTeam_Conference(testDate, Conference.WESTERN);
        
        assertEquals(1, easternResult.size());
        assertEquals("Boston Celtics", easternResult.get(0).getTeam().getTeamName());
        
        assertEquals(2, westernResult.size());
    }
}
