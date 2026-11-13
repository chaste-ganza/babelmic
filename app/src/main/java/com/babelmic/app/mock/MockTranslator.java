package com.babelmic.app.mock;

import com.babelmic.app.core.Translator;

/**
 * Mock implementation of {@link Translator} for unit testing.
 * Handles a small set of hardcoded EN↔FR phrase pairs.
 */
public class MockTranslator implements Translator {

    private int translationCount = 0;

    public int getTranslationCount() {
        return translationCount;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to translate cannot be empty.");
        }
        translationCount++;

        // EN → FR
        if (sourceLang.equalsIgnoreCase("en") && targetLang.equalsIgnoreCase("fr")) {
            if (text.equalsIgnoreCase("Where is the train station?")) return "Où est la gare ?";
            return "[FR Mock] " + text;
        }

        // FR → EN
        if (sourceLang.equalsIgnoreCase("fr") && targetLang.equalsIgnoreCase("en")) {
            if (text.equalsIgnoreCase("C'est tout droit.")) return "It is straight ahead.";
            return "[EN Mock] " + text;
        }

        // Fallback — return original text unchanged
        return text;
    }
}
