package com.babelmic.app.ui.widget;

import com.babelmic.app.model.AvatarShape;

/**
 * Calculates the normalised height envelope [0.0 – 1.0] for each bar
 * in the avatar visualizer, given a shape and bar index.
 *
 * <p>The envelope defines the MAXIMUM height a bar can reach when amplitude
 * is at full scale. At idle all bars collapse to zero (dots). During speech
 * each bar's actual height = envelope[i] × currentAmplitude.</p>
 *
 * <p>All shapes use the same number of bars ({@link #BAR_COUNT}) and are
 * symmetric around the horizontal center axis — bars grow both upward and
 * downward by half the computed height.</p>
 */
public final class ShapeEnvelopeCalculator {

    /** Number of vertical bars in every avatar. */
    public static final int BAR_COUNT = 14;

    private ShapeEnvelopeCalculator() {}

    /**
     * Returns a float array of length {@link #BAR_COUNT} where each value
     * is in [0.0, 1.0] and represents the normalised maximum half-height
     * of that bar for the given shape.
     *
     * @param shape The avatar shape
     * @return Envelope array, index 0 = leftmost bar
     */
    public static float[] compute(AvatarShape shape) {
        switch (shape) {
            case HEART:     return heart();
            case STAR:      return star();
            case CIRCLE:    return circle();
            case RECTANGLE: return rectangle();
            default:        return circle();
        }
    }

    // ─── Shape definitions ────────────────────────────────────────────────────
    // Each array has BAR_COUNT entries.
    // Values are hand-tuned to match the reference image:
    //   heart  — two humps with a dip in the middle, tapering at both ends
    //   star   — 5-point star cross-section: tall centre, medium tips, short valleys
    //   circle — smooth bell curve
    //   rect   — flat top, uniform height

    private static float[] heart() {
        // Mirror: left hump | dip | right hump, with pointed bottom taper
        return new float[]{
            0.30f, 0.62f, 0.85f, 0.95f, 0.88f,  // left lobe rising
            0.65f,                                 // centre dip
            0.88f, 0.95f, 0.85f, 0.62f, 0.30f,  // right lobe falling
            0.18f, 0.08f, 0.02f                   // bottom point taper (right side)
        };
        // Note: rendered left→right so the visual dip lands in the middle
    }

    private static float[] star() {
        // 5-point star cross-section: alternating tall tips and shallow valleys
        // Outer tips at positions 0, 3, 6 (centre), 9, 13; valleys between
        return new float[]{
            0.55f, 0.30f, 0.50f,   // left tip + valley + left-centre tip
            0.75f, 0.40f, 0.65f,   // rising to centre
            1.00f,                  // tallest centre bar
            0.65f, 0.40f, 0.75f,   // falling from centre
            0.50f, 0.30f, 0.55f,   // right-centre tip + valley + right tip
            0.20f                   // trailing edge
        };
    }

    private static float[] circle() {
        // Smooth sine-based bell curve — tallest in the centre
        float[] env = new float[BAR_COUNT];
        for (int i = 0; i < BAR_COUNT; i++) {
            // Map index to angle 0…π for a half-sine
            double angle = Math.PI * i / (BAR_COUNT - 1);
            env[i] = (float) Math.sin(angle);
        }
        return env;
    }

    private static float[] rectangle() {
        // Flat uniform height — all bars the same
        float[] env = new float[BAR_COUNT];
        for (int i = 0; i < BAR_COUNT; i++) {
            // Slight taper at the very edges to avoid harsh corners
            if (i == 0 || i == BAR_COUNT - 1)        env[i] = 0.70f;
            else if (i == 1 || i == BAR_COUNT - 2)   env[i] = 0.90f;
            else                                       env[i] = 1.00f;
        }
        return env;
    }
}
