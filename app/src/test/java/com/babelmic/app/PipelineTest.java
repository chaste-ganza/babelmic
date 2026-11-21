package com.babelmic.app;

import com.babelmic.app.mock.MockPeerConnection;
import com.babelmic.app.mock.MockSpeechRecognizer;
import com.babelmic.app.mock.MockTextToSpeech;
import com.babelmic.app.mock.MockTranslator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** End-to-end check of the four-block pipeline from the original console demo. */
public class PipelineTest {

    @Test
    public void speechIsRecognizedTranslatedSentAndSpoken() {
        MockSpeechRecognizer recognizer = new MockSpeechRecognizer();
        MockTranslator translator = new MockTranslator();
        MockTextToSpeech tts = new MockTextToSpeech();
        MockPeerConnection connection = new MockPeerConnection();

        assertTrue(connection.connect("Bob-Pixel16"));

        String original = recognizer.recognizeSpeech("alice_sample.wav");
        assertEquals("Where is the train station?", original);

        String translated = translator.translate(original, "en", "fr");
        assertEquals("Où est la gare ?", translated);

        connection.sendData(translated);
        tts.speak(translated, "French");

        assertEquals(1, translator.getTranslationCount());
        assertTrue(connection.isConnected());

        connection.disconnect();
        assertFalse(connection.isConnected());
    }
}
