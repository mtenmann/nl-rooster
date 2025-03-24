package com.nl.wowapi.boble;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class BlizzardApiService {

    @Value("${blizzard.api.url}")
    private String blizzardApiUrl;

    @Value("${blizzard.client.id}")
    private String clientId;

    @Value("${blizzard.client.secret}")
    private String clientSecret;

    private String accessToken;

    public BlizzardApiService() {
    }

    public String getAccessToken() {
        if (this.accessToken == null || this.accessToken.isEmpty()) {
            this.accessToken = fetchAccessToken();
        }
        return this.accessToken;
    }

    public String fetchAccessToken() {
        String url = "https://oauth.battle.net/token";
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);

            // ✅ Set headers
            request.setHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth));
            request.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"));
            request.setHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/json"));

            // ✅ Set request body
            request.setEntity(new StringEntity("grant_type=client_credentials", StandardCharsets.UTF_8));

            // ✅ Execute request
            try (CloseableHttpResponse response = httpClient.execute(request, HttpCoreContext.create())) {
                org.apache.hc.core5.http.HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    System.out.println("OAuth Response: " + responseBody);
                    return extractAccessToken(responseBody);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Failed to retrieve access token");
    }

    private String extractAccessToken(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            return rootNode.path("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse access token", e);
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken());
        headers.set(HttpHeaders.ACCEPT, "application/json");
        return headers;
    }

    public String fetchWoWTokenPrice() {
        String url = blizzardApiUrl + "/data/wow/token/?namespace=dynamic-eu";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken()); // ✅ Use Bearer Token
        headers.set(HttpHeaders.ACCEPT, "application/json"); // ✅ Ensure correct response type

        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching WoW Token price: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        }
    }

    @Cacheable(value = "characterProfiles", key = "#realm + '_' + #characterName")
    public String getCharacterProfile(String realm, String characterName) {
        String realmSlug = realm.trim().toLowerCase().replaceAll("\\s+", "-");
        String url = blizzardApiUrl + "/profile/wow/character/"
                + realmSlug + "/" + characterName.toLowerCase()
                + "?namespace=profile-eu&locale=en_GB";

        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching character profile: "
                    + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        }
    }

    @Cacheable(value = "mythicProfiles", key = "#realm + '_' + #characterName")
    public String getMythicKeystoneProfile(String realm, String characterName) {
        String realmSlug = realm.trim().toLowerCase().replaceAll("\\s+", "-");
        String url = blizzardApiUrl + "/profile/wow/character/"
                + realmSlug + "/" + characterName.toLowerCase() + "/mythic-keystone-profile"
                + "?namespace=profile-eu&locale=en_GB";

        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching mythic keystone profile: "
                    + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        }
    }

}
