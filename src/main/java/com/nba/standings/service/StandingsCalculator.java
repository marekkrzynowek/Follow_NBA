package com.nba.standings.service;

import com.nba.standings.model.entity.Game;
import com.nba.standings.model.entity.Team;
import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service responsible for calculating NBA standings from game results.
 * Computes win-loss records, winning percentages, and rankings.
 * 
 * The main entry point is calculateStandings() which ensures win-loss records
 * and winning percentages are calculated atomically to maintain consistency.
 */
@Service
public class StandingsCalculator {

    private static final Logger logger = LoggerFactory.getLogger(StandingsCalculator.class);

    /**
     * Internal class to hold calculated standings data for a team.
     * Implements Comparable to define natural ordering by win percentage (descending),
     * then by wins (descending), then by team name (alphabetically).
     */
    public static class TeamStanding implements Comparable<TeamStanding> {
        private final Team team;
        private int wins;
        private int losses;
        private BigDecimal winPct;
        private Integer divisionRank;
        private Integer conferenceRank;

        public TeamStanding(Team team) {
            this.team = team;
            this.wins = 0;
            this.losses = 0;
            this.winPct = BigDecimal.ZERO;
        }

        // Getters
        public Team getTeam() {
            return team;
        }

        public int getWins() {
            return wins;
        }

        public int getLosses() {
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

        // Setters
        public void setWins(int wins) {
            this.wins = wins;
        }

        public void setLosses(int losses) {
            this.losses = losses;
        }

        public void calculateWinPct() {
            int totalGames = this.wins + this.losses;
            if (totalGames > 0) {
                this.winPct = BigDecimal.valueOf(this.wins)
                        .divide(BigDecimal.valueOf(totalGames), 3, RoundingMode.HALF_UP);
            } else {
                this.winPct = BigDecimal.ZERO;
            }
        }

        public void setDivisionRank(Integer divisionRank) {
            this.divisionRank = divisionRank;
        }

        public void setConferenceRank(Integer conferenceRank) {
            this.conferenceRank = conferenceRank;
        }

        public void incrementWins() {
            this.wins++;
        }

        public void incrementLosses() {
            this.losses++;
        }

        @Override
        public int compareTo(TeamStanding other) {
            // Sort by win percentage descending
            int pctCompare = other.winPct.compareTo(this.winPct);
            if (pctCompare != 0) {
                return pctCompare;
            }
            // If tied, sort by wins descending
            int winsCompare = Integer.compare(other.wins, this.wins);
            if (winsCompare != 0) {
                return winsCompare;
            }
            // If still tied, sort by team name alphabetically
            return this.team.getTeamName().compareTo(other.team.getTeamName());
        }
    }

    /**
     * Calculate complete standings from games for all teams.
     * This method ensures win-loss records and winning percentages are calculated together
     * to maintain data consistency.
     * 
     * @param games List of games to process
     * @param allTeams List of all teams in the league
     * @return Map of team ID to TeamStanding with complete calculated data
     */
    public Map<Long, TeamStanding> calculateStandings(List<Game> games, List<Team> allTeams) {
        // Initialize standings for all teams
        Map<Long, TeamStanding> standings = new HashMap<>();
        for (Team team : allTeams) {
            standings.put(team.getId(), new TeamStanding(team));
        }

        // Process each game to calculate win-loss records
        for (Game game : games) {
            Long homeTeamId = game.getHomeTeam().getId();
            Long awayTeamId = game.getAwayTeam().getId();

            TeamStanding homeStanding = standings.get(homeTeamId);
            TeamStanding awayStanding = standings.get(awayTeamId);

            if (homeStanding == null || awayStanding == null) {
                logger.warn("Game {} has missing team data - Home Team ID: {}, Away Team ID: {}. Skipping game.",
                        game.getNbaGameId(), homeTeamId, awayTeamId);
                continue;
            }

            if (game.getHomeScore() > game.getAwayScore()) {
                // Home team wins
                homeStanding.incrementWins();
                awayStanding.incrementLosses();
            } else {
                // Away team wins
                awayStanding.incrementWins();
                homeStanding.incrementLosses();
            }
        }

        // Calculate winning percentages for all teams
        for (TeamStanding standing : standings.values()) {
            standing.calculateWinPct();
        }

        return standings;
    }

    /**
     * Group teams by division, sort them, and assign division ranks.
     * 
     * @param standings Map of all team standings
     * @return Map of Division to sorted list of TeamStanding with ranks assigned
     */
    public Map<Division, List<TeamStanding>> groupByDivision(Map<Long, TeamStanding> standings) {
        // Group teams by division
        Map<Division, List<TeamStanding>> divisionStandings = new HashMap<>();
        for (TeamStanding standing : standings.values()) {
            Division division = standing.getTeam().getDivision();
            divisionStandings.computeIfAbsent(division, k -> new ArrayList<>()).add(standing);
        }

        // Sort teams within each division and assign ranks
        for (List<TeamStanding> divisionTeams : divisionStandings.values()) {
            Collections.sort(divisionTeams);
            for (int i = 0; i < divisionTeams.size(); i++) {
                divisionTeams.get(i).setDivisionRank(i + 1);
            }
        }

        return divisionStandings;
    }

    /**
     * Group teams by conference, sort them, and assign conference ranks.
     * 
     * @param standings Map of all team standings
     * @return Map of Conference to sorted list of TeamStanding with ranks assigned
     */
    public Map<Conference, List<TeamStanding>> groupByConference(Map<Long, TeamStanding> standings) {
        // Group teams by conference
        Map<Conference, List<TeamStanding>> conferenceStandings = new HashMap<>();
        for (TeamStanding standing : standings.values()) {
            Conference conference = standing.getTeam().getConference();
            conferenceStandings.computeIfAbsent(conference, k -> new ArrayList<>()).add(standing);
        }

        // Sort teams within each conference and assign ranks
        for (List<TeamStanding> conferenceTeams : conferenceStandings.values()) {
            Collections.sort(conferenceTeams);
            for (int i = 0; i < conferenceTeams.size(); i++) {
                conferenceTeams.get(i).setConferenceRank(i + 1);
            }
        }

        return conferenceStandings;
    }
}
