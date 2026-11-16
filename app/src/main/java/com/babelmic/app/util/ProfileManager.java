package com.babelmic.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import com.babelmic.app.model.AvatarShape;

/**
 * Persists and retrieves the user's profile using {@link SharedPreferences}.
 *
 * <p>Profile data:
 * <ul>
 *   <li>Display name</li>
 *   <li>Chosen language BCP-47 tag</li>
 *   <li>Avatar shape ({@link AvatarShape})</li>
 *   <li>Two avatar colours (top gradient, bottom gradient)</li>
 * </ul>
 * </p>
 *
 * <p>All methods are static — pass the application context once per call.</p>
 */
public final class ProfileManager {

    private static final String PREFS_NAME       = "babelmic_profile";

    // ─── Profile version ─────────────────────────────────────────────────────
    // Bump this whenever the profile schema changes in a breaking way.
    // On mismatch the stored profile is wiped and the user goes through onboarding.
    private static final int    CURRENT_PROFILE_VERSION = 2; // 2 = avatar image system
    private static final String KEY_PROFILE_VERSION     = "profile_version";

    private static final String KEY_NAME         = "display_name";
    private static final String KEY_LANGUAGE_TAG = "language_tag";
    private static final String KEY_LANGUAGE_NAME= "language_name";
    private static final String KEY_SHAPE        = "avatar_shape";
    private static final String KEY_COLOR_TOP    = "avatar_color_top";
    private static final String KEY_COLOR_BOTTOM = "avatar_color_bottom";
    private static final String KEY_AVATAR_RES   = "avatar_res_id";
    private static final String KEY_SETUP_DONE   = "setup_complete";

    // Defaults
    private static final String DEFAULT_NAME     = "";
    private static final String DEFAULT_LANG_TAG = "en";
    private static final String DEFAULT_LANG_NAME= "English";
    private static final int    DEFAULT_COLOR_TOP    = Color.parseColor("#1A73E8");
    private static final int    DEFAULT_COLOR_BOTTOM = Color.parseColor("#4FC3F7");

    private ProfileManager() {}

    // ─── Setup complete flag ──────────────────────────────────────────────────

    /**
     * Checks if setup is complete AND the saved profile version matches the
     * current app version. If versions differ, clears stale data automatically.
     *
     * @return true if the profile is valid and setup should be skipped.
     */
    public static boolean isSetupComplete(Context ctx) {
        int savedVersion = prefs(ctx).getInt(KEY_PROFILE_VERSION, 0);
        if (savedVersion != CURRENT_PROFILE_VERSION) {
            // Profile from an older schema — clear it and force re-onboarding
            clearProfile(ctx);
            return false;
        }
        return prefs(ctx).getBoolean(KEY_SETUP_DONE, false);
    }

    /**
     * Wipes all stored profile data. The user will see the full onboarding flow
     * on next launch.
     */
    public static void clearProfile(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }

    /** Mark profile setup as complete with the current version stamp. */
    public static void markSetupComplete(Context ctx) {
        prefs(ctx).edit()
                .putBoolean(KEY_SETUP_DONE, true)
                .putInt(KEY_PROFILE_VERSION, CURRENT_PROFILE_VERSION)
                .apply();
    }

