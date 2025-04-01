package com.nl.wowapi.boble.controller;

import com.nl.wowapi.boble.model.CharacterIdentifier;
import com.nl.wowapi.boble.model.CharacterOverviewDto;
import com.nl.wowapi.boble.service.CharacterOverviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/characters")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://nl-rooster-alb-615277410.eu-west-1.elb.amazonaws.com"})
public class CharactersController {

    private final CharacterOverviewService characterOverviewService;
    private final ObjectMapper objectMapper;

    public CharactersController(CharacterOverviewService characterOverviewService) {
        this.characterOverviewService = characterOverviewService;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping("/overview/{team}")
    public List<CharacterOverviewDto> getTeamCharacters(@PathVariable String team) {
        String jsonFile = "/characters-" + team.toLowerCase() + ".json";
        List<CharacterIdentifier> characterList = loadCharacterIdentifiers(jsonFile);
        List<CharacterOverviewDto> result = new ArrayList<>();
        for (CharacterIdentifier id : characterList) {
            try {
                result.add(characterOverviewService.getCharacterOverview(id.getRealm(), id.getName(), "eu"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @GetMapping("/overview")
    public List<CharacterOverviewDto> getAllCharacterOverviews(@RequestParam("team") String team) {
        List<CharacterIdentifier> characterList = loadCharacterIdentifiers(team);
        List<CharacterOverviewDto> result = new ArrayList<>();
        for (CharacterIdentifier id : characterList) {
            try {
                result.add(characterOverviewService.getCharacterOverview(id.getRealm(), id.getName(), "eu"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Reads a JSON file from the resources (e.g., "/characters-boble.json") and maps it to a list of CharacterIdentifier.
     */
    private List<CharacterIdentifier> loadCharacterIdentifiers(String jsonFilePath) {
        try {
            ClassPathResource resource = new ClassPathResource(jsonFilePath.startsWith("/") ? jsonFilePath.substring(1) : jsonFilePath);
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, new TypeReference<List<CharacterIdentifier>>() {});
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + jsonFilePath, e);
        }
    }

    }
}
