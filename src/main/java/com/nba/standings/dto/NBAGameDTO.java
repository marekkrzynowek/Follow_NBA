package com.nba.standings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/**
 * DTO for NBA API game response
 */
public class NBAGameDTO {
    
    public static final String STATUS_FINAL = "Final";
    
    private Long id;
    
    private LocalDate date;
    
    @JsonProperty("home_team")
    private NBATeamDTO homeTeam;
    
    @JsonProperty("visitor_team")
    private NBATeamDTO visitorTeam;
    
    @JsonProperty("home_team_score")
    private Integer homeTeamScore;
    
    @JsonProperty("visitor_team_score")
    private Integer visitorTeamScore;
    
    private String status;
    
    // Constructors
    public NBAGameDTO() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public NBATeamDTO getHomeTeam() {
        return homeTeam;
    }
    
    public void setHomeTeam(NBATeamDTO homeTeam) {
        this.homeTeam = homeTeam;
    }
    
    public NBATeamDTO getVisitorTeam() {
        return visitorTeam;
    }
    
    public void setVisitorTeam(NBATeamDTO visitorTeam) {
        this.visitorTeam = visitorTeam;
    }
    
    public Integer getHomeTeamScore() {
        return homeTeamScore;
    }
    
    public void setHomeTeamScore(Integer homeTeamScore) {
        this.homeTeamScore = homeTeamScore;
    }
    
    public Integer getVisitorTeamScore() {
        return visitorTeamScore;
    }
    
    public void setVisitorTeamScore(Integer visitorTeamScore) {
        this.visitorTeamScore = visitorTeamScore;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Checks if the game has finished (status is "Final").
     * 
     * @return true if the game is final, false otherwise
     */
    public boolean isFinal() {
        return STATUS_FINAL.equals(status);
    }
}
