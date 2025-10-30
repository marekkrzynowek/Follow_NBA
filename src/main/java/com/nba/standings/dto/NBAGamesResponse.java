package com.nba.standings.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for NBA API games list response
 */
public class NBAGamesResponse {
    
    private List<NBAGameDTO> data = new ArrayList<>();
    
    private NBAMetaDTO meta;
    
    // Constructors
    public NBAGamesResponse() {
    }
    
    // Getters and Setters
    public List<NBAGameDTO> getData() {
        return data;
    }
    
    public void setData(List<NBAGameDTO> data) {
        this.data = data;
    }
    
    public NBAMetaDTO getMeta() {
        return meta;
    }
    
    public void setMeta(NBAMetaDTO meta) {
        this.meta = meta;
    }
}
