package com.nl.wowapi.boble.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl.wowapi.boble.model.ZoneRankings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    // Inject your client credentials from properties
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
     * Retrieve an access token from Warcraft Logs using the client credentials flow.
     * This uses a form-encoded POST request, as required by the API.
     *
     * @return the access token as a String.
     */
    private String getWarcraftLogsToken() {
        if (token != null) {
            return token;
        }

        // Prepare form data with the required grant_type parameter.
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpHeaders headers = new HttpHeaders();
        // Set the content type to application/x-www-form-urlencoded
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Prepare Basic Auth with clientId and clientSecret.
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

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
     * The response from the GraphQL query is deserialized into a ZoneRankings POJO,
     * allowing for easier access to its fields.
     *
     * @param characterName The name of the character.
     * @param serverSlug    The slug of the server (realm) the character is on.
     * @param region        The region (e.g., "eu").
     * @return the best average score as a double.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public double getBestPerformanceAvg(String characterName, String serverSlug, String region) {
        // Construct the GraphQL query without sub-selections for zoneRankings.
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
        // Use the previously obtained access token.
        headers.set("Authorization", "Bearer " + getWarcraftLogsToken());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Execute the POST request against the GraphQL endpoint.
            ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, JsonNode.class);
            System.out.println("WarcraftLogs Response: " + response.getBody());

            // Navigate to the zoneRankings field.
            JsonNode data = response.getBody().path("data");
            JsonNode character = data.path("characterData").path("character");
            JsonNode zoneRankings = character.path("zoneRankings");

            // If zoneRankings is returned as a JSON string, parse it into a ZoneRankings object.
            if (zoneRankings.isTextual()) {
                ZoneRankings zoneRankingsObj = objectMapper.readValue(zoneRankings.asText(), ZoneRankings.class);
                return zoneRankingsObj.getBestPerformanceAverage();
            } else if (zoneRankings.isObject()) {
                ZoneRankings zoneRankingsObj = objectMapper.treeToValue(zoneRankings, ZoneRankings.class);
                return zoneRankingsObj.getBestPerformanceAverage();
            } else if (zoneRankings.isArray() && zoneRankings.size() == 0) {
                // No data available—return a default value.
                return 0.0;
            } else {
                // Unexpected format.
                throw new RuntimeException("Unexpected format for zoneRankings: " + zoneRankings.toString());
            }


        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching Warcraft Logs data: " + e.getStatusCode() +
                    " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch best performance average score from Warcraft Logs API", e);
        }
    }
}
