package com.nl.wowapi.boble;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class WarcraftLogsApiService {

    // GraphQL API endpoint for Warcraft Logs
    private final String apiUrl = "https://www.warcraftlogs.com/api/v2/client";

    // Token endpoint URL
    private final String tokenUrl = "https://www.warcraftlogs.com/oauth/token";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // Inject your client credentials from application.properties
    @Value("${warcraftlogs.client.id}")
    private String clientId;

    @Value("${warcraftlogs.client.secret}")
    private String clientSecret;

    // Cache the token (in a real application you would want to check its expiry)
    private String token;

    public WarcraftLogsApiService() {
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    /**
     * Retrieve an access token from Warcraft Logs using the client credentials grant.
     *
     * @return the access token as a String.
     */
    private String getWarcraftLogsToken() {
        if (token != null) {
            return token;
        }

        // Prepare request body: grant_type=client_credentials
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");

        // Prepare headers with Basic Auth (Base64 encoded "clientId:clientSecret")
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBody = response.getBody();
            token = responseBody.path("access_token").asText();
            return token;
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching Warcraft Logs token: " + e.getStatusCode() +
                    " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Warcraft Logs token", e);
        }
    }

    /**
     * Fetches the best performance average score for a character from the Warcraft Logs API.
     *
     * @param characterName The name of the character.
     * @param serverSlug    The slug of the server (realm) the character is on.
     * @param region        The region (e.g., "eu").
     * @return the best average score as a double.
     */
    public double getBestPerformanceAvg(String characterName, String serverSlug, String region) {
        // Construct the query without variable declarations or sub-selections for zoneRankings
        String query = "query CharacterZoneRanking { " +
                "characterData { " +
                "  character(name: \"" + characterName + "\", serverSlug: \"" + serverSlug + "\", serverRegion: \"" + region + "\") { " +
                "    zoneRankings(zoneID: 42) " +
                "  } " +
                "} " +
                "}";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getWarcraftLogsToken());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, JsonNode.class);
            System.out.println("WarcraftLogs Response: " + response.getBody());

            // Navigate to the zoneRankings field
            JsonNode data = response.getBody().path("data");
            JsonNode character = data.path("characterData").path("character");
            JsonNode zoneRankings = character.path("zoneRankings");

            // Since zoneRankings is a JSON scalar, it might be returned as a text value.
            // If it is textual, we need to parse it as JSON.
            JsonNode zoneRankingsNode;
            if (zoneRankings.isTextual()) {
                zoneRankingsNode = objectMapper.readTree(zoneRankings.asText());
            } else {
                zoneRankingsNode = zoneRankings;
            }

            // Extract the bestPerformanceAverage from the parsed JSON
            return zoneRankingsNode.path("bestPerformanceAverage").asDouble();
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching Warcraft Logs data: " + e.getStatusCode() +
                    " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch best performance average score from Warcraft Logs API", e);
        }
    }


}
