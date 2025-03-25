package com.nl.wowapi.boble.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nl.wowapi.boble.util.SafeIntegerDeserializer;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllStars {
    private double points;
    private double possiblePoints;
    private int partition;

    @JsonDeserialize(using = SafeIntegerDeserializer.class)
    private Integer rank;

    @JsonDeserialize(using = SafeIntegerDeserializer.class)
    private Integer regionRank;

    @JsonDeserialize(using = SafeIntegerDeserializer.class)
    private Integer rankPercent;

    private String spec;
    private int serverRank;
    private int total;
}
