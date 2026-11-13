package com.babelmic.app.mock;

import com.babelmic.app.core.SpeechRecognizer;

/**
 * Mock implementation of {@link SpeechRecognizer} for unit testing.
 * Returns hardcoded transcription results based on the audio source filename.
 */
public class MockSpeechRecognizer implements SpeechRecognizer {

    private int microphoneSensitivity = 50;

    public int getMicrophoneSensitivity() {
        return microphoneSensitivity;
    }

    public void setMicrophoneSensitivity(int sensitivity) {
        if (sensitivity < 0 || sensitivity > 100) {
            throw new IllegalArgumentException("Sensitivity must be between 0 and 100.");
        }
        this.microphoneSensitivity = sensitivity;
    }

    @Override
    public String recognizeSpeech(String audioSource) {
        if (audioSource == null || audioSource.trim().isEmpty()) {
            throw new IllegalArgumentException("Audio source cannot be empty.");
        }
        System.out.println("[MockSTT] Processing: " + audioSource
                + " (sensitivity: " + microphoneSensitivity + "%)");

        if (audioSource.equals("alice_sample.wav")) return "Where is the train station?";
        if (audioSource.equals("bob_sample.wav"))   return "C'est tout droit.";

        return "Unrecognized speech audio.";
    }
}
