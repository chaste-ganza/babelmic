package com.babelmic.app.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.babelmic.app.R;

/**
 * Small circular avatar badge with an identity-colour ring.
 * Used in the top-right corner of Language, Discovery, Pending, Incoming, and Conversation screens.
 *
 * Usage:
 *   badge.set(R.drawable.avatar_bird, 0xFFFF2D78);
 */
public class AvatarBadgeView extends FrameLayout {

    private ImageView ivAvatar;
    private ImageView ivRing;

    public AvatarBadgeView(@NonNull Context context) {
        super(context); init(context);
    }
    public AvatarBadgeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init(context);
    }
    public AvatarBadgeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_avatar_badge, this);
        ivAvatar = findViewById(R.id.iv_badge_avatar);
        ivRing   = findViewById(R.id.iv_badge_ring);
    }

    /** @param avatarResId  Drawable resource of the animal avatar image
     *  @param ringColor    ARGB identity colour for the ring */
    public void set(int avatarResId, int ringColor) {
        ivAvatar.setImageResource(avatarResId);
        // Ring is set via android:src — tint it with the identity colour
        android.graphics.drawable.Drawable d = ivRing.getDrawable();
        if (d != null) {
            d = d.mutate();
            d.setColorFilter(ringColor, android.graphics.PorterDuff.Mode.SRC_IN);
            ivRing.setImageDrawable(d);
        }
    }
}
