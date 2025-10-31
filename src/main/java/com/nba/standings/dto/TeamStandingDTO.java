package com.nba.standings.dto;

import java.math.BigDecimal;

public class TeamStandingDTO {
    private Integer rank;
    private String teamName;
    private Integer wins;
    private Integer losses;
    private BigDecimal winPct;

    public TeamStandingDTO() {
    }

    public TeamStandingDTO(Integer rank, String teamName, Integer wins, Integer losses, BigDecimal winPct) {
        this.rank = rank;
        this.teamName = teamName;
        this.wins = wins;
        this.losses = losses;
        this.winPct = winPct;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getLosses() {
        return losses;
    }

    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public BigDecimal getWinPct() {
        return winPct;
    }

    public void setWinPct(BigDecimal winPct) {
        this.winPct = winPct;
    }
}
