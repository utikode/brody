package com.genapp.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Komponen UI dalam layar
 */
public record Komponen(
    @JsonProperty("tipe") TipeKomponen tipe,
    @JsonProperty("id") String id,
    @JsonProperty("label") String label,
    @JsonProperty("properties") Map<String, Object> properties,
    @JsonProperty("dataSource") String dataSource,
    @JsonProperty("actionType") ActionType actionType,
    @JsonProperty("children") List<Komponen> children,
    @JsonProperty("styling") Map<String, String> styling
) {
    public enum TipeKomponen {
        TEXT,
        BUTTON,
        TEXTFIELD,
        LAZYCOLUMN,
        LAZYROW,
        COLUMN,
        ROW,
        BOX,
        IMAGE,
        ICON,
        CARD,
        SPACER,
        DIVIDER,
        BOTTOMNAVIGATION,
        TOPAPPBAR,
        FLOATINGACTIONBUTTON,
        DIALOG,
        SNACKBAR,
        CUSTOM
    }

    public enum ActionType {
        NONE,
        NAVIGATE,
        FETCH_DATA,
        SUBMIT_FORM,
        ADD_TO_CART,
        REMOVE_FROM_CART,
        SHOW_AD,
        DISMISS_AD,
        LOGOUT,
        REFRESH,
        OPEN_URL,
        CUSTOM
    }
}
