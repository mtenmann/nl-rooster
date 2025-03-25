package com.nl.wowapi.boble.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
public class WarcraftLogsTokenService {

    @Value("${warcraftlogs.client.id}")
    private String clientId;

    @Value("${warcraftlogs.client.secret}")
    private String clientSecret;

    private final String tokenUrl = "https://www.warcraftlogs.com/oauth/token";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // In a real app, you’d also cache the expiry time.
    private String token;

    public WarcraftLogsTokenService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Retrieves and caches the OAuth token using client credentials flow.
     *
     * @return the access token as a String.
     */
    public String getAccessToken() {
        if (token != null) {
            return token;
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, JsonNode.class);
        token = response.getBody().path("access_token").asText();
        return token;
    }
}
