package com.babelmic.app.impl;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.util.Log;

import com.babelmic.app.core.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real implementation of {@link SpeechRecognizer} using Android's on-device
 * speech recognition engine. No internet required on most modern Android devices
 * for common languages (English, French, Spanish, etc.).
 *
 * <p>This class wraps Android's {@link android.speech.SpeechRecognizer} with a
 * synchronous-style callback so callers don't need to manage raw RecognitionListeners.
 * Under the hood it still uses the async Android API and blocks via a CountDownLatch.</p>
 *
 * <p><b>Usage:</b> Always call {@link #destroy()} when done to release system resources.</p>
 */
public class AndroidSpeechRecognizer implements SpeechRecognizer {

    private static final String TAG = "AndroidSTT";
    private static final long RECOGNITION_TIMEOUT_SECONDS = 10;

    private final Context context;
    // BCP-47 language tag for the language the local user speaks
    private String languageTag;

    // The underlying Android recognizer — recreated per session on the main thread
    private android.speech.SpeechRecognizer androidRecognizer;

    /** Listener interface so the UI can react to live recognition events. */
    public interface SpeechCallback {
        /** Called when the microphone has started capturing audio. */
        void onListeningStarted();
        /** Called with partial (interim) recognition results as the user speaks. */
        void onPartialResult(String partial);
        /** Called with the final recognised text. */
        void onResult(String text);
        /** Called when recognition ends with an error. */
        void onError(String errorMessage);
    }

    private SpeechCallback callback;

    public AndroidSpeechRecognizer(Context context, String languageTag) {
        this.context = context.getApplicationContext();
        this.languageTag = languageTag;
    }

    public void setLanguageTag(String languageTag) {
        this.languageTag = languageTag;
    }

    public void setCallback(SpeechCallback callback) {
        this.callback = callback;
    }

    /**
     * Starts listening and returns the recognised text synchronously.
     * <p><b>Must be called from a background thread</b> — it blocks until a result
     * arrives or the timeout elapses.</p>
     *
     * @param audioSource  Ignored in the real implementation (mic is always the source).
     *                     Kept to satisfy the {@link SpeechRecognizer} interface.
     * @return The recognised text, or an empty string on timeout/error.
     */
    @Override
    public String recognizeSpeech(String audioSource) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>("");

        // SpeechRecognizer must run on the main thread
        android.os.Handler mainHandler = new android.os.Handler(
                android.os.Looper.getMainLooper());

        mainHandler.post(() -> {
            androidRecognizer = android.speech.SpeechRecognizer
                    .createSpeechRecognizer(context);

            androidRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    if (callback != null) callback.onListeningStarted();
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "Speech input started");
                }

                @Override
                public void onRmsChanged(float rmsdB) { /* volume indicator — ignored */ }

                @Override
                public void onBufferReceived(byte[] buffer) { /* raw audio buffer — ignored */ }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "Speech input ended");
                }

                @Override
                public void onError(int error) {
                    String msg = errorCodeToMessage(error);
                    Log.e(TAG, "Recognition error: " + msg);
                    if (callback != null) callback.onError(msg);
                    result.set("");
                    latch.countDown();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(
                            android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                    String text = (matches != null && !matches.isEmpty())
                            ? matches.get(0) : "";
                    Log.d(TAG, "Final result: " + text);
                    result.set(text);
                    if (callback != null) callback.onResult(text);
                    latch.countDown();
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> partial = partialResults.getStringArrayList(
                            android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                    if (partial != null && !partial.isEmpty() && callback != null) {
                        callback.onPartialResult(partial.get(0));
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) { /* unused */ }
            });

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag);
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageTag);
            // Request partial results so the UI shows live text
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            // Longer silence before timeout
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);

            androidRecognizer.startListening(intent);
        });

        try {
            latch.await(RECOGNITION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Recognition interrupted", e);
        }

        return result.get();
    }

    /**
     * Programmatically stop listening (e.g. when the user releases the PTT button).
     * Triggers the normal {@code onResults} callback path.
     */
    public void stopListening() {
        android.os.Handler mainHandler = new android.os.Handler(
                android.os.Looper.getMainLooper());
        mainHandler.post(() -> {
            if (androidRecognizer != null) {
                androidRecognizer.stopListening();
            }
        });
    }

    /** Release the underlying Android SpeechRecognizer. Call on activity destroy. */
    public void destroy() {
        android.os.Handler mainHandler = new android.os.Handler(
                android.os.Looper.getMainLooper());
        mainHandler.post(() -> {
            if (androidRecognizer != null) {
                androidRecognizer.destroy();
                androidRecognizer = null;
            }
        });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String errorCodeToMessage(int errorCode) {
        switch (errorCode) {
            case android.speech.SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case android.speech.SpeechRecognizer.ERROR_CLIENT:
                return "Client-side error";
            case android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Microphone permission not granted";
            case android.speech.SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case android.speech.SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match found";
            case android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case android.speech.SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech detected";
            default:
                return "Unknown error (code " + errorCode + ")";
        }
    }
}
