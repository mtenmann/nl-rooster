package com.nl.wowapi.boble.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl.wowapi.boble.model.ZoneRankings;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WarcraftLogsClient {

    // GraphQL API endpoint for Warcraft Logs
    private final String apiUrl = "https://www.warcraftlogs.com/api/v2/client";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WarcraftLogsTokenService tokenService;

    public WarcraftLogsClient(WarcraftLogsTokenService tokenService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.tokenService = tokenService;
    }

    /**
     * Normalizes a server slug by trimming, lowercasing and replacing spaces with hyphens.
     */
    private String normalizeServerSlug(String serverSlug) {
        return serverSlug.trim().toLowerCase().replace(" ", "-");
    }

    /**
     * Executes a GraphQL query to retrieve zoneRankings for a character,
     * logs the outgoing query and the raw JSON response, and deserializes the result into a ZoneRankings POJO.
     *
     * @param characterName the character's name.
     * @param serverSlug    the server slug.
     * @param region        the region (e.g., "eu").
     * @return a ZoneRankings object containing the rankings.
     * @throws Exception if mapping fails.
     */
    public ZoneRankings getZoneRankings(String characterName, String serverSlug, String region) throws Exception {
        // Normalize the server slug (e.g. "tarren mill" becomes "tarren-mill")
        String normalizedSlug = normalizeServerSlug(serverSlug);

        // Build the GraphQL query using the normalized server slug.
        String query = "query CharacterZoneRanking { " +
                "characterData { " +
                "  character(name: \"" + characterName + "\", serverSlug: \"" + normalizedSlug + "\", serverRegion: \"" + region + "\") { " +
                "    zoneRankings(zoneID: 42) " +
                "  } " +
                "} " +
                "}";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Get the access token from your token service
        headers.set("Authorization", "Bearer " + tokenService.getAccessToken());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Print the outgoing query for debugging.
        System.out.println("Sending GraphQL query for character " + characterName + ": " + query);

        ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, JsonNode.class);

        // Debug print: raw JSON response
        JsonNode root = response.getBody();
        System.out.println("Raw response for character " + characterName + ": " + root);

        JsonNode zoneRankingsNode = root.path("data")
                .path("characterData")
                .path("character")
                .path("zoneRankings");

        if (zoneRankingsNode.isMissingNode() || zoneRankingsNode.isNull()) {
            System.err.println("Warning: zoneRankings not found for character: " + characterName +
                    " (serverSlug: " + normalizedSlug + ")");
            // You might return an empty ZoneRankings object (ensure your model has a default constructor) or handle this case as needed.
            return new ZoneRankings();
        }

        // Debug print the raw zoneRankings JSON.
        System.out.println("Raw zoneRankings JSON for character " + characterName + ": " + zoneRankingsNode);

        ZoneRankings zoneRankings;
        if (zoneRankingsNode.isTextual()) {
            zoneRankings = objectMapper.readValue(zoneRankingsNode.asText(), ZoneRankings.class);
        } else {
            zoneRankings = objectMapper.treeToValue(zoneRankingsNode, ZoneRankings.class);
        }
        return zoneRankings;
    }

    /**
     * Convenience method to get the best performance average.
     *
     * @param characterName the character's name.
     * @param serverSlug    the server slug.
     * @param region        the region.
     * @return bestPerformanceAverage as a double.
     * @throws Exception if mapping fails.
     */
    public double getBestPerformanceAverage(String characterName, String serverSlug, String region) throws Exception {
        ZoneRankings rankings = getZoneRankings(characterName, serverSlug, region);
        // Debug print the computed best performance average.
        System.out.println("Best Performance Average for " + characterName + ": " + rankings.getBestPerformanceAverage());
        return rankings.getBestPerformanceAverage();
    }
}
