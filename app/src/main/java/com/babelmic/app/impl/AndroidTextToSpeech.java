package com.babelmic.app.impl;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.babelmic.app.core.TextToSpeech;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
     * Queues the utterance — if another utterance is already playing it will
     * play afterward (QUEUE_ADD behaviour).
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

        Locale locale = resolveLocale(language);
        int setLangResult = tts.setLanguage(locale);

        if (setLangResult == android.speech.tts.TextToSpeech.LANG_MISSING_DATA
                || setLangResult == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported: " + language + " — falling back to default locale");
            tts.setLanguage(Locale.getDefault());
        }

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
