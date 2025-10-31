package com.nba.standings.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.standings.dto.NBAGameDTO;
import com.nba.standings.dto.NBAGamesResponse;
import com.nba.standings.dto.NBAMetaDTO;
import com.nba.standings.dto.NBATeamDTO;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for NBAApiClient using WireMock to simulate NBA API responses.
 * Tests pagination, error handling, and data transformation.
 */
class NBAApiClientIntegrationTest {

    private WireMockServer wireMockServer;
    private NBAApiClient nbaApiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8089")
                .build();

        nbaApiClient = new NBAApiClient(webClient);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testGetGames_SinglePage_Success() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 10, 22);
        LocalDate endDate = LocalDate.of(2024, 10, 22);

        NBAGamesResponse mockResponse = createMockResponse(2, null);
        String responseJson = objectMapper.writeValueAsString(mockResponse);

        stubFor(get(urlPathEqualTo("/games"))
                .withQueryParam("start_date", equalTo("2024-10-22"))
                .withQueryParam("end_date", equalTo("2024-10-22"))
                .withQueryParam("per_page", equalTo("100"))
                .withQueryParam("postseason", equalTo("false"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        Mono<NBAGamesResponse> result = nbaApiClient.getGames(startDate, endDate, null, 100);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(2, response.getData().size());
                })
                .verifyComplete();

        verify(exactly(1), getRequestedFor(urlPathEqualTo("/games")));
    }

    @Test
    void testGetGames_WithCursor_Success() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 10, 22);
        LocalDate endDate = LocalDate.of(2024, 10, 22);
        Integer cursor = 50;

        NBAGamesResponse mockResponse = createMockResponse(2, null);
        String responseJson = objectMapper.writeValueAsString(mockResponse);

        stubFor(get(urlPathEqualTo("/games"))
                .withQueryParam("start_date", equalTo("2024-10-22"))
                .withQueryParam("end_date", equalTo("2024-10-22"))
                .withQueryParam("per_page", equalTo("100"))
                .withQueryParam("cursor", equalTo("50"))
                .withQueryParam("postseason", equalTo("false"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        Mono<NBAGamesResponse> result = nbaApiClient.getGames(startDate, endDate, cursor, 100);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(2, response.getData().size());
                })
                .verifyComplete();

        verify(exactly(1), getRequestedFor(urlPathEqualTo("/games"))
                .withQueryParam("cursor", equalTo("50")));
    }

    @Test
    void testGetAllGames_MultiplePagesWithPagination() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 10, 22);
        LocalDate endDate = LocalDate.of(2024, 10, 24);

        // First page with next cursor
        NBAGamesResponse page1 = createMockResponse(3, 100);
        String page1Json = objectMapper.writeValueAsString(page1);

        // Second page with next cursor
        NBAGamesResponse page2 = createMockResponse(3, 200);
        String page2Json = objectMapper.writeValueAsString(page2);

        // Third page without next cursor (last page)
        NBAGamesResponse page3 = createMockResponse(2, null);
        String page3Json = objectMapper.writeValueAsString(page3);

        // Stub first page (without cursor parameter)
        stubFor(get(urlPathEqualTo("/games"))
                .withQueryParam("start_date", equalTo("2024-10-22"))
                .withQueryParam("end_date", equalTo("2024-10-24"))
                .withQueryParam("per_page", equalTo("100"))
                .withQueryParam("postseason", equalTo("false"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(page1Json)));

        // Stub second page
        stubFor(get(urlPathEqualTo("/games"))
                .withQueryParam("start_date", equalTo("2024-10-22"))
                .withQueryParam("end_date", equalTo("2024-10-24"))
                .withQueryParam("per_page", equalTo("100"))
                .withQueryParam("cursor", equalTo("100"))
                .withQueryParam("postseason", equalTo("false"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(page2Json)));

        // Stub third page
        stubFor(get(urlPathEqualTo("/games"))
                .withQueryParam("start_date", equalTo("2024-10-22"))
                .withQueryParam("end_date", equalTo("2024-10-24"))
                .withQueryParam("per_page", equalTo("100"))
                .withQueryParam("cursor", equalTo("200"))
                .withQueryParam("postseason", equalTo("false"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(page3Json)));

        Mono<NBAGamesResponse> result = nbaApiClient.getAllGames(startDate, endDate);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(8, response.getData().size()); // 3 + 3 + 2
                    assertNull(response.getMeta().getNextCursor()); // Last page has no cursor
                })
                .verifyComplete();

        verify(exactly(3), getRequestedFor(urlPathEqualTo("/games")));
    }

    @Test
    void testGetAllGames_SinglePage_NoCursor() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 10, 22);
        LocalDate endDate = LocalDate.of(2024, 10, 22);

        NBAGamesResponse mockResponse = createMockResponse(5, null);
        String responseJson = objectMapper.writeValueAsString(mockResponse);

        stubFor(get(urlPathEqualTo("/games"))
                .withQueryParam("start_date", equalTo("2024-10-22"))
                .withQueryParam("end_date", equalTo("2024-10-22"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        Mono<NBAGamesResponse> result = nbaApiClient.getAllGames(startDate, endDate);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(5, response.getData().size());
                    assertNull(response.getMeta().getNextCursor());
                })
                .verifyComplete();

        verify(exactly(1), getRequestedFor(urlPathEqualTo("/games")));
    }

    @Test
    void testGetGames_ApiError_ReturnsError() {
        LocalDate startDate = LocalDate.of(2024, 10, 22);
        LocalDate endDate = LocalDate.of(2024, 10, 22);

        stubFor(get(urlPathEqualTo("/games"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        Mono<NBAGamesResponse> result = nbaApiClient.getGames(startDate, endDate, null, 100);

        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void testGetGames_RateLimitError_ReturnsError() {
        LocalDate startDate = LocalDate.of(2024, 10, 22);
        LocalDate endDate = LocalDate.of(2024, 10, 22);

        stubFor(get(urlPathEqualTo("/games"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withBody("Too Many Requests")));

        Mono<NBAGamesResponse> result = nbaApiClient.getGames(startDate, endDate, null, 100);

        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void testGetGames_EmptyResponse_Success() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 10, 22);
        LocalDate endDate = LocalDate.of(2024, 10, 22);

        NBAGamesResponse mockResponse = createMockResponse(0, null);
        String responseJson = objectMapper.writeValueAsString(mockResponse);

        stubFor(get(urlPathEqualTo("/games"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        Mono<NBAGamesResponse> result = nbaApiClient.getGames(startDate, endDate, null, 100);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(0, response.getData().size());
                })
                .verifyComplete();
    }

    /**
     * Helper method to create mock NBA API response with specified number of games
     */
    private NBAGamesResponse createMockResponse(int gameCount, Integer nextCursor) {
        NBAGamesResponse response = new NBAGamesResponse();
        List<NBAGameDTO> games = new ArrayList<>();

        for (int i = 0; i < gameCount; i++) {
            NBAGameDTO game = new NBAGameDTO();
            game.setId((long) (100 + i));
            game.setDate(LocalDate.of(2024, 10, 22));
            game.setStatus("Final");
            game.setHomeTeamScore(110 + i);
            game.setVisitorTeamScore(105 + i);

            NBATeamDTO homeTeam = new NBATeamDTO();
            homeTeam.setId((long) (1 + i));
            homeTeam.setName("Team " + (1 + i));
            homeTeam.setAbbreviation("T" + (1 + i));
            game.setHomeTeam(homeTeam);

            NBATeamDTO awayTeam = new NBATeamDTO();
            awayTeam.setId((long) (10 + i));
            awayTeam.setName("Team " + (10 + i));
            awayTeam.setAbbreviation("T" + (10 + i));
            game.setVisitorTeam(awayTeam);

            games.add(game);
        }

        response.setData(games);

        NBAMetaDTO meta = new NBAMetaDTO();
        meta.setNextCursor(nextCursor);
        response.setMeta(meta);

        return response;
    }
}
