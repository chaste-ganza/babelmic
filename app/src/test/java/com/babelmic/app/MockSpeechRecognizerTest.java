package com.babelmic.app;

import com.babelmic.app.mock.MockSpeechRecognizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** Tests the speech-to-text practice component. */
public class MockSpeechRecognizerTest {

    @Test
    public void recognizesKnownEnglishSample() {
        MockSpeechRecognizer recognizer = new MockSpeechRecognizer();

        String text = recognizer.recognizeSpeech("alice_sample.wav");

        assertEquals("Where is the train station?", text);
    }

    @Test
    public void recognizesKnownFrenchSample() {
        MockSpeechRecognizer recognizer = new MockSpeechRecognizer();

        String text = recognizer.recognizeSpeech("bob_sample.wav");

        assertEquals("C'est tout droit.", text);
    }

    @Test
    public void unknownSampleGetsFallbackText() {
        MockSpeechRecognizer recognizer = new MockSpeechRecognizer();

        assertEquals("Unrecognized speech audio.",
                recognizer.recognizeSpeech("some_other_file.wav"));
    }

    @Test
    public void emptyAudioSourceIsRejected() {
        MockSpeechRecognizer recognizer = new MockSpeechRecognizer();
        try {
            recognizer.recognizeSpeech("   ");
            fail("Expected IllegalArgumentException for an empty audio source");
        } catch (IllegalArgumentException expected) {
            // Expected behaviour.
        }
    }

    @Test
    public void sensitivityDefaultsToFifty() {
        assertEquals(50, new MockSpeechRecognizer().getMicrophoneSensitivity());
    }

    @Test
    public void sensitivityMustBeBetweenZeroAndOneHundred() {
        MockSpeechRecognizer recognizer = new MockSpeechRecognizer();

        recognizer.setMicrophoneSensitivity(0);
        recognizer.setMicrophoneSensitivity(100);
        assertEquals(100, recognizer.getMicrophoneSensitivity());

        try {
            recognizer.setMicrophoneSensitivity(101);
            fail("Expected IllegalArgumentException for sensitivity above 100");
        } catch (IllegalArgumentException expected) {
            // Expected behaviour.
        }

        try {
            recognizer.setMicrophoneSensitivity(-1);
            fail("Expected IllegalArgumentException for negative sensitivity");
        } catch (IllegalArgumentException expected) {
            // Expected behaviour.
        }
    }
}
