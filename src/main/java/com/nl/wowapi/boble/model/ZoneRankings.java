package com.nl.wowapi.boble.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZoneRankings {

    @JsonProperty("bestPerformanceAverage")
    private double bestPerformanceAverage;

    @JsonProperty("medianPerformanceAverage")
    private double medianPerformanceAverage;

    private int difficulty;
    private String metric;
    private int partition;
    private int zone;
    private int serverRank;
    private List<AllStars> allStars;
    private List<EncounterRanking> rankings;
    private String spec;

    public double getBestPerformanceAverage() {
        return bestPerformanceAverage;
    }
}

