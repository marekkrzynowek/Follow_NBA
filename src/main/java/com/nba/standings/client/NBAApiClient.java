package com.nba.standings.client;

import com.nba.standings.dto.NBAGamesResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Client for interacting with the NBA API
 */
@Component
public class NBAApiClient {
    
    private final WebClient webClient;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int DEFAULT_PER_PAGE = 100;
    
    public NBAApiClient(WebClient nbaWebClient) {
        this.webClient = nbaWebClient;
    }
    
    /**
     * Fetches games from the NBA API for a specific date range
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param cursor Cursor for pagination (null for first page)
     * @param perPage Number of results per page
     * @return Mono containing the games response
     */
    public Mono<NBAGamesResponse> getGames(LocalDate startDate, LocalDate endDate, Integer cursor, int perPage) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/games")
                            .queryParam("start_date", startDate.format(DATE_FORMATTER))
                            .queryParam("end_date", endDate.format(DATE_FORMATTER))
                            .queryParam("per_page", perPage)
                            .queryParam("postseason", false);

                    if (cursor != null) {
                        builder.queryParam("cursor", cursor);
                    }
                    
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(NBAGamesResponse.class);
    }
    
    /**
     * Fetches all games for a date range by handling cursor-based pagination
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Mono containing all games across all pages
     */
    public Mono<NBAGamesResponse> getAllGames(LocalDate startDate, LocalDate endDate) {
        return getGames(startDate, endDate, null, DEFAULT_PER_PAGE)
                .expand(response -> {
                    if (response.getMeta() != null && 
                        response.getMeta().getNextCursor() != null) {
                        return getGames(startDate, endDate, 
                                response.getMeta().getNextCursor(), DEFAULT_PER_PAGE);
                    }
                    return Mono.empty();
                })
                .reduce(new NBAGamesResponse(), (accumulated, current) -> {
                    accumulated.getData().addAll(current.getData());
                    accumulated.setMeta(current.getMeta());
                    return accumulated;
                });
    }
}
