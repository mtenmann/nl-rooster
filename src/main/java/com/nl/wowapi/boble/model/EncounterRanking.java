package com.nl.wowapi.boble.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncounterRanking {
    private Encounter encounter;
    private Double rankPercent;
    private Double medianPercent;
    private boolean lockedIn;
    private int totalKills;
    private int fastestKill;
    private AllStars allStars;
    private String spec;
    private String bestSpec;
    private double bestAmount;
    private int serverRank;
}
