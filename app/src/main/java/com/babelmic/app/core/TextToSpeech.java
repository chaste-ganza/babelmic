package com.babelmic.app.core;

/**
 * Contract for rendering a text string as audible speech.
 * The real implementation wraps Android's TextToSpeech engine.
 */
public interface TextToSpeech {
    /**
     * @param text      The text to speak aloud
     * @param language  BCP-47 language tag (e.g. "en", "fr")
     */
    void speak(String text, String language);

    /** Release underlying TTS resources when no longer needed. */
    void shutdown();
}
