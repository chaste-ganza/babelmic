package com.babelmic.app.util;

import com.babelmic.app.model.AvatarShape;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Encodes and decodes the small profile "business card" that two phones swap
 * when their Wi-Fi Direct socket connects.
 *
 * <p>Using a real JSON object means names containing quotes, backslashes,
 * commas or emoji survive the trip — unlike the old hand-built string.</p>
 */
public final class HandshakeCodec {

    public static final String DEFAULT_NAME       = "Partner";
    public static final String DEFAULT_LANG_TAG   = "en";
    public static final String DEFAULT_LANG_NAME  = "English";
    public static final AvatarShape DEFAULT_SHAPE = AvatarShape.CIRCLE;
    public static final String DEFAULT_AVATAR     = "";
    public static final int DEFAULT_IDENTITY_COLOR = 0xFFFF2D78;
    public static final int DEFAULT_COLOR_TOP     = 0xFF1A73E8;
    public static final int DEFAULT_COLOR_BOTTOM  = 0xFF4FC3F7;

    private HandshakeCodec() {}

    /** Builds the JSON profile sent during the handshake. */
    public static String encode(String name,
                                String languageTag,
                                String languageName,
                                String avatarShape,
                                String avatarName,
                                int identityColor,
                                int colorTop,
                                int colorBottom) {
        JSONObject json = new JSONObject();
        try {
            json.put("n",  name);
            json.put("l",  languageTag);
            json.put("ln", languageName);
            json.put("s",  avatarShape);
            json.put("an", avatarName);
            json.put("ic", identityColor);
            json.put("ct", colorTop);
            json.put("cb", colorBottom);
        } catch (JSONException e) {
            throw new IllegalStateException("Could not build handshake profile", e);
        }
        return json.toString();
    }

    /**
     * Parses a peer's profile. Missing or invalid values are replaced with
     * sensible defaults so one odd message can't break the conversation.
     */
    public static Parsed decode(String json) {
        try {
            JSONObject obj = new JSONObject(json);

            AvatarShape shape;
            try {
                shape = AvatarShape.valueOf(obj.optString("s", DEFAULT_SHAPE.name()));
            } catch (IllegalArgumentException e) {
                shape = DEFAULT_SHAPE;
            }

            return new Parsed(
                    obj.optString("n",  DEFAULT_NAME),
                    obj.optString("l",  DEFAULT_LANG_TAG),
                    obj.optString("ln", DEFAULT_LANG_NAME),
                    shape,
                    obj.optString("an", DEFAULT_AVATAR),
                    obj.optInt("ic", DEFAULT_IDENTITY_COLOR),
                    obj.optInt("ct", DEFAULT_COLOR_TOP),
                    obj.optInt("cb", DEFAULT_COLOR_BOTTOM));
        } catch (Exception e) {
            return new Parsed(DEFAULT_NAME, DEFAULT_LANG_TAG, DEFAULT_LANG_NAME,
                    DEFAULT_SHAPE, DEFAULT_AVATAR, DEFAULT_IDENTITY_COLOR,
                    DEFAULT_COLOR_TOP, DEFAULT_COLOR_BOTTOM);
        }
    }

    /** Raw decoded fields, before Android turns the avatar name into an image. */
    public static final class Parsed {
        public final String      name;
        public final String      languageTag;
        public final String      languageName;
        public final AvatarShape avatarShape;
        public final String      avatarName;
        public final int         identityColor;
        public final int         colorTop;
        public final int         colorBottom;

        public Parsed(String name, String languageTag, String languageName,
                      AvatarShape avatarShape, String avatarName,
                      int identityColor, int colorTop, int colorBottom) {
            this.name          = name;
            this.languageTag   = languageTag;
            this.languageName  = languageName;
            this.avatarShape   = avatarShape;
            this.avatarName    = avatarName;
            this.identityColor = identityColor;
            this.colorTop      = colorTop;
            this.colorBottom   = colorBottom;
        }
    }
}
