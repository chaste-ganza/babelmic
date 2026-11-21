package com.babelmic.app;

import com.babelmic.app.model.Language;
import com.babelmic.app.util.LanguageManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/** Tests the shared language catalogue used by the picker and translation pipeline. */
public class LanguageManagerTest {

    @Test
    public void findsLanguageByTagIgnoringCase() {
        Language french = LanguageManager.findByTag("FR");

        assertNotNull(french);
        assertEquals("fr", french.getBcp47Tag());
        assertEquals("French", french.getDisplayName());
    }

    @Test
    public void findsLanguageByDisplayNameIgnoringCase() {
        Language english = LanguageManager.findByDisplayName("english");

        assertNotNull(english);
        assertEquals("en", english.getBcp47Tag());
    }

    @Test
    public void unknownLanguageReturnsNull() {
        assertNull(LanguageManager.findByTag("xx-not-a-language"));
        assertNull(LanguageManager.findByDisplayName("Klingon"));
    }

    @Test
    public void catalogueContainsCommonLanguages() {
        assertNotNull(LanguageManager.findByTag("en"));
        assertNotNull(LanguageManager.findByTag("fr"));
        assertNotNull(LanguageManager.findByTag("ar"));
        assertNotNull(LanguageManager.findByTag("zh"));
    }

    @Test
    public void catalogueCannotBeModifiedByCallers() {
        try {
            LanguageManager.getSupportedLanguages().clear();
            fail("Expected the language list to be read-only");
        } catch (UnsupportedOperationException expected) {
            // Expected behaviour.
        }
    }
}
