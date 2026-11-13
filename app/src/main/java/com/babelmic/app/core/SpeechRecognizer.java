package com.babelmic.app.core;

/**
 * Contract for converting spoken audio into a text string.
 * The real implementation uses Android's on-device SpeechRecognizer API.
 */
public interface SpeechRecognizer {
    /**
     * @param audioSource  Identifier for the audio source (file path, mic stream tag, etc.)
     * @return             Transcribed text, never null
     */
    String recognizeSpeech(String audioSource);
}
