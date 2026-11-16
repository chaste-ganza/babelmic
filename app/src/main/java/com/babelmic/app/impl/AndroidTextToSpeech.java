package com.babelmic.app.impl;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import com.babelmic.app.core.TextToSpeech;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real implementation of {@link TextToSpeech} wrapping Android's built-in
 * {@link android.speech.tts.TextToSpeech} engine.
 *
 * <p><b>Key characteristics:</b>
 * <ul>
 *   <li>Fully on-device — no network call required.</li>
 *   <li>Supports any language installed on the device; falls back gracefully.</li>
 *   <li>Thread-safe: speak() can be called from any thread.</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage:</b> Instantiate once and reuse across the conversation. Call
 * {@link #shutdown()} when the hosting Activity is destroyed.</p>
 */
public class AndroidTextToSpeech implements TextToSpeech {

    private static final String TAG = "AndroidTTS";

    /** Callback so the UI knows when an utterance has finished playing. */
    public interface SpeechDoneCallback {
        void onSpeechDone(String utteranceId);
        void onSpeechError(String utteranceId, String errorMessage);
    }

    private android.speech.tts.TextToSpeech tts;
    private final AtomicBoolean isReady = new AtomicBoolean(false);
    private SpeechDoneCallback doneCallback;

    // Cache: language tag → best voice found for that language (null = not yet searched)
    private final Map<String, android.speech.tts.Voice> voiceCache = new HashMap<>();
    // Cache of language tag → Locale to avoid repeated lookups
    private final Map<String, Locale> localeCache = new HashMap<>();

    public AndroidTextToSpeech(Context context) {
        tts = new android.speech.tts.TextToSpeech(context.getApplicationContext(), status -> {
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                isReady.set(true);
                Log.d(TAG, "TTS engine initialised successfully");

                // Global utterance progress listener
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "Speaking utterance: " + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        if (doneCallback != null) doneCallback.onSpeechDone(utteranceId);
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(TAG, "TTS error for utterance: " + utteranceId);
                        if (doneCallback != null)
                            doneCallback.onSpeechError(utteranceId, "TTS engine error");
                    }
                });

            } else {
                Log.e(TAG, "TTS engine initialisation failed with status: " + status);
            }
        });
    }

    public void setDoneCallback(SpeechDoneCallback doneCallback) {
        this.doneCallback = doneCallback;
    }

    /**
     * Speak the given text in the specified language.
     * Selects the best available voice for that language automatically:
     * prefers network-quality voices when available, then local high-quality,
     * then any voice for that locale — always better than the default.
     *
     * @param text     Text to speak. Must not be null or empty.
     * @param language BCP-47 language tag, e.g. "fr", "en", "es".
     */
    @Override
    public void speak(String text, String language) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to speak cannot be empty.");
        }

        if (!isReady.get()) {
            Log.w(TAG, "TTS engine not ready yet — utterance dropped: " + text);
            return;
        }

        // Apply the best voice for this language
        applyBestVoice(language);

        // Slightly slower speech rate makes translations easier to follow
        tts.setSpeechRate(0.9f);
        tts.setPitch(1.0f);

        String utteranceId = UUID.randomUUID().toString();
        Bundle params = new Bundle();
        params.putString(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                utteranceId);

        tts.speak(text,
                android.speech.tts.TextToSpeech.QUEUE_ADD,
                params,
                utteranceId);

        Log.d(TAG, "Queued utterance [" + language + "]: " + text);
    }

    /**
     * Applies the best available TTS voice for the given language tag.
     * Preference order:
     *   1. Network-quality voice (highest naturalness, needs internet for first use)
     *   2. Local high-quality voice
     *   3. Any voice that matches the language locale
     *   4. Fallback: setLanguage() with the resolved locale
     *
     * Results are cached so the voice search only runs once per language per session.
     */
    private void applyBestVoice(String languageTag) {
        // Check cache first
        if (voiceCache.containsKey(languageTag)) {
            Voice cached = voiceCache.get(languageTag);
            if (cached != null) {
                tts.setVoice(cached);
            } else {
                // Previously found no good voice — fall back to setLanguage
                tts.setLanguage(resolveLocale(languageTag));
            }
            return;
        }

        Locale targetLocale = resolveLocale(languageTag);
        Set<Voice> voices = tts.getVoices();

        if (voices == null || voices.isEmpty()) {
            Log.w(TAG, "No voices available from TTS engine");
            tts.setLanguage(targetLocale);
            voiceCache.put(languageTag, null);
            return;
        }

        // Filter to voices that match the target language
        List<Voice> matching = new ArrayList<>();
        for (Voice voice : voices) {
            Locale voiceLocale = voice.getLocale();
            if (voiceLocale != null
                    && voiceLocale.getLanguage().equalsIgnoreCase(targetLocale.getLanguage())
                    && !voice.isNetworkConnectionRequired()  // prefer local voices that work offline
                    ) {
                matching.add(voice);
            }
        }

        // Also collect network voices as fallback (better quality but needs internet)
        List<Voice> networkVoices = new ArrayList<>();
        for (Voice voice : voices) {
            Locale voiceLocale = voice.getLocale();
            if (voiceLocale != null
                    && voiceLocale.getLanguage().equalsIgnoreCase(targetLocale.getLanguage())
                    && voice.isNetworkConnectionRequired()) {
                networkVoices.add(voice);
            }
        }

        // Score voices: lower latency = better for local voices
        // For network voices: sort by quality (name often contains "enhanced"/"wavenet")
        Voice best = null;

        if (!matching.isEmpty()) {
            // Sort local voices: prefer those without network requirement and lower latency
            Collections.sort(matching, (a, b) -> {
                int aLatency = a.getLatency();
                int bLatency = b.getLatency();
                // Also prefer voices with "enhanced" or higher quality in the name
                boolean aEnhanced = a.getName().toLowerCase().contains("enhanced")
                        || a.getName().toLowerCase().contains("high");
                boolean bEnhanced = b.getName().toLowerCase().contains("enhanced")
                        || b.getName().toLowerCase().contains("high");
                if (aEnhanced != bEnhanced) return aEnhanced ? -1 : 1;
                return Integer.compare(aLatency, bLatency);
            });
            best = matching.get(0);
        } else if (!networkVoices.isEmpty()) {
            // Prefer network voice with "enhanced" or "wavenet" in name
            Collections.sort(networkVoices, (a, b) -> {
                boolean aEnhanced = a.getName().toLowerCase().contains("enhanced")
                        || a.getName().toLowerCase().contains("wavenet")
                        || a.getName().toLowerCase().contains("neural");
                boolean bEnhanced = b.getName().toLowerCase().contains("enhanced")
                        || b.getName().toLowerCase().contains("wavenet")
                        || b.getName().toLowerCase().contains("neural");
                if (aEnhanced != bEnhanced) return aEnhanced ? -1 : 1;
                return 0;
            });
            best = networkVoices.get(0);
        }

        if (best != null) {
            Log.d(TAG, "Selected voice for [" + languageTag + "]: " + best.getName()
                    + " | network=" + best.isNetworkConnectionRequired()
                    + " | latency=" + best.getLatency());
            tts.setVoice(best);
            voiceCache.put(languageTag, best);
        } else {
            Log.w(TAG, "No matching voice for language: " + languageTag
                    + " — falling back to setLanguage()");
            int result = tts.setLanguage(targetLocale);
            if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA
                    || result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language pack missing for: " + languageTag);
                tts.setLanguage(Locale.getDefault());
            }
            voiceCache.put(languageTag, null);
        }
    }

    /**
     * Immediately stop any ongoing speech and clear the utterance queue.
     */
    public void stop() {
        if (isReady.get()) {
            tts.stop();
        }
    }

    /** @return true if the TTS engine has finished initialising and is ready to speak. */
    public boolean isReady() {
        return isReady.get();
    }

    @Override
    public void shutdown() {
        tts.stop();
        tts.shutdown();
        isReady.set(false);
        Log.d(TAG, "TTS engine shut down");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Converts a BCP-47 language tag to an Android {@link Locale}.
     * Results are cached for performance.
     */
    private Locale resolveLocale(String languageTag) {
        return localeCache.computeIfAbsent(languageTag, tag -> {
            try {
                Locale locale = Locale.forLanguageTag(tag);
                // Locale.forLanguageTag returns an empty locale for unrecognised tags
                return (locale.getLanguage().isEmpty()) ? Locale.getDefault() : locale;
            } catch (Exception e) {
                Log.w(TAG, "Could not resolve locale for tag: " + tag);
                return Locale.getDefault();
            }
        });
    }
}
