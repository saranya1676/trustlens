package com.truthlens.entity;

/**
 * Enum representing the type of content submitted for verification.
 */
public enum SubmissionType {
    TEXT("Plain text or article content"),
    URL("News article URL"),
    IMAGE("Image containing news text");

    private final String description;

    SubmissionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
