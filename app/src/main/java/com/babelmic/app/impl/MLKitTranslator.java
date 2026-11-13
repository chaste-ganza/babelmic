package com.babelmic.app.impl;

import android.util.Log;

import androidx.annotation.NonNull;

import com.babelmic.app.core.Translator;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real implementation of {@link Translator} using Google ML Kit on-device translation.
 *
 * <p><b>Key characteristics:</b>
 * <ul>
 *   <li>No internet required after initial model download (~50 MB per language pair).</li>
 *   <li>Models are downloaded on first use with Wi-Fi only by default.</li>
 *   <li>Internally caches one {@link com.google.mlkit.nl.translate.Translator} per
 *       source→target pair to avoid repeated initialisation overhead.</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage:</b> Call {@link #prepareModel(String, String, ModelReadyCallback)} before
 * the first translation so the UI can show a download-progress indicator. Then call
 * {@link #translate(String, String, String)} freely from any thread.</p>
 */
public class MLKitTranslator implements Translator {

    private static final String TAG = "MLKitTranslator";
    private static final long TRANSLATE_TIMEOUT_SECONDS = 15;
    private static final long DOWNLOAD_TIMEOUT_SECONDS  = 120;

    /** Callback for the async model-download step. */
    public interface ModelReadyCallback {
        void onModelReady();
        void onModelDownloadFailed(Exception e);
    }

    // Active ML Kit translator — replaced whenever the language pair changes
    private com.google.mlkit.nl.translate.Translator mlkitTranslator;
    private String cachedSourceLang = "";
    private String cachedTargetLang = "";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Downloads (if needed) and warms up the translation model for the given language pair.
     * Call this on the ConversationActivity's onCreate so the model is ready by the time
     * the user starts speaking.
     *
     * @param sourceLang BCP-47 tag e.g. "en"
     * @param targetLang BCP-47 tag e.g. "fr"
     * @param callback   Notified on success or failure (called on a background thread)
     */
    public void prepareModel(@NonNull String sourceLang,
                             @NonNull String targetLang,
                             @NonNull ModelReadyCallback callback) {
        executor.execute(() -> {
            try {
                ensureTranslator(sourceLang, targetLang);

                DownloadConditions conditions = new DownloadConditions.Builder()
                        .requireWifi()
                        .build();

                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Exception> error = new AtomicReference<>();

                mlkitTranslator.downloadModelIfNeeded(conditions)
                        .addOnSuccessListener(unused -> latch.countDown())
                        .addOnFailureListener(e -> {
                            error.set(e);
                            latch.countDown();
                        });

                latch.await(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (error.get() != null) {
                    callback.onModelDownloadFailed(error.get());
                } else {
                    callback.onModelReady();
                }

            } catch (Exception e) {
                Log.e(TAG, "prepareModel failed", e);
                callback.onModelDownloadFailed(e);
            }
        });
    }

    /**
     * Translates text synchronously (blocks the calling thread).
     * <b>Do not call on the main/UI thread.</b>
     *
     * @param text       Text to translate
     * @param sourceLang BCP-47 source language tag (e.g. "en")
     * @param targetLang BCP-47 target language tag (e.g. "fr")
     * @return Translated string, or the original text on failure
     */
    @Override
    public String translate(@NonNull String text,
                            @NonNull String sourceLang,
                            @NonNull String targetLang) {
        if (text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to translate cannot be empty.");
        }

        try {
            ensureTranslator(sourceLang, targetLang);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> result = new AtomicReference<>(text);

            mlkitTranslator.translate(text)
                    .addOnSuccessListener(translated -> {
                        result.set(translated);
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Translation failed", e);
                        latch.countDown(); // result stays as original text
                    });

            latch.await(TRANSLATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return result.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Translation interrupted", e);
            return text;
        }
    }

    /**
     * Releases the underlying ML Kit translator.
     * Call from the host Activity's onDestroy.
     */
    public void close() {
        if (mlkitTranslator != null) {
            mlkitTranslator.close();
            mlkitTranslator = null;
        }
        executor.shutdown();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Lazily creates (or replaces) the ML Kit translator when the language pair changes.
     */
    private synchronized void ensureTranslator(String sourceLang, String targetLang) {
        if (sourceLang.equals(cachedSourceLang) && targetLang.equals(cachedTargetLang)
                && mlkitTranslator != null) {
            return; // already set up for this pair
        }

        if (mlkitTranslator != null) {
            mlkitTranslator.close();
        }

        String mlkitSource = TranslateLanguage.fromLanguageTag(sourceLang);
        String mlkitTarget = TranslateLanguage.fromLanguageTag(targetLang);

        if (mlkitSource == null || mlkitTarget == null) {
            throw new IllegalArgumentException(
                    "Unsupported language tag: " + sourceLang + " → " + targetLang);
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(mlkitSource)
                .setTargetLanguage(mlkitTarget)
                .build();

        mlkitTranslator = Translation.getClient(options);
        cachedSourceLang = sourceLang;
        cachedTargetLang = targetLang;

        Log.d(TAG, "Translator created for: " + sourceLang + " → " + targetLang);
    }
}
