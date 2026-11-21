package com.babelmic.app;

import com.babelmic.app.mock.MockTranslator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** Tests the translation practice component. */
public class MockTranslatorTest {

    @Test
    public void translatesKnownEnglishPhraseToFrench() {
        MockTranslator translator = new MockTranslator();

        assertEquals("Où est la gare ?",
                translator.translate("Where is the train station?", "en", "fr"));
    }

    @Test
    public void translatesKnownFrenchPhraseToEnglish() {
        MockTranslator translator = new MockTranslator();

        assertEquals("It is straight ahead.",
                translator.translate("C'est tout droit.", "fr", "en"));
    }

    @Test
    public void unsupportedLanguagePairReturnsOriginalText() {
        MockTranslator translator = new MockTranslator();

        assertEquals("Hello", translator.translate("Hello", "en", "es"));
    }

    @Test
    public void emptyTextIsRejected() {
        MockTranslator translator = new MockTranslator();
        try {
            translator.translate(null, "en", "fr");
            fail("Expected IllegalArgumentException for null text");
        } catch (IllegalArgumentException expected) {
            // Expected behaviour.
        }
    }

    @Test
    public void translationCountIncrementsForEveryValidCall() {
        MockTranslator translator = new MockTranslator();
        assertEquals(0, translator.getTranslationCount());

        translator.translate("Where is the train station?", "en", "fr");
        translator.translate("Hello", "en", "fr");

        assertEquals(2, translator.getTranslationCount());
    }
}
