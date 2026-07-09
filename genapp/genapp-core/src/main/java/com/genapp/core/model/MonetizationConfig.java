package com.genapp.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Konfigurasi monetisasi (AdMob, Facebook Audience Network)
 */
public record MonetizationConfig(
    @JsonProperty("enabled") boolean enabled,
    @JsonProperty("admobAppId") String admobAppId,
    @JsonProperty("bannerAdUnitId") String bannerAdUnitId,
    @JsonProperty("interstitialAdUnitId") String interstitialAdUnitId,
    @JsonProperty("rewardedAdUnitId") String rewardedAdUnitId,
    @JsonProperty("facebookPlacementId") String facebookPlacementId,
    @JsonProperty("adFrequency") AdFrequency adFrequency
) {
    public enum AdFrequency {
        LOW,      // Iklan jarang muncul
        MEDIUM,   // Iklan muncul sesekali
        HIGH      // Iklan sering muncul
    }

    public static MonetizationConfig disabled() {
        return new MonetizationConfig(false, null, null, null, null, null, AdFrequency.MEDIUM);
    }
}
