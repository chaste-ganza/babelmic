package com.babelmic.app.model;

/**
 * Represents a single translated exchange shown in the conversation screen.
 *
 * <p>Each message carries both the original spoken text and the translated
 * version so both are shown on screen simultaneously.</p>
 */
public class TranslationMessage {

    public enum Direction {
        /** This device's user spoke — message originated locally. */
        OUTGOING,
        /** The peer spoke — message arrived over the P2P connection. */
        INCOMING
    }

    private final String    originalText;
    private final String    translatedText;
    private final Direction direction;
    private final long      timestampMs;

    public TranslationMessage(String originalText,
                              String translatedText,
                              Direction direction) {
        this.originalText   = originalText;
        this.translatedText = translatedText;
        this.direction      = direction;
        this.timestampMs    = System.currentTimeMillis();
    }

    public String    getOriginalText()   { return originalText;   }
    public String    getTranslatedText() { return translatedText; }
    public Direction getDirection()      { return direction;      }
    public long      getTimestampMs()    { return timestampMs;    }

    public boolean isOutgoing() { return direction == Direction.OUTGOING; }
}
