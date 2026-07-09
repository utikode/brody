package com.genapp.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Definisi layar dalam aplikasi
 */
public record Layar(
    @JsonProperty("nama") String nama,
    @JsonProperty("tipe") TipeLayar tipe,
    @JsonProperty("komponen") List<Komponen> komponen,
    @JsonProperty("route") String route,
    @JsonProperty("isDefault") boolean isDefault
) {
    public enum TipeLayar {
        SPLASH,
        LOGIN,
        REGISTER,
        HOME,
        PRODUCT_DETAIL,
        CART,
        CHECKOUT,
        PROFILE,
        SETTINGS,
        CUSTOM
    }
}
