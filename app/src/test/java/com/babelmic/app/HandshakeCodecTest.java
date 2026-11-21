package com.babelmic.app;

import com.babelmic.app.model.AvatarShape;
import com.babelmic.app.util.HandshakeCodec;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Tests the profile "business card" exchanged when two phones connect. */
public class HandshakeCodecTest {

    private static String profile(String name) {
        return HandshakeCodec.encode(name, "en", "English", "HEART",
                "avatar_bird", 0xFF123456, 0xFF1A73E8, 0xFF4FC3F7);
    }

    @Test
    public void difficultNameSurvivesRoundTrip() {
        String name = "Alice \"A\\B\"\nO'Brien: 🎧, from Paris";

        HandshakeCodec.Parsed decoded = HandshakeCodec.decode(profile(name));

        assertEquals(name, decoded.name);
    }

    @Test
    public void allFieldsSurviveRoundTrip() {
        String json = HandshakeCodec.encode("Alice", "fr", "Français", "STAR",
                "avatar_cat", 0xFFAABBCC, 0xFF112233, 0xFF445566);

        HandshakeCodec.Parsed p = HandshakeCodec.decode(json);

        assertEquals("Alice", p.name);
        assertEquals("fr", p.languageTag);
        assertEquals("Français", p.languageName);
        assertEquals(AvatarShape.STAR, p.avatarShape);
        assertEquals("avatar_cat", p.avatarName);
        assertEquals(0xFFAABBCC, p.identityColor);
        assertEquals(0xFF112233, p.colorTop);
        assertEquals(0xFF445566, p.colorBottom);
    }

    @Test
    public void malformedJsonUsesDefaults() {
        HandshakeCodec.Parsed p = HandshakeCodec.decode("{ this is not json");

        assertEquals(HandshakeCodec.DEFAULT_NAME, p.name);
        assertEquals(HandshakeCodec.DEFAULT_LANG_TAG, p.languageTag);
        assertEquals(HandshakeCodec.DEFAULT_LANG_NAME, p.languageName);
        assertEquals(AvatarShape.CIRCLE, p.avatarShape);
        assertEquals(HandshakeCodec.DEFAULT_IDENTITY_COLOR, p.identityColor);
    }

    @Test
    public void missingFieldsUseDefaults() {
        HandshakeCodec.Parsed p = HandshakeCodec.decode("{}");

        assertEquals(HandshakeCodec.DEFAULT_NAME, p.name);
        assertEquals(HandshakeCodec.DEFAULT_AVATAR, p.avatarName);
        assertEquals(HandshakeCodec.DEFAULT_COLOR_TOP, p.colorTop);
        assertEquals(HandshakeCodec.DEFAULT_COLOR_BOTTOM, p.colorBottom);
    }

    @Test
    public void invalidShapeFallsBackToCircle() {
        String json = "{\"n\":\"Bob\",\"s\":\"TRIANGLE\"}";

        assertEquals(AvatarShape.CIRCLE, HandshakeCodec.decode(json).avatarShape);
    }

    @Test
    public void legacyEscapedQuoteIsParsedCorrectly() {
        String legacy = "{\"n\":\"Alice \\\"The Great\\\"\",\"l\":\"en\"}";

        assertEquals("Alice \"The Great\"", HandshakeCodec.decode(legacy).name);
    }

    @Test
    public void nullInputUsesDefaults() {
        assertEquals(HandshakeCodec.DEFAULT_NAME, HandshakeCodec.decode(null).name);
    }
}
