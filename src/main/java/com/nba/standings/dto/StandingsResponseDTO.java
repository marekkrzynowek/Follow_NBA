package com.nba.standings.dto;

import com.nba.standings.model.enums.Conference;
import com.nba.standings.model.enums.Division;
import com.nba.standings.model.enums.GroupBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO for standings API response.
 * The standings map uses String keys that represent enum names:
 * - For division grouping: Division enum names (ATLANTIC, CENTRAL, etc.)
 * - For conference grouping: Conference enum names (EASTERN, WESTERN)
 */
public class StandingsResponseDTO {
    private LocalDate date;
    private GroupBy groupBy;
    
    /**
     * Map of group name to list of team standings.
     * Key is the enum name as a String (e.g., "ATLANTIC", "EASTERN")
     */
    private Map<String, List<TeamStandingDTO>> standings;

    public StandingsResponseDTO() {
    }

    public StandingsResponseDTO(LocalDate date, GroupBy groupBy, Map<String, List<TeamStandingDTO>> standings) {
        this.date = date;
        this.groupBy = groupBy;
        this.standings = standings;
    }

    /**
     * Type-safe factory method for division-grouped standings.
     * Converts Division enum keys to String keys for JSON serialization.
     */
    public static StandingsResponseDTO forDivisions(LocalDate date, Map<Division, List<TeamStandingDTO>> divisionStandings) {
        Map<String, List<TeamStandingDTO>> standings = divisionStandings.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        Map.Entry::getValue
                ));
        return new StandingsResponseDTO(date, GroupBy.DIVISION, standings);
    }

    /**
     * Type-safe factory method for conference-grouped standings.
     * Converts Conference enum keys to String keys for JSON serialization.
     */
    public static StandingsResponseDTO forConferences(LocalDate date, Map<Conference, List<TeamStandingDTO>> conferenceStandings) {
        Map<String, List<TeamStandingDTO>> standings = conferenceStandings.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        Map.Entry::getValue
                ));
        return new StandingsResponseDTO(date, GroupBy.CONFERENCE, standings);
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public GroupBy getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(GroupBy groupBy) {
        this.groupBy = groupBy;
    }

    public Map<String, List<TeamStandingDTO>> getStandings() {
        return standings;
    }

    public void setStandings(Map<String, List<TeamStandingDTO>> standings) {
        this.standings = standings;
    }
}
