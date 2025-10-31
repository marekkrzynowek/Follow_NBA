package com.nba.standings.repository;

import com.nba.standings.model.entity.Team;
import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class TeamRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TeamRepository teamRepository;

    private Team celtics;
    private Team lakers;
    private Team warriors;

    @BeforeEach
    void setUp() {
        celtics = new Team(1, "Boston Celtics", "BOS", Division.ATLANTIC, Conference.EASTERN);
        lakers = new Team(2, "Los Angeles Lakers", "LAL", Division.PACIFIC, Conference.WESTERN);
        warriors = new Team(3, "Golden State Warriors", "GSW", Division.PACIFIC, Conference.WESTERN);
        
        entityManager.persist(celtics);
        entityManager.persist(lakers);
        entityManager.persist(warriors);
        entityManager.flush();
    }

    @Test
    void testFindByDivision() {
        List<Team> pacificTeams = teamRepository.findByDivision(Division.PACIFIC);
        
        assertEquals(2, pacificTeams.size());
        assertTrue(pacificTeams.stream().anyMatch(t -> t.getTeamName().equals("Los Angeles Lakers")));
        assertTrue(pacificTeams.stream().anyMatch(t -> t.getTeamName().equals("Golden State Warriors")));
    }

    @Test
    void testFindByConference() {
        List<Team> westernTeams = teamRepository.findByConference(Conference.WESTERN);
        
        assertEquals(2, westernTeams.size());
        assertTrue(westernTeams.stream().allMatch(t -> t.getConference() == Conference.WESTERN));
    }

    @Test
    void testFindByNbaTeamId() {
        Optional<Team> result = teamRepository.findByNbaTeamId(1);
        
        assertTrue(result.isPresent());
        assertEquals("Boston Celtics", result.get().getTeamName());
    }

    @Test
    void testFindByNbaTeamId_NotFound() {
        Optional<Team> result = teamRepository.findByNbaTeamId(999);
        
        assertFalse(result.isPresent());
    }
}
