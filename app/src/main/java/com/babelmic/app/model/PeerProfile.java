package com.babelmic.app.model;

/**
 * Immutable snapshot of a connected peer's profile, received during the
 * Wi-Fi Direct handshake.
 */
public class PeerProfile {
    public final String      name;
    public final String      languageTag;
    public final String      languageName;
    public final AvatarShape avatarShape;
    public final int         identityColor; // ring / wave colour
    public final int         colorTop;
    public final int         colorBottom;
    public final int         avatarResId;   // resolved drawable resource

    public PeerProfile(String name, String languageTag, String languageName,
                       AvatarShape avatarShape, int identityColor,
                       int colorTop, int colorBottom, int avatarResId) {
        this.name          = name;
        this.languageTag   = languageTag;
        this.languageName  = languageName;
        this.avatarShape   = avatarShape;
        this.identityColor = identityColor;
        this.colorTop      = colorTop;
        this.colorBottom   = colorBottom;
        this.avatarResId   = avatarResId;
    }
}
