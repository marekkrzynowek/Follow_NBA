package com.nba.standings.model.entity;

import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

/**
 * Entity representing an NBA team.
 * This is a read-only entity - teams are seeded via Flyway migration and never modified.
 */
@Entity
@Table(name = "teams")
@Immutable
public class Team {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nba_team_id", nullable = false, unique = true)
    private Integer nbaTeamId;
    
    @Column(name = "team_name", nullable = false, unique = true, length = 100)
    private String teamName;
    
    @Column(nullable = false, unique = true, length = 3)
    private String abbreviation;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Division division;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Conference conference;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public Team() {
    }
    
    public Team(Integer nbaTeamId, String teamName, String abbreviation, Division division, Conference conference) {
        this.nbaTeamId = nbaTeamId;
        this.teamName = teamName;
        this.abbreviation = abbreviation;
        this.division = division;
        this.conference = conference;
    }
    
    // Getters only - this is a read-only entity
    public Long getId() {
        return id;
    }
    
    public Integer getNbaTeamId() {
        return nbaTeamId;
    }
    
    public String getTeamName() {
        return teamName;
    }
    
    public String getAbbreviation() {
        return abbreviation;
    }
    
    public Division getDivision() {
        return division;
    }
    
    public Conference getConference() {
        return conference;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", nbaTeamId=" + nbaTeamId +
                ", teamName='" + teamName + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                ", division=" + division +
                ", conference=" + conference +
                '}';
    }
}
