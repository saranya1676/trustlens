package com.truthlens.entity;

/**
 * Enum representing the possible verification verdict for submitted content.
 */
public enum Verdict {
    LIKELY_TRUE("Likely True", "verdict-true", "bi-check-circle-fill",
            "The submitted content aligns closely with verified trusted sources."),
    LIKELY_FALSE("Likely False", "verdict-false", "bi-x-circle-fill",
            "The submitted content contradicts or significantly deviates from verified trusted sources."),
    NEEDS_VERIFICATION("Needs Verification", "verdict-unverified", "bi-question-circle-fill",
            "Insufficient trusted sources found to confirm or deny the submitted content.");

    private final String displayName;
    private final String cssClass;
    private final String icon;
    private final String defaultExplanation;

    Verdict(String displayName, String cssClass, String icon, String defaultExplanation) {
        this.displayName = displayName;
        this.cssClass = cssClass;
        this.icon = icon;
        this.defaultExplanation = defaultExplanation;
    }

    public String getDisplayName()       { return displayName; }
    public String getCssClass()          { return cssClass; }
    public String getIcon()              { return icon; }
    public String getDefaultExplanation(){ return defaultExplanation; }
}
