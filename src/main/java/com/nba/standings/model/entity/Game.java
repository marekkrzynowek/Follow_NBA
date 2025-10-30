package com.nba.standings.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an NBA game with final scores.
 * This is a read-only entity - once a game is fetched and stored, it never changes.
 * Stores game results for standings calculation.
 */
@Entity
@Table(name = "games", indexes = {
    @Index(name = "idx_game_date", columnList = "game_date"),
    @Index(name = "idx_home_team", columnList = "home_team_id"),
    @Index(name = "idx_away_team", columnList = "away_team_id")
})
@Immutable
public class Game {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nba_game_id", nullable = false, unique = true)
    private Long nbaGameId;
    
    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;
    
    @Column(name = "home_score", nullable = false)
    private Integer homeScore;
    
    @Column(name = "away_score", nullable = false)
    private Integer awayScore;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public Game() {
    }
    
    public Game(Long nbaGameId, LocalDate gameDate, Team homeTeam, Team awayTeam, 
                Integer homeScore, Integer awayScore) {
        this.nbaGameId = nbaGameId;
        this.gameDate = gameDate;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
    
    // Getters only - this is a read-only entity
    public Long getId() {
        return id;
    }
    
    public Long getNbaGameId() {
        return nbaGameId;
    }
    
    public LocalDate getGameDate() {
        return gameDate;
    }
    
    public Team getHomeTeam() {
        return homeTeam;
    }
    
    public Team getAwayTeam() {
        return awayTeam;
    }
    
    public Integer getHomeScore() {
        return homeScore;
    }
    
    public Integer getAwayScore() {
        return awayScore;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public String toString() {
        return "Game{" +
                "id=" + id +
                ", nbaGameId=" + nbaGameId +
                ", gameDate=" + gameDate +
                ", homeTeam=" + (homeTeam != null ? homeTeam.getTeamName() : "null") +
                ", awayTeam=" + (awayTeam != null ? awayTeam.getTeamName() : "null") +
                ", homeScore=" + homeScore +
                ", awayScore=" + awayScore +
                '}';
    }
}
