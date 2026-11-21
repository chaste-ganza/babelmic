package com.babelmic.app;

import com.babelmic.app.mock.MockTextToSpeech;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Tests the text-to-speech practice component. */
public class MockTextToSpeechTest {

    @Test
    public void startsUnmuted() {
        assertFalse(new MockTextToSpeech().isMuted());
    }

    @Test
    public void muteFlagCanBeToggled() {
        MockTextToSpeech tts = new MockTextToSpeech();

        tts.setMuted(true);
        assertTrue(tts.isMuted());

        tts.setMuted(false);
        assertFalse(tts.isMuted());
    }

    @Test
    public void speakingIsSafeWhenMuted() {
        MockTextToSpeech tts = new MockTextToSpeech();
        tts.setMuted(true);

        tts.speak("Bonjour", "French");
    }

    @Test
    public void emptyTextIsRejected() {
        MockTextToSpeech tts = new MockTextToSpeech();
        try {
            tts.speak("", "French");
            fail("Expected IllegalArgumentException for empty text");
        } catch (IllegalArgumentException expected) {
            // Expected behaviour.
        }
    }
}
