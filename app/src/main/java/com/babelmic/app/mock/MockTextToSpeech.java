package com.babelmic.app.mock;

import com.babelmic.app.core.TextToSpeech;

/**
 * Mock implementation of {@link TextToSpeech} for unit testing.
 * Prints output to stdout; supports a mute flag to simulate a silenced device.
 */
public class MockTextToSpeech implements TextToSpeech {

    private boolean isMuted = false;

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { this.isMuted = muted; }

    @Override
    public void speak(String text, String language) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to speak cannot be empty.");
        }
        if (isMuted) {
            System.out.println("[MockTTS MUTED]: \"" + text + "\"");
            return;
        }
        System.out.println("[MockTTS (" + language + ")]: Speaking -> \"" + text + "\"");
    }

    @Override
    public void shutdown() {
        System.out.println("[MockTTS]: shutdown called.");
    }
}
