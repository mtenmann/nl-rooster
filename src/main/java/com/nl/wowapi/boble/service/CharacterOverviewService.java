package com.nl.wowapi.boble.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl.wowapi.boble.model.CharacterOverviewDto;
import com.nl.wowapi.boble.model.ZoneRankings;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class CharacterOverviewService {

    private final BlizzardApiService blizzardApiService;
    private final WarcraftLogsClient warcraftLogsClient;
    private final ObjectMapper objectMapper;

    public CharacterOverviewService(BlizzardApiService blizzardApiService, WarcraftLogsClient warcraftLogsClient) {
        this.blizzardApiService = blizzardApiService;
        this.warcraftLogsClient = warcraftLogsClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Merges data from the Blizzard API and the Warcraft Logs API into a single CharacterOverviewDto.
     *
     * @param realm         The realm of the character.
     * @param characterName The name of the character.
     * @param region        The region (for example, "eu").
     * @return A fully populated CharacterOverviewDto.
     */
    public CharacterOverviewDto getCharacterOverview(String realm, String characterName, String region) {
        try {
            // Retrieve Blizzard API data
            String profileJson = blizzardApiService.getCharacterProfile(realm, characterName);
            String mythicJson = blizzardApiService.getMythicKeystoneProfile(realm, characterName);

            // Parse Blizzard data
            JsonNode profileRoot = objectMapper.readTree(profileJson);
            String name = profileRoot.path("name").asText();
            String className = profileRoot.path("character_class").path("name").asText();
            String realmName = profileRoot.path("realm").path("name").asText();
            int equippedItemLevel = profileRoot.path("equipped_item_level").asInt();
            String activeSpec = profileRoot.path("active_spec").path("name").asText();
            String classIcon = mapClassNameToIconUrl(className);
            String role = determineRole(className, activeSpec);

            int mythicRating = 0;
            String mythicRatingColor = "rgba(255,255,255,1)";
            if (mythicJson != null && !mythicJson.isEmpty()) {
                JsonNode mythicRoot = objectMapper.readTree(mythicJson);
                JsonNode ratingNode = mythicRoot.path("current_mythic_rating");
                mythicRating = ratingNode.path("rating").asInt(0);
                JsonNode colorNode = ratingNode.path("color");
                int r = colorNode.path("r").asInt(255);
                int g = colorNode.path("g").asInt(255);
                int b = colorNode.path("b").asInt(255);
                double a = colorNode.path("a").asDouble(1.0);
                mythicRatingColor = String.format("rgba(%d,%d,%d,%.2f)", r, g, b, a);

                // Kødde med Hodevine - TODO: Ta vekk
                if ("hodevine".equalsIgnoreCase(characterName)) {
                    int penalty = ThreadLocalRandom.current().nextInt(50, 301);
                    mythicRating = Math.max(0, mythicRating - penalty);
                }
            }

            // Retrieve Warcraft Logs data (zone rankings, encounters, etc.)
            ZoneRankings zoneRankings = warcraftLogsClient.getZoneRankings(characterName, realm.toLowerCase(), region);
            double bestPerfAvg = zoneRankings.getBestPerformanceAverage();

            // Kode for å "hjelpe" Hodevine sin score - TODO: Ta vekk
            if ("hodevine".equalsIgnoreCase(characterName)) {
                int penalty = ThreadLocalRandom.current().nextInt(5, 20);
                bestPerfAvg = Math.max(0, bestPerfAvg - penalty);
            }

            // Build and return the merged DTO
            return new CharacterOverviewDto(
                    name,
                    className,
                    realmName,
                    equippedItemLevel,
                    classIcon,
                    mythicRating,
                    mythicRatingColor,
                    activeSpec,
                    role,
                    bestPerfAvg
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get character overview for " + characterName, e);
        }
    }

    private String mapClassNameToIconUrl(String className) {
        if (className == null || className.isBlank()) {
            return "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/default.png";
        }
        String key = className.toLowerCase().replace(" ", "");
        return switch (key) {
            case "deathknight" -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/deathknight.png";
            case "demonhunter"  -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/demonhunter.png";
            case "druid"        -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/druid.png";
            case "hunter"       -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/hunter.png";
            case "mage"         -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/mage.png";
            case "monk"         -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/monk.png";
            case "paladin"      -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/paladin.png";
            case "priest"       -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/priest.png";
            case "rogue"        -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/rogue.png";
            case "shaman"       -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/shaman.png";
            case "warlock"      -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/warlock.png";
            case "warrior"      -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/warrior.png";
            default             -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/default.png";
        };
    }

    private String determineRole(String className, String specName) {
        className = className.toLowerCase();
        specName = specName.toLowerCase();
        return switch (className) {
            case "warrior" -> specName.equals("protection") ? "Tank" : "DPS";
            case "paladin" -> {
                if (specName.equals("holy")) yield "Healer";
                if (specName.equals("protection")) yield "Tank";
                yield "DPS";
            }
            case "hunter" -> "DPS";
            case "rogue" -> "DPS";
            case "priest" -> (specName.equals("discipline") || specName.equals("holy")) ? "Healer" : "DPS";
            case "death knight" -> specName.equals("blood") ? "Tank" : "DPS";
            case "shaman" -> specName.equals("restoration") ? "Healer" : "DPS";
            case "mage" -> "DPS";
            case "warlock" -> "DPS";
            case "monk" -> {
                if (specName.equals("brewmaster")) yield "Tank";
                if (specName.equals("mistweaver")) yield "Healer";
                yield "DPS";
            }
            case "druid" -> {
                if (specName.equals("guardian")) yield "Tank";
                if (specName.equals("restoration")) yield "Healer";
                yield "DPS";
            }
            case "demon hunter" -> specName.equals("vengeance") ? "Tank" : "DPS";
            case "evoker" -> specName.equals("preservation") ? "Healer" : "DPS";
            default -> "DPS";
        };
    }
}
