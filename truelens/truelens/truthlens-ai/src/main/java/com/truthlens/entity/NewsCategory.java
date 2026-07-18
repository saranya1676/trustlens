package com.truthlens.entity;

/**
 * Enum representing the five news categories supported by TruthLens AI.
 */
public enum NewsCategory {
    INTERNATIONAL("International", "bi-globe"),
    NATIONAL("National", "bi-flag"),
    POLITICS("Politics", "bi-building"),
    SPORTS("Sports", "bi-trophy"),
    TECHNOLOGY("Technology", "bi-cpu");

    private final String displayName;
    private final String icon;

    NewsCategory(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * Returns a CSS badge color class for each category.
     */
    public String getBadgeClass() {
        return switch (this) {
            case INTERNATIONAL -> "badge-international";
            case NATIONAL      -> "badge-national";
            case POLITICS      -> "badge-politics";
            case SPORTS        -> "badge-sports";
            case TECHNOLOGY    -> "badge-technology";
        };
    }
}
