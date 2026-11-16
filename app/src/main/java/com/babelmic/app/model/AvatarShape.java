package com.babelmic.app.model;

/**
 * The silhouette shapes available for a user's avatar visualizer.
 * Each shape defines how tall each vertical bar should be relative to
 * its horizontal position across the visualizer.
 */
public enum AvatarShape {
    HEART,
    STAR,
    CIRCLE,
    RECTANGLE;

    /** Human-readable label shown in the shape picker. */
    public String displayName() {
        switch (this) {
            case HEART:     return "Heart";
            case STAR:      return "Star";
            case CIRCLE:    return "Circle";
            case RECTANGLE: return "Rectangle";
            default:        return name();
        }
    }

    /** Emoji icon shown alongside the label in the picker. */
    public String emoji() {
        switch (this) {
            case HEART:     return "❤️";
            case STAR:      return "⭐";
            case CIRCLE:    return "⬤";
            case RECTANGLE: return "▬";
            default:        return "";
        }
    }
}