    /**
     * Ensures a valid profile exists, creating defaults if setup was never run.
     * Call this from the first screen that depends on profile data
     * (e.g. LanguageSelectionActivity or DeviceDiscoveryActivity).
     *
     * Defaults:
     *   name       = "User"
     *   shape      = CIRCLE
     *   colorTop   = #1A73E8 (Google Blue)
     *   colorBottom= #4FC3F7 (Light Blue)
     *   language   = preserved as-is (set when user picks language)
     */
    public static void ensureDefaultProfile(Context ctx) {
        // Only create silent defaults if setup has NEVER been done.
        // Do NOT mark setup as complete — that must only happen after the user
        // explicitly completes onboarding (Username + Avatar + Language screens).
        if (isSetupComplete(ctx)) return;
        prefs(ctx).edit()
                .putString(KEY_NAME,         "User")
                .putString(KEY_SHAPE,        AvatarShape.CIRCLE.name())
                .putInt(KEY_COLOR_TOP,       DEFAULT_COLOR_TOP)
                .putInt(KEY_COLOR_BOTTOM,    DEFAULT_COLOR_BOTTOM)
                // Do NOT set KEY_SETUP_DONE — user hasn't completed onboarding yet
                .apply();
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public static String getDisplayName(Context ctx) {
        return prefs(ctx).getString(KEY_NAME, DEFAULT_NAME);
    }

    public static String getLanguageTag(Context ctx) {
        return prefs(ctx).getString(KEY_LANGUAGE_TAG, DEFAULT_LANG_TAG);
    }

    public static String getLanguageName(Context ctx) {
        return prefs(ctx).getString(KEY_LANGUAGE_NAME, DEFAULT_LANG_NAME);
    }

    public static AvatarShape getAvatarShape(Context ctx) {
        String name = prefs(ctx).getString(KEY_SHAPE, AvatarShape.HEART.name());
        try {
            return AvatarShape.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AvatarShape.HEART;
        }
    }

    public static int getColorTop(Context ctx) {
        return prefs(ctx).getInt(KEY_COLOR_TOP, DEFAULT_COLOR_TOP);
    }

    public static int getColorBottom(Context ctx) {
        return prefs(ctx).getInt(KEY_COLOR_BOTTOM, DEFAULT_COLOR_BOTTOM);
    }

    /** Resource ID of the selected avatar drawable, e.g. R.drawable.avatar_1 */
    public static int getAvatarResId(Context ctx) {
        return prefs(ctx).getInt(KEY_AVATAR_RES, 0);
    }

    // ─── Setters ─────────────────────────────────────────────────────────────

    public static void setDisplayName(Context ctx, String name) {
        prefs(ctx).edit().putString(KEY_NAME, name.trim()).apply();
    }

    public static void setLanguage(Context ctx, String tag, String displayName) {
        prefs(ctx).edit()
                .putString(KEY_LANGUAGE_TAG, tag)
                .putString(KEY_LANGUAGE_NAME, displayName)
                .apply();
    }

    public static void setAvatarShape(Context ctx, AvatarShape shape) {
        prefs(ctx).edit().putString(KEY_SHAPE, shape.name()).apply();
    }

    public static void setColors(Context ctx, int colorTop, int colorBottom) {
        prefs(ctx).edit()
                .putInt(KEY_COLOR_TOP, colorTop)
                .putInt(KEY_COLOR_BOTTOM, colorBottom)
                .apply();
    }

    public static void setAvatarResId(Context ctx, int resId) {
        prefs(ctx).edit().putInt(KEY_AVATAR_RES, resId).apply();
    }

    /**
     * Save the entire profile in one atomic write.
     */
    public static void saveProfile(Context ctx,
                                   String name,
                                   String languageTag,
                                   String languageName,
                                   AvatarShape shape,
                                   int colorTop,
                                   int colorBottom) {
        prefs(ctx).edit()
                .putString(KEY_NAME,          name.trim())
                .putString(KEY_LANGUAGE_TAG,  languageTag)
                .putString(KEY_LANGUAGE_NAME, languageName)
                .putString(KEY_SHAPE,         shape.name())
                .putInt(KEY_COLOR_TOP,        colorTop)
                .putInt(KEY_COLOR_BOTTOM,     colorBottom)
                .putBoolean(KEY_SETUP_DONE,   true)
                .putInt(KEY_PROFILE_VERSION,  CURRENT_PROFILE_VERSION)
                .apply();
    }

    /**
     * Builds a compact JSON string of this profile suitable for sending over
     * the Wi-Fi Direct socket during the connection handshake.
     *
     * <p>Format: {@code {"n":"Alice","l":"en","s":"HEART","ct":-16711681,"cb":-12236160}}</p>
     */
    public static String toHandshakeJson(Context ctx) {
        return "{"
                + "\"n\":\"" + escape(getDisplayName(ctx)) + "\","
                + "\"l\":\""  + getLanguageTag(ctx)        + "\","
                + "\"ln\":\"" + escape(getLanguageName(ctx))+ "\","
                + "\"s\":\""  + getAvatarShape(ctx).name() + "\","
                + "\"ct\":"   + getColorTop(ctx)           + ","
                + "\"cb\":"   + getColorBottom(ctx)
                + "}";
    }

    /**
     * Parse a handshake JSON string received from the peer into a {@link PeerProfile}.
     * Returns a default profile on any parse error.
     */
    public static PeerProfile fromHandshakeJson(String json) {
        try {
            String name     = extractString(json, "n",  "Partner");
            String langTag  = extractString(json, "l",  "en");
            String langName = extractString(json, "ln", "English");
            String shapeName= extractString(json, "s",  "CIRCLE");
            int    ct       = extractInt(json,    "ct", Color.parseColor("#1A73E8"));
            int    cb       = extractInt(json,    "cb", Color.parseColor("#4FC3F7"));

            AvatarShape shape;
            try { shape = AvatarShape.valueOf(shapeName); }
            catch (Exception e) { shape = AvatarShape.CIRCLE; }

            return new PeerProfile(name, langTag, langName, shape, ct, cb);
        } catch (Exception e) {
            return new PeerProfile("Partner", "en", "English",
                    AvatarShape.CIRCLE,
                    Color.parseColor("#1A73E8"),
                    Color.parseColor("#4FC3F7"));
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"").replace("\\", "\\\\");
    }

    private static String extractString(String json, String key, String fallback) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return fallback;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return fallback;
        return json.substring(start, end);
    }

    private static int extractInt(String json, String key, int fallback) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return fallback;
        start += search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '-')) {
            end++;
        }
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (Exception e) { return fallback; }
    }

    // ─── PeerProfile ─────────────────────────────────────────────────────────

    /**
     * Immutable snapshot of a connected peer's profile received during handshake.
     */
    public static class PeerProfile {
        public final String      name;
        public final String      languageTag;
        public final String      languageName;
        public final AvatarShape avatarShape;
        public final int         colorTop;
        public final int         colorBottom;

        public PeerProfile(String name, String languageTag, String languageName,
                           AvatarShape avatarShape, int colorTop, int colorBottom) {
            this.name         = name;
            this.languageTag  = languageTag;
            this.languageName = languageName;
            this.avatarShape  = avatarShape;
            this.colorTop     = colorTop;
            this.colorBottom  = colorBottom;
        }
    }
}
