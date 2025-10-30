package com.nba.standings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for NBA API pagination metadata
 */
public class NBAMetaDTO {
    
    @JsonProperty("next_cursor")
    private Integer nextCursor;
    
    @JsonProperty("per_page")
    private Integer perPage;
    
    // Constructors
    public NBAMetaDTO() {
    }
    
    // Getters and Setters
    public Integer getNextCursor() {
        return nextCursor;
    }
    
    public void setNextCursor(Integer nextCursor) {
        this.nextCursor = nextCursor;
    }
    
    public Integer getPerPage() {
        return perPage;
    }
    
    public void setPerPage(Integer perPage) {
        this.perPage = perPage;
    }
}
