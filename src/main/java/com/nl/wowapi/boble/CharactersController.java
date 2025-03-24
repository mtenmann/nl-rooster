package com.nl.wowapi.boble;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/characters")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class CharactersController {

    private final BlizzardApiService blizzardApiService;
    private final ObjectMapper objectMapper;
    private final WarcraftLogsApiService warcraftLogsApiService;

    public CharactersController(BlizzardApiService blizzardApiService, WarcraftLogsApiService warcraftLogsApiService) {
        this.blizzardApiService = blizzardApiService;
        this.objectMapper = new ObjectMapper();
        this.warcraftLogsApiService = warcraftLogsApiService;
    }

    /**
     * Endpoint to return the overview DTOs for a specific team, e.g.:
     * GET /api/characters/overview/boble
     * GET /api/characters/overview/tempo
     */
    @GetMapping("/overview/{team}")
    public List<CharacterOverviewDto> getTeamCharacters(@PathVariable String team) {
        // build the JSON filename, e.g. "/characters-boble.json"
        String jsonFile = "/characters-" + team.toLowerCase() + ".json";
        List<CharacterIdentifier> characterList = loadCharacterIdentifiers(jsonFile);

        List<CharacterOverviewDto> result = new ArrayList<>();
        for (CharacterIdentifier id : characterList) {
            // 1) Normal profile
            String profileJson = blizzardApiService.getCharacterProfile(id.realm(), id.name());
            // 2) Mythic keystone profile
            String mythicJson = blizzardApiService.getMythicKeystoneProfile(id.realm(), id.name());

            // 3) Merge them into a single DTO
            CharacterOverviewDto dto = mapToOverviewDto(profileJson, mythicJson);
            result.add(dto);
        }
        return result;
    }

    @GetMapping("/overview")
    public List<CharacterOverviewDto> getAllCharacterOverviews(@RequestParam("team") String team) {
        List<CharacterIdentifier> characterList = loadCharacterIdentifiers(team);
        List<CharacterOverviewDto> result = new ArrayList<>();
        for (CharacterIdentifier id : characterList) {
            String profileJson = blizzardApiService.getCharacterProfile(id.realm(), id.name());
            String mythicJson = blizzardApiService.getMythicKeystoneProfile(id.realm(), id.name());
            result.add(mapToOverviewDto(profileJson, mythicJson));
        }
        return result;
    }

    /**
     * Reads a given JSON file (like "/characters-boble.json") and returns a List of CharacterIdentifier
     */
    private List<CharacterIdentifier> loadCharacterIdentifiers(String team) {
        // Remove any leading/trailing slashes.
        if (team.startsWith("/")) {
            team = team.substring(1);
        }
        // If team parameter starts with "characters-" and ends with ".json", strip them.
        String normalizedTeam = team;
        if (team.toLowerCase().startsWith("characters-") && team.toLowerCase().endsWith(".json")) {
            normalizedTeam = team.substring("characters-".length(), team.length() - ".json".length());
        }

        String fileName;
        switch (normalizedTeam.toLowerCase()) {
            case "tempo":
                fileName = "/characters-tempo.json";
                break;
            case "boble":
                fileName = "/characters-boble.json";
                break;
            case "panser":
                fileName = "/characters-panser.json";
                break;
            case "foniks":
                fileName = "/characters-foniks.json";
                break;
            case "delta":
                fileName = "/characters-delta.json";
                break;
            default:
                throw new RuntimeException("Unknown team: " + normalizedTeam);
        }
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new RuntimeException(fileName + " file not found in resources");
            }
            return objectMapper.readValue(is, new TypeReference<List<CharacterIdentifier>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + fileName, e);
        }
    }

    /**
     * Merge data from both the normal character profile JSON and mythic keystone JSON
     */
    private CharacterOverviewDto mapToOverviewDto(String profileJson, String mythicJson) {
        try {
            JsonNode profileRoot = objectMapper.readTree(profileJson);
            String name = profileRoot.path("name").asText();
            String className = profileRoot.path("character_class").path("name").asText();
            String realm = profileRoot.path("realm").path("name").asText();
            int equippedItemLevel = profileRoot.path("equipped_item_level").asInt();
            String classIcon = mapClassNameToIconUrl(className);
            String activeSpec = profileRoot.path("active_spec").path("name").asText();
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
            }

            // Call the Warcraft Logs service to get the best performance average score.
            // The third argument "eu" is an example for the region.
            double bestPerfAvg = warcraftLogsApiService.getBestPerformanceAvg(name, realm.toLowerCase(), "eu");

            return new CharacterOverviewDto(
                    name,
                    className,
                    realm,
                    equippedItemLevel,
                    classIcon,
                    mythicRating,
                    mythicRatingColor,
                    activeSpec,
                    role,
                    bestPerfAvg   // Make sure your DTO has a field (and constructor) for this.
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to map character data", e);
        }
    }

    /**
     * Map a class name to an icon URL
     */
    private String mapClassNameToIconUrl(String className) {
        if (className == null || className.isBlank()) {
            return "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/default.png";
        }

        String key = className.toLowerCase().replace(" ", "");
        return switch (key) {
            case "deathknight"   -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/deathknight.png";
            case "demonhunter"    -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/demonhunter.png";
            case "druid"          -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/druid.png";
            case "hunter"         -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/hunter.png";
            case "mage"           -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/mage.png";
            case "monk"           -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/monk.png";
            case "paladin"        -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/paladin.png";
            case "priest"         -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/priest.png";
            case "rogue"          -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/rogue.png";
            case "shaman"         -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/shaman.png";
            case "warlock"        -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/warlock.png";
            case "warrior"        -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/warrior.png";
            default               -> "https://raw.githubusercontent.com/orourkek/Wow-Icons/master/images/class/64/default.png";
        };
    }

    private String determineRole(String className, String specName) {
        className = className.toLowerCase();
        specName = specName.toLowerCase();
        switch (className) {
            case "warrior":
                if (specName.equals("protection")) return "Tank";
                else return "DPS";
            case "paladin":
                if (specName.equals("holy")) return "Healer";
                if (specName.equals("protection")) return "Tank";
                else return "DPS";
            case "hunter":
                return "DPS";
            case "rogue":
                return "DPS";
            case "priest":
                if (specName.equals("discipline") || specName.equals("holy")) return "Healer";
                else return "DPS";
            case "death knight":
                if (specName.equals("blood")) return "Tank";
                else return "DPS";
            case "shaman":
                if (specName.equals("restoration")) return "Healer";
                else return "DPS";
            case "mage":
                return "DPS";
            case "warlock":
                return "DPS";
            case "monk":
                if (specName.equals("brewmaster")) return "Tank";
                if (specName.equals("mistweaver")) return "Healer";
                else return "DPS";
            case "druid":
                if (specName.equals("guardian")) return "Tank";
                if (specName.equals("restoration")) return "Healer";
                else return "DPS";
            case "demon hunter":
                if (specName.equals("vengeance")) return "Tank";
                else return "DPS";
            case "evoker":
                if (specName.equals("preservation")) return "Healer";
                else return "DPS";
            default:
                return "DPS"; // Fallback role
        }
    }


}
