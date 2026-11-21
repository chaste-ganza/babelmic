package com.babelmic.app.ui.conversation;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.babelmic.app.databinding.ActivityConversationBinding;
import com.babelmic.app.impl.AndroidSpeechRecognizer;
import com.babelmic.app.impl.AndroidTextToSpeech;
import com.babelmic.app.impl.MLKitTranslator;
import com.babelmic.app.model.Language;
import com.babelmic.app.model.PeerProfile;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.network.WifiDirectPeerConnection;
import com.babelmic.app.ui.disconnected.DisconnectedActivity;
import com.babelmic.app.ui.widget.WaveVisualizerView;
import com.babelmic.app.util.LanguageManager;
import com.babelmic.app.util.ProfileManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Screen 10 — Live conversation.
 *
 * Top bar: Disconnect (red pill) | peer avatar badge (centre) | ● Connected (right)
 * Zone 1: WaveVisualizerView — reacts to mic or TTS with speaker's identity colour
 * Zone 2: Scrolling dialogue RecyclerView — name in colour + text, older lines fade
 * Zone 3: Circular black mic FAB with BabelMic logo
 */
public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = "ConversationActivity";

    private static final String PREFIX_FROM         = "FROM:";
    private static final String CTRL_SPEAKING_START = "CTRL:SPEAKING_START";
    private static final String CTRL_SPEAKING_STOP  = "CTRL:SPEAKING_STOP";
    private static final String CTRL_DISCONNECT     = "CTRL:DISCONNECT";

    private ActivityConversationBinding binding;

    // Pipeline
    private AndroidSpeechRecognizer  speechRecognizer;
    private MLKitTranslator          translator;
    private AndroidTextToSpeech      textToSpeech;
    private WifiDirectPeerConnection peerConnection;

    // Session
    private Language myLanguage;
    private boolean  isGroupOwner;
    private String   peerIpAddress;
    private boolean  isLoopbackMode     = false;
    private String   loopbackTargetLang = "fr";

    private PeerProfile peerProfile = null;
    private volatile boolean peerIsSpeaking   = false;
    private volatile boolean intentionalDisconnect = false;
    private final AtomicBoolean translationModelReady = new AtomicBoolean(false);
    private final CountDownLatch modelReadyLatch = new CountDownLatch(1);

    // Dialogue
    private DialogueAdapter dialogueAdapter;

    private final ExecutorService executor    = Executors.newCachedThreadPool();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConversationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.getRoot().setKeepScreenOn(true);

        readSessionConfig();
        setupPeerBadge();
        setupDialogue();
        initPipeline();
        setupMicButton();
        setupDisconnectButton();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (translator       != null) translator.close();
        if (textToSpeech     != null) textToSpeech.shutdown();
        if (peerConnection   != null && peerConnection.isConnected()) peerConnection.disconnect();
        executor.shutdown();
    }

    @Override public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("End Session?")
                .setMessage("This will disconnect you from your partner.")
                .setPositiveButton("End", (d, w) -> performDisconnect())
                .setNegativeButton("Stay", null).show();
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void readSessionConfig() {
        String tag = getIntent().getStringExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG);
        myLanguage = LanguageManager.findByTag(tag);
        if (myLanguage == null) myLanguage = LanguageManager.findByTag("en");
        isGroupOwner       = getIntent().getBooleanExtra(SessionConfig.EXTRA_IS_GROUP_OWNER, false);
        peerIpAddress      = getIntent().getStringExtra(SessionConfig.EXTRA_PEER_IP_ADDRESS);
        isLoopbackMode     = getIntent().getBooleanExtra(SessionConfig.EXTRA_LOOPBACK_MODE, false);
        loopbackTargetLang = getIntent().getStringExtra(SessionConfig.EXTRA_LOOPBACK_TARGET_LANG);
        if (loopbackTargetLang == null) loopbackTargetLang = "fr";
    }

    private void setupPeerBadge() {
        // Will be updated when handshake arrives; show a default for now
        int defaultRes = com.babelmic.app.R.drawable.avatar_rabbit;
        binding.badgePeerAvatar.set(defaultRes, 0xFF4ADE80);
        if (isLoopbackMode) {
            Language target = LanguageManager.findByTag(loopbackTargetLang);
            String tName = (target != null) ? target.getDisplayName() : loopbackTargetLang;
            binding.tvConnectionStatus.setText("🧪 Test → " + tName);
        }
    }

    private void setupDialogue() {
        dialogueAdapter = new DialogueAdapter();
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.rvDialogue.setLayoutManager(llm);
        binding.rvDialogue.setAdapter(dialogueAdapter);
    }

    // ─── Pipeline ─────────────────────────────────────────────────────────────

    private void initPipeline() {
        // STT
        speechRecognizer = new AndroidSpeechRecognizer(this, myLanguage.getBcp47Tag());
        speechRecognizer.setCallback(new AndroidSpeechRecognizer.SpeechCallback() {
            @Override public void onListeningStarted() {
                runOnUiThread(() -> {
                    binding.waveVisualizer.setColor(ProfileManager.getIdentityColor(
                            ConversationActivity.this));
                    binding.waveVisualizer.setState(WaveVisualizerView.State.SPEAKING_LOCAL);
                });
            }
            @Override public void onRmsChanged(float rmsDb) {
                binding.waveVisualizer.setAmplitude(rmsDb);
            }
            @Override public void onPartialResult(String partial) {
                // Show live partial in dialogue as a preview
                runOnUiThread(() -> {
                    // We don't add partial lines to the adapter — just log them
                });
            }
            @Override public void onResult(String text) {
                binding.waveVisualizer.setState(WaveVisualizerView.State.IDLE);
                if (!text.isEmpty()) translateAndSend(text);
                else runOnUiThread(() -> setMicEnabled(true));
            }
            @Override public void onError(String error) {
                binding.waveVisualizer.setState(WaveVisualizerView.State.IDLE);
                runOnUiThread(() -> {
                    setMicEnabled(true);
                    if (error.contains("permission")) showMicPermissionDialog();
                });
            }
        });

        // Translator
        translator = new MLKitTranslator();
        if (isLoopbackMode) {
            prepareLoopbackModel();
        } else {
            binding.btnMic.setEnabled(false);
            addDialogueLine("System", 0xFF888888, "Connecting…");
        }

        // TTS
        textToSpeech = new AndroidTextToSpeech(this);
        textToSpeech.setDoneCallback(new AndroidTextToSpeech.SpeechDoneCallback() {
            @Override public void onSpeechDone(String id) {
                runOnUiThread(() -> {
                    binding.waveVisualizer.setState(WaveVisualizerView.State.IDLE);
                    int myColor = ProfileManager.getIdentityColor(ConversationActivity.this);
                    binding.waveVisualizer.setColor(myColor);
                });
            }
            @Override public void onSpeechError(String id, String msg) {
                runOnUiThread(() -> binding.waveVisualizer.setState(WaveVisualizerView.State.IDLE));
            }
        });

        // Socket
        peerConnection = new WifiDirectPeerConnection();
        peerConnection.setMyProfileJson(ProfileManager.toHandshakeJson(this));
        peerConnection.setHandshakeCallback(json -> {
            peerProfile = ProfileManager.fromHandshakeJson(json);
            runOnUiThread(() -> {
                // Update peer badge with real avatar and identity colour
                binding.badgePeerAvatar.set(peerProfile.avatarResId, peerProfile.identityColor);
                downloadModelThenEnableMic(peerProfile.languageTag, myLanguage.getBcp47Tag());
            });
        });

        peerConnection.setDataCallback(new WifiDirectPeerConnection.DataReceivedCallback() {
            @Override public void onDataReceived(String data) { handleIncoming(data); }
            @Override public void onConnectionLost(String reason) {
                if (intentionalDisconnect) return;
                runOnUiThread(() -> navigateToDisconnected(false));
            }
        });

        if (!isLoopbackMode) {
            final ConversationActivity self = this;
            executor.execute(() -> {
                if (isGroupOwner) peerConnection.startServer();
                else {
                    boolean ok = peerConnection.connect(peerIpAddress);
                    if (!ok) runOnUiThread(() ->
                            Toast.makeText(self, "Could not connect. Go back and retry.",
                                    Toast.LENGTH_LONG).show());
                }
            });
        }
    }

    // ─── Model download ───────────────────────────────────────────────────────

    /** Signal everyone waiting for the translation model exactly once. */
    private void markModelReady() {
        if (translationModelReady.compareAndSet(false, true)) {
            modelReadyLatch.countDown();
        }
    }

    private void downloadModelThenEnableMic(String fromLang, String toLang) {
        addDialogueLine("System", 0xFF888888, "Downloading translation model…");
        binding.btnMic.setEnabled(false);

        translator.prepareModel(fromLang, toLang, new MLKitTranslator.ModelReadyCallback() {
            @Override public void onModelReady() {
                markModelReady();
                runOnUiThread(() -> {
                    dialogueAdapter.clear();
                    setMicEnabled(true);
                });
            }
            @Override public void onModelDownloadFailed(Exception e) {
                markModelReady();
                runOnUiThread(() -> {
                    addDialogueLine("System", 0xFFEF4444, "⚠ Model unavailable — will retry");
                    setMicEnabled(true);
                });
            }
        });
    }

    private void prepareLoopbackModel() {
        Language target = LanguageManager.findByTag(loopbackTargetLang);
        String tName = (target != null) ? target.getDisplayName() : loopbackTargetLang;
        binding.btnMic.setEnabled(false);
        addDialogueLine("System", 0xFF888888, "Downloading model for " + tName + "…");

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Downloading Language Model")
                .setMessage("One-time download for " + myLanguage.getDisplayName()
                        + " → " + tName + " (~50 MB).")
                .setCancelable(false).create();
        dlg.show();

        translator.prepareModel(myLanguage.getBcp47Tag(), loopbackTargetLang,
                new MLKitTranslator.ModelReadyCallback() {
                    @Override public void onModelReady() {
                        markModelReady();
                        runOnUiThread(() -> { dlg.dismiss(); dialogueAdapter.clear(); setMicEnabled(true); });
                    }
                    @Override public void onModelDownloadFailed(Exception e) {
                        markModelReady();
                        runOnUiThread(() -> { dlg.dismiss(); setMicEnabled(true); });
                    }
                });
    }

    // ─── Mic button ───────────────────────────────────────────────────────────

    private void setupMicButton() {
        binding.btnMic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!peerIsSpeaking) startListening();
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
        binding.btnDisconnect.setOnClickListener(v -> onBackPressed());
    }

    private void startListening() {
        setMicEnabled(true);
        int myColor = ProfileManager.getIdentityColor(this);
        binding.waveVisualizer.setColor(myColor);
        binding.waveVisualizer.setState(WaveVisualizerView.State.SPEAKING_LOCAL);
        if (!isLoopbackMode && peerConnection.isConnected())
            peerConnection.sendData(CTRL_SPEAKING_START);
        executor.execute(() -> speechRecognizer.recognizeSpeech("mic"));
    }

    private void stopListening() {
        speechRecognizer.stopListening();
        if (!isLoopbackMode && peerConnection.isConnected())
            peerConnection.sendData(CTRL_SPEAKING_STOP);
    }

    private void setMicEnabled(boolean enabled) {
        binding.btnMic.setEnabled(enabled && !peerIsSpeaking);
    }

    // ─── Outgoing ─────────────────────────────────────────────────────────────

    private void translateAndSend(String original) {
        executor.execute(() -> {
            try {
                if (isLoopbackMode) {
                    String translated = translator.translate(
                            original, myLanguage.getBcp47Tag(), loopbackTargetLang);
                    Language target = LanguageManager.findByTag(loopbackTargetLang);

                    int myColor = ProfileManager.getIdentityColor(this);
                    String myName = ProfileManager.getDisplayName(this);
                    runOnUiThread(() -> {
                        addDialogueLine(myName.isEmpty() ? "You" : myName, myColor, translated);
                        scrollDialogue();
                        // Peer (simulated) speaks back
                        binding.waveVisualizer.setColor(0xFF4ADE80);
                        binding.waveVisualizer.setState(WaveVisualizerView.State.SPEAKING_PEER);
                        setMicEnabled(false);
                    });
                    textToSpeech.speak(translated, loopbackTargetLang);
                    runOnUiThread(() -> setMicEnabled(true));
                } else {
                    String payload = PREFIX_FROM + myLanguage.getBcp47Tag() + "|" + original;
                    if (peerConnection.isConnected()) peerConnection.sendData(payload);

                    int myColor = ProfileManager.getIdentityColor(this);
                    String myName = ProfileManager.getDisplayName(this);
                    runOnUiThread(() -> {
                        addDialogueLine(myName.isEmpty() ? "You" : myName, myColor, original);
                        scrollDialogue();
                        binding.waveVisualizer.setState(WaveVisualizerView.State.IDLE);
                        setMicEnabled(true);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "translateAndSend failed", e);
                runOnUiThread(() -> setMicEnabled(true));
            }
        });
    }

    // ─── Incoming ─────────────────────────────────────────────────────────────

    private void handleIncoming(String raw) {
        if (raw.startsWith("CTRL:")) {
            switch (raw) {
                case CTRL_SPEAKING_START:
                    peerIsSpeaking = true;
                    runOnUiThread(() -> {
                        setMicEnabled(false);
                        int peerColor = peerProfile != null ? peerProfile.identityColor : 0xFF4ADE80;
                        binding.waveVisualizer.setColor(peerColor);
                        binding.waveVisualizer.setState(WaveVisualizerView.State.IDLE);
                    });
                    break;
                case CTRL_SPEAKING_STOP:
                    peerIsSpeaking = false;
                    runOnUiThread(() -> {
                        if (translationModelReady.get()) setMicEnabled(true);
                    });
                    break;
                case CTRL_DISCONNECT:
                    runOnUiThread(() -> navigateToDisconnected(true));
                    break;
            }
            return;
        }

        executor.execute(() -> {
            try {
                String peerLang, original;
                if (raw.startsWith(PREFIX_FROM) && raw.contains("|")) {
                    int sep  = raw.indexOf("|");
                    peerLang = raw.substring(PREFIX_FROM.length(), sep);
                    original = raw.substring(sep + 1);
                } else { peerLang = "en"; original = raw; }

                // Wait until the translation model is ready (or time out).
                try {
                    modelReadyLatch.await(15, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                String translated = translator.translate(
                        original, peerLang, myLanguage.getBcp47Tag());

                int peerColor = peerProfile != null ? peerProfile.identityColor : 0xFF4ADE80;
                String peerName = peerProfile != null ? peerProfile.name : "Partner";

                runOnUiThread(() -> {
                    binding.waveVisualizer.setColor(peerColor);
                    binding.waveVisualizer.setState(WaveVisualizerView.State.SPEAKING_PEER);
                    addDialogueLine(peerName, peerColor, translated);
                    scrollDialogue();
                });
                textToSpeech.speak(translated, myLanguage.getBcp47Tag());

            } catch (Exception e) {
                Log.e(TAG, "handleIncoming failed", e);
            }
        });
    }

    // ─── Dialogue helpers ─────────────────────────────────────────────────────

    private void addDialogueLine(String name, int color, String text) {
        dialogueAdapter.addLine(new DialogueAdapter.DialogueLine(name, color, text));
    }

    private void scrollDialogue() {
        int count = dialogueAdapter.getItemCount();
        if (count > 0) binding.rvDialogue.smoothScrollToPosition(count - 1);
    }

    // ─── Disconnect ───────────────────────────────────────────────────────────

    private void performDisconnect() {
        intentionalDisconnect = true;
        if (peerConnection == null || !peerConnection.isConnected()) {
            navigateToDisconnected(false);
            return;
        }

        try {
            // Close the socket only after the "disconnect" message has been flushed.
            peerConnection.sendData(CTRL_DISCONNECT, () -> {
                peerConnection.disconnect();
                runOnUiThread(() -> navigateToDisconnected(false));
            });
        } catch (IllegalStateException e) {
            // Peer vanished between the check and the send — just leave.
            peerConnection.disconnect();
            navigateToDisconnected(false);
        }
    }

    private void navigateToDisconnected(boolean peerInitiated) {
        Intent intent = new Intent(this, DisconnectedActivity.class);
        intent.putExtra("peer_initiated", peerInitiated);
        intent.putExtra("peer_name",
                peerProfile != null ? peerProfile.name : "Partner");
        intent.putExtra("peer_color",
                peerProfile != null ? peerProfile.identityColor : 0xFF4ADE80);
        // Carry language for "Change language" button
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG,
                myLanguage.getBcp47Tag());
        startActivity(intent);
        finish();
    }

    // ─── Dialogs ──────────────────────────────────────────────────────────────

    private void showMicPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Microphone Access Needed")
                .setMessage("Go to Settings → Apps → BabelMic → Permissions.")
                .setPositiveButton("Open Settings", (d, w) -> {
                    Intent i = new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    i.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null).show();
    }
}
