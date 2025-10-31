package com.nba.standings.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a cached snapshot of team standings for a specific date.
 * This is a read-only entity - once a snapshot is calculated and stored, it never changes.
 * Stores pre-calculated standings to avoid recalculating on every request.
 * Each snapshot represents a team's standing as of a particular date.
 */
@Entity
@Table(name = "standings_snapshots", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_snapshot_date_team", columnNames = {"snapshot_date", "team_id"})
    },
    indexes = {
        @Index(name = "idx_snapshot_date", columnList = "snapshot_date"),
        @Index(name = "idx_team_date", columnList = "team_id, snapshot_date")
    }
)
@Immutable
public class StandingsSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @Column(nullable = false)
    private Integer wins;
    
    @Column(nullable = false)
    private Integer losses;
    
    @Column(name = "win_pct", nullable = false, precision = 5, scale = 3)
    private BigDecimal winPct;
    
    @Column(name = "division_rank", nullable = false)
    private Integer divisionRank;
    
    @Column(name = "conference_rank", nullable = false)
    private Integer conferenceRank;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public StandingsSnapshot() {
    }
    
    public StandingsSnapshot(LocalDate snapshotDate, Team team, Integer wins, Integer losses, 
                            BigDecimal winPct, Integer divisionRank, Integer conferenceRank) {
        this.snapshotDate = snapshotDate;
        this.team = team;
        this.wins = wins;
        this.losses = losses;
        this.winPct = winPct;
        this.divisionRank = divisionRank;
        this.conferenceRank = conferenceRank;
    }
    
    // Static factory method
    /**
     * Creates a StandingsSnapshot from a TeamStanding calculation result.
     * This factory method encapsulates the logic of converting calculated standings
     * into a persistent snapshot entity.
     * 
     * @param snapshotDate the date of the snapshot
     * @param standing the calculated team standing
     * @return a new StandingsSnapshot instance
     */
    public static StandingsSnapshot fromTeamStanding(LocalDate snapshotDate, 
                                                     com.nba.standings.service.StandingsCalculator.TeamStanding standing) {
        return new StandingsSnapshot(
                snapshotDate,
                standing.getTeam(),
                standing.getWins(),
                standing.getLosses(),
                standing.getWinPct(),
                standing.getDivisionRank(),
                standing.getConferenceRank()
        );
    }
    
    // Getters only - this is a read-only entity
    public Long getId() {
        return id;
    }
    
    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }
    
    public Team getTeam() {
        return team;
    }
    
    public Integer getWins() {
        return wins;
    }
    
    public Integer getLosses() {
        return losses;
    }
    
    public BigDecimal getWinPct() {
        return winPct;
    }
    
    public Integer getDivisionRank() {
        return divisionRank;
    }
    
    public Integer getConferenceRank() {
        return conferenceRank;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public String toString() {
        return "StandingsSnapshot{" +
                "id=" + id +
                ", snapshotDate=" + snapshotDate +
                ", team=" + (team != null ? team.getTeamName() : "null") +
                ", wins=" + wins +
                ", losses=" + losses +
                ", winPct=" + winPct +
                ", divisionRank=" + divisionRank +
                ", conferenceRank=" + conferenceRank +
                '}';
    }
}
