package com.nba.standings.repository;

import com.nba.standings.model.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Game entity.
 * Provides query methods for retrieving games by date ranges and checking game existence.
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    
    /**
     * Find all games within a date range (inclusive).
     * 
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return list of games within the specified date range
     */
    List<Game> findByGameDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find all games on or before a specific date.
     * 
     * @param date the cutoff date
     * @return list of games on or before the specified date
     */
    List<Game> findByGameDateLessThanEqual(LocalDate date);
    
    /**
     * Check if a game with the specified NBA game ID already exists.
     * 
     * @param nbaGameId the NBA API game ID
     * @return true if a game with this ID exists, false otherwise
     */
    boolean existsByNbaGameId(Long nbaGameId);
}
