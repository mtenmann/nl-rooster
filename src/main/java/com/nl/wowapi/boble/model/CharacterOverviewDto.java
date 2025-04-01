package com.nl.wowapi.boble.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class CharacterOverviewDto {
    private String name;
    private String className;
    private String realm;
    private int equippedItemLevel;
    private String classIcon;
    private int mythicRating;
    private String mythicRatingColor;
    private String activeSpec;
    private String role;
    private double bestPerfAvgScore;

    public CharacterOverviewDto(String name, String className, String realm,
                                int equippedItemLevel, String classIcon,
                                int mythicRating, String mythicRatingColor,
                                String activeSpec, String role, double bestPerfAvgScore) {
        this.name = name;
        this.className = className;
        this.realm = realm;
        this.equippedItemLevel = equippedItemLevel;
        this.classIcon = classIcon;
        this.mythicRating = mythicRating;
        this.mythicRatingColor = mythicRatingColor;
        this.activeSpec = activeSpec;
        this.role = role;
        this.bestPerfAvgScore = bestPerfAvgScore;
    }

    public String getBlizzardUrl() {
        return "https://worldofwarcraft.blizzard.com/en-gb/character/eu/"
                + normalizeRealm(realm) + "/" + normalizeName(name);
    }

    public String getRaiderIoUrl() {
        return "https://raider.io/characters/eu/"
                + normalizeRealm(realm) + "/" + normalizeName(name)
                + "?season=season-tww-2";
    }

    public String getWarcraftLogsUrl() {
        return "https://www.warcraftlogs.com/character/eu/"
                + normalizeRealm(realm) + "/" + normalizeName(name);
    }

    // Helper methods to normalize realm and name (for URL compatibility)
    private String normalizeRealm(String realm) {
        return realm.toLowerCase().replace(" ", "-");
    }

    private String normalizeName(String name) {
        return name.toLowerCase();
    }
}
