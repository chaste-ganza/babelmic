package com.babelmic.app.core;

/**
 * Contract for translating text from one language to another.
 * The real implementation uses Google ML Kit on-device translation.
 */
public interface Translator {
    /**
     * @param text        The text to translate
     * @param sourceLang  BCP-47 language tag of the source (e.g. "en", "fr")
     * @param targetLang  BCP-47 language tag of the target
     * @return            Translated text, never null
     */
    String translate(String text, String sourceLang, String targetLang);
}
