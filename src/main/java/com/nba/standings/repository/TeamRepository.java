package com.nba.standings.repository;

import com.nba.standings.model.entity.Team;
import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Team entity.
 * Provides query methods for retrieving teams by division, conference, and NBA team ID.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    /**
     * Find all teams in a specific division.
     * 
     * @param division the division to filter by
     * @return list of teams in the specified division
     */
    List<Team> findByDivision(Division division);
    
    /**
     * Find all teams in a specific conference.
     * 
     * @param conference the conference to filter by
     * @return list of teams in the specified conference
     */
    List<Team> findByConference(Conference conference);
    
    /**
     * Find a team by its NBA API team ID.
     * 
     * @param nbaTeamId the NBA API team ID
     * @return Optional containing the team if found, empty otherwise
     */
    Optional<Team> findByNbaTeamId(Integer nbaTeamId);
}
