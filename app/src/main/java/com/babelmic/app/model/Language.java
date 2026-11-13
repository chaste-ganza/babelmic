package com.babelmic.app.model;

/**
 * Immutable value object representing a supported translation language.
 *
 * <p>Each language carries three identifiers:
 * <ul>
 *   <li>{@code bcp47Tag}   — the BCP-47 tag used by ML Kit and Android TTS/STT
 *                            (e.g. {@code "fr"}, {@code "zh"}).</li>
 *   <li>{@code displayName} — the English name shown in the UI picker
 *                             (e.g. {@code "French"}).</li>
 *   <li>{@code nativeName}  — the name in the language itself, shown as a subtitle
 *                             in the picker (e.g. {@code "Français"}).</li>
 * </ul>
 * </p>
 */
public final class Language {

    private final String bcp47Tag;
    private final String displayName;
    private final String nativeName;
    private final String flagEmoji;

    public Language(String bcp47Tag, String displayName, String nativeName, String flagEmoji) {
        this.bcp47Tag    = bcp47Tag;
        this.displayName = displayName;
        this.nativeName  = nativeName;
        this.flagEmoji   = flagEmoji;
    }

    /** BCP-47 language tag, e.g. {@code "en"}, {@code "fr"}, {@code "es"}. */
    public String getBcp47Tag()    { return bcp47Tag;    }

    /** English display name, e.g. {@code "French"}. */
    public String getDisplayName() { return displayName; }

    /** Name in the language itself, e.g. {@code "Français"}. */
    public String getNativeName()  { return nativeName;  }

    /** Flag emoji for visual identification in the picker, e.g. {@code "🇫🇷"}. */
    public String getFlagEmoji()   { return flagEmoji;   }

    @Override
    public String toString() {
        return flagEmoji + "  " + displayName + " (" + nativeName + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Language)) return false;
        return bcp47Tag.equals(((Language) o).bcp47Tag);
    }

    @Override
    public int hashCode() {
        return bcp47Tag.hashCode();
    }
}
