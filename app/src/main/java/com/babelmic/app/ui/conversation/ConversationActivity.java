package com.babelmic.app.ui.conversation;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.babelmic.app.databinding.ActivityConversationBinding;
import com.babelmic.app.impl.AndroidSpeechRecognizer;
import com.babelmic.app.impl.AndroidTextToSpeech;
import com.babelmic.app.impl.MLKitTranslator;
import com.babelmic.app.model.Language;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.model.TranslationMessage;
import com.babelmic.app.network.WifiDirectPeerConnection;
import com.babelmic.app.util.LanguageManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The core screen of BabelMic — the live translation conversation view.
 *
 * <p><b>Pipeline (outgoing — this user speaks):</b>
 * <pre>
 *   Hold mic button
 *       → AndroidSpeechRecognizer  (STT — on device)
 *       → MLKitTranslator          (translate to peer's language — on device)
 *       → WifiDirectPeerConnection  (send translated text to peer)
 *       → ConversationAdapter       (show in local chat)
 * </pre>
 * </p>
 *
 * <p><b>Pipeline (incoming — peer speaks):</b>
 * <pre>
 *   WifiDirectPeerConnection receives data
 *       → AndroidTextToSpeech      (speak aloud in my language — on device)
 *       → ConversationAdapter      (show in local chat)
 * </pre>
 * </p>
 *
 * <p><b>Important:</b> Both devices in the session already know each other's
 * language because the translated text received over Wi-Fi Direct is already in
 * this device's language — it was translated by the peer before sending.</p>
 */
public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = "ConversationActivity";

    // ─── View binding ─────────────────────────────────────────────────────────
    private ActivityConversationBinding binding;

    // ─── Core pipeline components ─────────────────────────────────────────────
    private AndroidSpeechRecognizer speechRecognizer;
    private MLKitTranslator         translator;
    private AndroidTextToSpeech     textToSpeech;
    private WifiDirectPeerConnection peerConnection;

    // ─── UI ───────────────────────────────────────────────────────────────────
    private ConversationAdapter conversationAdapter;

    // ─── Session state ────────────────────────────────────────────────────────
    private Language myLanguage;
    private boolean  isGroupOwner;
    private String   peerIpAddress;

    // ─── Background work ─────────────────────────────────────────────────────
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConversationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        readSessionConfig();
        setupToolbar();
        setupConversationList();
        initPipeline();
        setupMicButton();
        setupDisconnectButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up all pipeline resources
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (translator       != null) translator.close();
        if (textToSpeech     != null) textToSpeech.shutdown();
        if (peerConnection   != null) peerConnection.disconnect();
        executor.shutdown();
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void readSessionConfig() {
        String tag  = getIntent().getStringExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG);
        myLanguage  = LanguageManager.findByTag(tag);
        if (myLanguage == null) {
            // Fallback — should not happen in normal flow
            myLanguage = LanguageManager.findByTag("en");
        }
        isGroupOwner  = getIntent().getBooleanExtra(SessionConfig.EXTRA_IS_GROUP_OWNER, false);
        peerIpAddress = getIntent().getStringExtra(SessionConfig.EXTRA_PEER_IP_ADDRESS);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                    myLanguage.getFlagEmoji() + "  " + myLanguage.getDisplayName());
        }
        binding.tvConnectionStatus.setText(
                isGroupOwner ? "Session host" : "Connected to partner");
    }

    private void setupConversationList() {
        conversationAdapter = new ConversationAdapter();
        binding.rvConversation.setLayoutManager(new LinearLayoutManager(this));
        binding.rvConversation.setAdapter(conversationAdapter);
    }

    private void initPipeline() {
        // 1. Speech recogniser — listens in MY language
        speechRecognizer = new AndroidSpeechRecognizer(this, myLanguage.getBcp47Tag());
        speechRecognizer.setCallback(new AndroidSpeechRecognizer.SpeechCallback() {
            @Override public void onListeningStarted() {
                runOnUiThread(() -> setMicState(MicState.LISTENING));
            }
            @Override public void onPartialResult(String partial) {
                runOnUiThread(() -> binding.tvLiveCaption.setText(partial));
            }
            @Override public void onResult(String text) {
                runOnUiThread(() -> {
                    binding.tvLiveCaption.setText("");
                    setMicState(MicState.PROCESSING);
                });
                if (!text.isEmpty()) {
                    translateAndSend(text);
                } else {
                    runOnUiThread(() -> setMicState(MicState.IDLE));
                }
            }
            @Override public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setMicState(MicState.IDLE);
                    Toast.makeText(ConversationActivity.this,
                            "Speech error: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });

        // 2. Translator
        translator = new MLKitTranslator();
        // Pre-download model in background (we don't know peer's language at this point
        // so the model download happens on first translation; shown via PROCESSING state)

        // 3. Text-to-speech — speaks in MY language (incoming messages are already translated)
        textToSpeech = new AndroidTextToSpeech(this);

        // 4. Peer connection — socket
        peerConnection = new WifiDirectPeerConnection();
        peerConnection.setDataCallback(new WifiDirectPeerConnection.DataReceivedCallback() {
            @Override
            public void onDataReceived(String data) {
                // Incoming data is already translated into MY language by the peer
                handleIncomingMessage(data);
            }
            @Override
            public void onConnectionLost(String reason) {
                runOnUiThread(() -> {
                    binding.tvConnectionStatus.setText("Connection lost");
                    Toast.makeText(ConversationActivity.this,
                            "Connection lost: " + reason, Toast.LENGTH_LONG).show();
                });
            }
        });

        // 5. Start socket in background
        executor.execute(() -> {
            if (isGroupOwner) {
                peerConnection.startServer();
            } else {
                boolean ok = peerConnection.connect(peerIpAddress);
                if (!ok) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Could not connect to partner. Try going back and reconnecting.",
                            Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void setupMicButton() {
        binding.btnMic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startListening();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopListening();
                    return true;
            }
            return false;
        });
    }

    private void setupDisconnectButton() {
        binding.btnDisconnect.setOnClickListener(v -> {
            if (peerConnection != null) peerConnection.disconnect();
            finish();
        });
    }

    // ─── Push-to-talk ─────────────────────────────────────────────────────────

    private void startListening() {
        setMicState(MicState.LISTENING);
        executor.execute(() -> speechRecognizer.recognizeSpeech("mic"));
    }

    private void stopListening() {
        speechRecognizer.stopListening();
        // Result arrives via SpeechCallback.onResult()
    }

    // ─── Outgoing pipeline ────────────────────────────────────────────────────

    /**
     * Translate the recognised text into the peer's language and send it.
     * We don't know the peer's language tag in this design — the translation
     * happens on the SENDING side. We need to encode the peer's language in the
     * message or negotiate it at connection time.
     *
     * <p>For the prototype, we embed a simple wire format:
     * {@code LANG:<peerLang>|<translatedText>}
     * The peer's language tag is encoded in the payload so the receiving side
     * can speak it in the right language.</p>
     *
     * <p>Since we don't yet have a handshake to exchange language tags, the peer's
     * language is currently unknown to this device. To solve this, we send the
     * raw recognised text prefixed with this device's language tag, and let the
     * RECEIVING side do the translation instead.</p>
     *
     * <p>Wire format sent: {@code FROM:<myLangTag>|<originalText>}</p>
     */
    private void translateAndSend(String originalText) {
        executor.execute(() -> {
            try {
                // Send: FROM:<myLangTag>|<originalText>
                // The peer will translate FROM myLangTag INTO their language
                String payload = "FROM:" + myLanguage.getBcp47Tag() + "|" + originalText;

                if (peerConnection.isConnected()) {
                    peerConnection.sendData(payload);
                }

                // Show outgoing message in local UI (original + "sent" indicator)
                TranslationMessage msg = new TranslationMessage(
                        originalText, "(sent to partner)", TranslationMessage.Direction.OUTGOING);
                runOnUiThread(() -> {
                    conversationAdapter.addMessage(msg);
                    scrollToBottom();
                    setMicState(MicState.IDLE);
                });

            } catch (Exception e) {
                Log.e(TAG, "translateAndSend failed", e);
                runOnUiThread(() -> setMicState(MicState.IDLE));
            }
        });
    }

    // ─── Incoming pipeline ────────────────────────────────────────────────────

    /**
     * Process an incoming wire-format message, translate it, speak it, and show it.
     *
     * <p>Wire format received: {@code FROM:<peerLangTag>|<originalText>}</p>
     */
    private void handleIncomingMessage(String raw) {
        executor.execute(() -> {
            try {
                String peerLangTag;
                String originalText;

                // Parse wire format
                if (raw.startsWith("FROM:") && raw.contains("|")) {
                    int sep = raw.indexOf("|");
                    peerLangTag  = raw.substring(5, sep);       // e.g. "fr"
                    originalText = raw.substring(sep + 1);      // e.g. "Bonjour!"
                } else {
                    // Fallback for plain text (mock / debug messages)
                    peerLangTag  = "en";
                    originalText = raw;
                }

                // Translate from peer's language into MY language
                String translatedText = translator.translate(
                        originalText, peerLangTag, myLanguage.getBcp47Tag());

                // Speak the translated text aloud
                textToSpeech.speak(translatedText, myLanguage.getBcp47Tag());

                // Show in conversation list
                TranslationMessage msg = new TranslationMessage(
                        originalText, translatedText, TranslationMessage.Direction.INCOMING);

                runOnUiThread(() -> {
                    conversationAdapter.addMessage(msg);
                    scrollToBottom();
                });

            } catch (Exception e) {
                Log.e(TAG, "handleIncomingMessage failed", e);
            }
        });
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private void scrollToBottom() {
        int count = conversationAdapter.getItemCount();
        if (count > 0) {
            binding.rvConversation.smoothScrollToPosition(count - 1);
        }
    }

    /** Mic button visual states. */
    private enum MicState { IDLE, LISTENING, PROCESSING }

    private void setMicState(MicState state) {
        switch (state) {
            case IDLE:
                binding.btnMic.setEnabled(true);
                binding.btnMic.setText("🎤  Hold to Speak");
                binding.tvStatus.setText("Ready");
                break;
            case LISTENING:
                binding.btnMic.setEnabled(true);
                binding.btnMic.setText("🔴  Listening…");
                binding.tvStatus.setText("Listening…");
                break;
            case PROCESSING:
                binding.btnMic.setEnabled(false);
                binding.btnMic.setText("⏳  Translating…");
                binding.tvStatus.setText("Translating…");
                break;
        }
    }
}
