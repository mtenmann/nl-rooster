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
}
