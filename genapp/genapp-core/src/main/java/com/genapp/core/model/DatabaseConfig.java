package com.genapp.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Konfigurasi database (Supabase / Firebase)
 */
public record DatabaseConfig(
    @JsonProperty("provider") Provider provider,
    @JsonProperty("url") String url,
    @JsonProperty("apiKey") String apiKey,
    @JsonProperty("anonKey") String anonKey,
    @JsonProperty("projectId") String projectId,
    @JsonProperty("tables") java.util.List<String> tables
) {
    public enum Provider {
        SUPABASE,
        FIREBASE,
        NONE
    }

    public static DatabaseConfig none() {
        return new DatabaseConfig(Provider.NONE, null, null, null, null, null);
    }
}
