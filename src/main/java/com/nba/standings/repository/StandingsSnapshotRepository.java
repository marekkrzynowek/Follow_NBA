package com.nba.standings.repository;

import com.nba.standings.model.entity.StandingsSnapshot;
import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for StandingsSnapshot entity.
 * Provides query methods for retrieving cached standings snapshots by date, division, and conference.
 */
@Repository
public interface StandingsSnapshotRepository extends JpaRepository<StandingsSnapshot, Long> {
    
    /**
     * Find all standings snapshots for a specific date.
     * 
     * @param snapshotDate the date to retrieve standings for
     * @return list of standings snapshots for the specified date
     */
    List<StandingsSnapshot> findBySnapshotDate(LocalDate snapshotDate);
    
    /**
     * Check if standings snapshots exist for a specific date.
     * Used to determine if standings need to be calculated or can be retrieved from cache.
     * 
     * @param snapshotDate the date to check
     * @return true if standings exist for this date, false otherwise
     */
    boolean existsBySnapshotDate(LocalDate snapshotDate);
    
    /**
     * Find all standings snapshots for a specific date filtered by division.
     * Uses Spring Data JPA nested property syntax (team.division) to query through the relationship.
     * 
     * @param snapshotDate the date to retrieve standings for
     * @param division the division to filter by
     * @return list of standings snapshots for the specified date and division
     */
    List<StandingsSnapshot> findBySnapshotDateAndTeam_Division(LocalDate snapshotDate, Division division);
    
    /**
     * Find all standings snapshots for a specific date filtered by conference.
     * Uses Spring Data JPA nested property syntax (team.conference) to query through the relationship.
     * 
     * @param snapshotDate the date to retrieve standings for
     * @param conference the conference to filter by
     * @return list of standings snapshots for the specified date and conference
     */
    List<StandingsSnapshot> findBySnapshotDateAndTeam_Conference(LocalDate snapshotDate, Conference conference);
}
