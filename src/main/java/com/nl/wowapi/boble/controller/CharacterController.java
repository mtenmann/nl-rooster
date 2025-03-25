package com.nl.wowapi.boble.controller;

import com.nl.wowapi.boble.service.BlizzardApiService;
import org.springframework.web.bind.annotation.*;

// Not really used anymore, but will keep it if we need to do individual lookups later
@RestController
@RequestMapping("/api/character")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class CharacterController {
    private final BlizzardApiService blizzardApiService;

    public CharacterController(BlizzardApiService blizzardApiService) {
        this.blizzardApiService = blizzardApiService;
    }

    @GetMapping("/{realm}/{name}")
    public String getCharacter(@PathVariable String realm, @PathVariable String name) {
        return blizzardApiService.getCharacterProfile(realm, name);
    }
}
