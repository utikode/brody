package com.genapp.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Root object untuk struktur aplikasi
 */
public record Aplikasi(
    @JsonProperty("nama") String nama,
    @JsonProperty("packageName") String packageName,
    @JsonProperty("platform") Platform platform,
    @JsonProperty("monetization") MonetizationConfig monetization,
    @JsonProperty("database") DatabaseConfig database,
    @JsonProperty("screens") List<Layar> screens,
    @JsonProperty("version") String version,
    @JsonProperty("description") String description
) {
    public enum Platform {
        ANDROID,
        IOS,
        BOTH
    }
}
