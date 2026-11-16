package com.babelmic.app.ui.conversation;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.babelmic.app.databinding.ActivityConversationBinding;
import com.babelmic.app.impl.AndroidSpeechRecognizer;
import com.babelmic.app.impl.AndroidTextToSpeech;
import com.babelmic.app.impl.MLKitTranslator;
import com.babelmic.app.model.Language;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.network.WifiDirectPeerConnection;
import com.babelmic.app.ui.widget.AvatarVisualizerView;
import com.babelmic.app.util.LanguageManager;
import com.babelmic.app.util.ProfileManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Screen 7 — Live conversation.
 *
 * <h3>Translation readiness</h3>
 * In normal mode, the mic is disabled until after the connection handshake
 * delivers the peer's language tag AND the ML Kit model for that pair has
 * finished downloading. Status is shown in the subtitle card.
 *
 * <h3>Disconnect protocol</h3>
 * When either side taps Disconnect or presses Back:
 * <ol>
 *   <li>Send {@code CTRL:DISCONNECT} to the peer.</li>
 *   <li>Close the socket.</li>
 *   <li>Finish the activity.</li>
 * </ol>
 * When the peer sends {@code CTRL:DISCONNECT} to us, we show a friendly
 * "Partner ended the session" dialog rather than the generic "connection lost" error.
 */
public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = "ConversationActivity";

    // ─── Wire protocol ────────────────────────────────────────────────────────
    private static final String PREFIX_FROM         = "FROM:";
    private static final String CTRL_SPEAKING_START = "CTRL:SPEAKING_START";
    private static final String CTRL_SPEAKING_STOP  = "CTRL:SPEAKING_STOP";
    private static final String CTRL_DISCONNECT     = "CTRL:DISCONNECT";

    // ─── View binding ─────────────────────────────────────────────────────────
    private ActivityConversationBinding binding;

    // ─── Pipeline ─────────────────────────────────────────────────────────────
    private AndroidSpeechRecognizer  speechRecognizer;
    private MLKitTranslator          translator;
    private AndroidTextToSpeech      textToSpeech;
    private WifiDirectPeerConnection peerConnection;

    // ─── Session state ────────────────────────────────────────────────────────
    private Language myLanguage;
    private boolean  isGroupOwner;
    private String   peerIpAddress;
    private boolean  isLoopbackMode     = false;
    private String   loopbackTargetLang = "fr";

    private ProfileManager.PeerProfile peerProfile = null;
    private volatile boolean peerIsSpeaking = false;

    /**
     * True once the ML Kit translation model for the peer's language is ready.
     * Mic is disabled until this is true in normal mode.
     */
    private final AtomicBoolean translationModelReady = new AtomicBoolean(false);

    /** True if WE initiated the disconnect (suppresses the "connection lost" error dialog). */
    private volatile boolean intentionalDisconnect = false;

    // ─── Subtitle state ───────────────────────────────────────────────────────
    private String previousSubtitle = "";

    private final ExecutorService executor    = Executors.newCachedThreadPool();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConversationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.getRoot().setKeepScreenOn(true);

        readSessionConfig();
        setupMyAvatar();
        initPipeline();
        setupMicButton();
        setupDisconnectButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (translator       != null) translator.close();
        if (textToSpeech     != null) textToSpeech.shutdown();
        // peerConnection.disconnect() is called explicitly in performDisconnect()
        // onDestroy can reach here after that, so guard with isConnected check
        if (peerConnection != null && peerConnection.isConnected()) {
            peerConnection.disconnect();
        }
        executor.shutdown();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("End Session?")
                .setMessage("This will disconnect you from your partner.")
                .setPositiveButton("End", (d, w) -> performDisconnect())
                .setNegativeButton("Stay", null).show();
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void readSessionConfig() {
        String tag     = getIntent().getStringExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG);
        myLanguage     = LanguageManager.findByTag(tag);
        if (myLanguage == null) myLanguage = LanguageManager.findByTag("en");
        isGroupOwner       = getIntent().getBooleanExtra(SessionConfig.EXTRA_IS_GROUP_OWNER, false);
        peerIpAddress      = getIntent().getStringExtra(SessionConfig.EXTRA_PEER_IP_ADDRESS);
        isLoopbackMode     = getIntent().getBooleanExtra(SessionConfig.EXTRA_LOOPBACK_MODE, false);
        loopbackTargetLang = getIntent().getStringExtra(SessionConfig.EXTRA_LOOPBACK_TARGET_LANG);
        if (loopbackTargetLang == null) loopbackTargetLang = "fr";
    }

    private void setupMyAvatar() {
        binding.avatarVisualizer.setAvatar(
                ProfileManager.getAvatarShape(this),
                ProfileManager.getColorTop(this),
                ProfileManager.getColorBottom(this));
        binding.avatarVisualizer.setState(AvatarVisualizerView.VisualizerState.IDLE);
        binding.tvSpeakerName.setText("");
    }

    // ─── Pipeline ─────────────────────────────────────────────────────────────

    private void initPipeline() {
        // STT
        speechRecognizer = new AndroidSpeechRecognizer(this, myLanguage.getBcp47Tag());
        speechRecognizer.setCallback(new AndroidSpeechRecognizer.SpeechCallback() {
            @Override public void onListeningStarted() {
                runOnUiThread(() -> setMicActive(true));
            }
            @Override public void onRmsChanged(float rmsDb) {
                binding.avatarVisualizer.setAmplitude(rmsDb);
            }
            @Override public void onPartialResult(String partial) {
                runOnUiThread(() -> showSubtitle(previousSubtitle, partial));
            }
            @Override public void onResult(String text) {
                binding.avatarVisualizer.setState(AvatarVisualizerView.VisualizerState.IDLE);
                runOnUiThread(() -> setMicActive(false));
                if (!text.isEmpty()) translateAndSend(text);
                else runOnUiThread(() -> setMicActive(false));
            }
            @Override public void onError(String error) {
                binding.avatarVisualizer.setState(AvatarVisualizerView.VisualizerState.IDLE);
                runOnUiThread(() -> {
                    setMicActive(false);
                    if (error.contains("permission")) showMicPermissionDialog();
                });
            }
        });

        // Translator
        translator = new MLKitTranslator();

        if (isLoopbackMode) {
            // Loopback: we know both languages immediately — download now
            prepareLoopbackModel();
        } else {
            // Normal mode: mic blocked until handshake + model download both complete
            binding.btnMic.setEnabled(false);
            showSubtitle("", "Connecting to partner…");
        }

        // TTS
        textToSpeech = new AndroidTextToSpeech(this);
        textToSpeech.setDoneCallback(new AndroidTextToSpeech.SpeechDoneCallback() {
            @Override public void onSpeechDone(String id) {
                runOnUiThread(() -> {
                    binding.avatarVisualizer.setState(AvatarVisualizerView.VisualizerState.IDLE);
                    restoreMyAvatar();
                    binding.tvSpeakerName.setText("");
                });
            }
            @Override public void onSpeechError(String id, String msg) {
                runOnUiThread(() -> {
                    binding.avatarVisualizer.setState(AvatarVisualizerView.VisualizerState.IDLE);
                    restoreMyAvatar();
                });
            }
        });

        // Socket
        peerConnection = new WifiDirectPeerConnection();
        peerConnection.setMyProfileJson(ProfileManager.toHandshakeJson(this));

        peerConnection.setHandshakeCallback(json -> {
            peerProfile = ProfileManager.fromHandshakeJson(json);
            Log.d(TAG, "Handshake complete — peer lang: " + peerProfile.languageTag);
            runOnUiThread(() -> {
                showSubtitle("", "Downloading translation model…");
                binding.btnMic.setEnabled(false);
            });
            // Now download model for peer's language → my language
            downloadModelThenEnableMic(peerProfile.languageTag, myLanguage.getBcp47Tag());
        });

        peerConnection.setDataCallback(new WifiDirectPeerConnection.DataReceivedCallback() {
            @Override public void onDataReceived(String data) { handleIncoming(data); }
            @Override public void onConnectionLost(String reason) {
                if (intentionalDisconnect) return; // we disconnected — no dialog needed
                runOnUiThread(() -> showConnectionLostDialog(reason));
            }
        });

        if (!isLoopbackMode) {
            final ConversationActivity self = this;
            executor.execute(() -> {
                if (isGroupOwner) {
                    peerConnection.startServer();
                } else {
                    boolean ok = peerConnection.connect(peerIpAddress);
                    if (!ok) runOnUiThread(() ->
                            Toast.makeText(self, "Could not connect. Go back and retry.",
                                    Toast.LENGTH_LONG).show());
                }
            });
        }
    }

    // ─── Model download ───────────────────────────────────────────────────────

    /**
     * Downloads the translation model for the given language pair.
     * When complete, enables the mic and clears the status message.
     * Called after the handshake in normal mode.
     */
    private void downloadModelThenEnableMic(String fromLang, String toLang) {
        translator.prepareModel(fromLang, toLang, new MLKitTranslator.ModelReadyCallback() {
            @Override public void onModelReady() {
                translationModelReady.set(true);
                runOnUiThread(() -> {
                    showSubtitle("", "");
                    binding.btnMic.setEnabled(true);
                    Log.d(TAG, "Model ready: " + fromLang + "→" + toLang);
                });
            }
            @Override public void onModelDownloadFailed(Exception e) {
                // Still allow speaking — translate() will retry inline
                // but warn the user
                translationModelReady.set(true);
                runOnUiThread(() -> {
                    showSubtitle("", "⚠ Translation model unavailable — check internet");
                    binding.btnMic.setEnabled(true);
                    Log.w(TAG, "Model download failed, will retry on first message", e);
                });
            }
        });
    }

    private void prepareLoopbackModel() {
        Language target = LanguageManager.findByTag(loopbackTargetLang);
        String tName = (target != null) ? target.getDisplayName() : loopbackTargetLang;

        binding.btnMic.setEnabled(false);
        showSubtitle("", "Downloading model for " + tName + "…");

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Downloading Language Model")
                .setMessage("One-time download for " + myLanguage.getDisplayName()
                        + " → " + tName + " (~50 MB).")
                .setCancelable(false).create();
        dlg.show();

        translator.prepareModel(myLanguage.getBcp47Tag(), loopbackTargetLang,
                new MLKitTranslator.ModelReadyCallback() {
                    @Override public void onModelReady() {
                        translationModelReady.set(true);
                        runOnUiThread(() -> {
                            dlg.dismiss();
                            binding.btnMic.setEnabled(true);
                            showSubtitle("", "");
                        });
                    }
                    @Override public void onModelDownloadFailed(Exception e) {
                        translationModelReady.set(true);
                        runOnUiThread(() -> {
                            dlg.dismiss();
                            binding.btnMic.setEnabled(true);
                            showSubtitle("", "⚠ Model download failed");
                        });
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
        setMicActive(true);
        restoreMyAvatar();
        binding.avatarVisualizer.setState(AvatarVisualizerView.VisualizerState.SPEAKING_LOCAL);
        String name = ProfileManager.getDisplayName(this);
        binding.tvSpeakerName.setText(name.isEmpty() ? "You" : name);
        if (!isLoopbackMode && peerConnection.isConnected())
            peerConnection.sendData(CTRL_SPEAKING_START);
        executor.execute(() -> speechRecognizer.recognizeSpeech("mic"));
    }

    private void stopListening() {
        speechRecognizer.stopListening();
        if (!isLoopbackMode && peerConnection.isConnected())
            peerConnection.sendData(CTRL_SPEAKING_STOP);
    }

    private void setMicActive(boolean active) {
        binding.btnMic.animate()
                .scaleX(active ? 1.15f : 1f)
                .scaleY(active ? 1.15f : 1f)
                .setDuration(120).start();
        // Only re-enable if model is ready and peer is not speaking
        if (!active) {
            binding.btnMic.setEnabled(translationModelReady.get() && !peerIsSpeaking);
        }
    }

    // ─── Disconnect ───────────────────────────────────────────────────────────

    /**
     * Clean intentional disconnect: notifies the peer, closes socket, finishes.
     */
    private void performDisconnect() {
        intentionalDisconnect = true;
        // Notify peer before closing so they see a friendly message
        if (peerConnection != null && peerConnection.isConnected()) {
            try {
                peerConnection.sendData(CTRL_DISCONNECT);
                // Small delay to let the message flush before closing socket
                Thread.sleep(150);
            } catch (Exception ignored) {}
            peerConnection.disconnect();
        }
        finish();
    }

    // ─── Outgoing ─────────────────────────────────────────────────────────────

    private void translateAndSend(String original) {
        executor.execute(() -> {
            try {
                if (isLoopbackMode) {
                    String translated = translator.translate(
                            original, myLanguage.getBcp47Tag(), loopbackTargetLang);
                    runOnUiThread(() -> {
                        showSubtitle(original, translated);
                        binding.avatarVisualizer.setState(
                                AvatarVisualizerView.VisualizerState.SPEAKING_PEER);
                        binding.tvSpeakerName.setText("Translation");
                    });
                    textToSpeech.speak(translated, loopbackTargetLang);
                } else {
                    String payload = PREFIX_FROM + myLanguage.getBcp47Tag() + "|" + original;
                    if (peerConnection.isConnected()) peerConnection.sendData(payload);
                    runOnUiThread(() -> {
                        showSubtitle(original, "→ sent");
                        binding.avatarVisualizer.setState(
                                AvatarVisualizerView.VisualizerState.IDLE);
                        restoreMyAvatar();
                        binding.tvSpeakerName.setText("");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "translateAndSend failed", e);
                runOnUiThread(() -> setMicActive(false));
            }
        });
    }

    // ─── Incoming ─────────────────────────────────────────────────────────────

    private void handleIncoming(String raw) {
        // CTRL messages
        if (raw.startsWith("CTRL:")) {
            switch (raw) {
                case CTRL_SPEAKING_START:
                    peerIsSpeaking = true;
                    runOnUiThread(() -> {
                        binding.btnMic.setEnabled(false);
                        setPeerAvatar();
                        binding.avatarVisualizer.setState(
                                AvatarVisualizerView.VisualizerState.IDLE);
                        String pName = peerProfile != null ? peerProfile.name : "Partner";
                        binding.tvSpeakerName.setText(pName + " is speaking…");
                    });
                    break;
                case CTRL_SPEAKING_STOP:
                    peerIsSpeaking = false;
                    runOnUiThread(() -> {
                        if (translationModelReady.get()) {
                            binding.btnMic.setEnabled(true);
                        }
                    });
                    break;
                case CTRL_DISCONNECT:
                    // Peer deliberately ended the session
                    runOnUiThread(() -> showPeerDisconnectedDialog());
                    break;
            }
            return;
        }

        // Translation payload
        executor.execute(() -> {
            try {
                String peerLang, original;
                if (raw.startsWith(PREFIX_FROM) && raw.contains("|")) {
                    int sep  = raw.indexOf("|");
                    peerLang = raw.substring(PREFIX_FROM.length(), sep);
                    original = raw.substring(sep + 1);
                } else {
                    peerLang = "en";
                    original = raw;
                }

                // If model isn't ready yet, wait up to 15s before giving up
                int waited = 0;
                while (!translationModelReady.get() && waited < 15000) {
                    Thread.sleep(300);
                    waited += 300;
                }

                String translated = translator.translate(
                        original, peerLang, myLanguage.getBcp47Tag());

                runOnUiThread(() -> {
                    setPeerAvatar();
                    binding.avatarVisualizer.setState(
                            AvatarVisualizerView.VisualizerState.SPEAKING_PEER);
                    String pName = peerProfile != null ? peerProfile.name : "Partner";
                    binding.tvSpeakerName.setText(pName + " says…");
                    showSubtitle(original, translated);
                    checkTtsLanguagePack(myLanguage.getBcp47Tag());
                });

                textToSpeech.speak(translated, myLanguage.getBcp47Tag());

            } catch (Exception e) {
                Log.e(TAG, "handleIncoming failed", e);
            }
        });
    }

    // ─── Subtitle ─────────────────────────────────────────────────────────────

    private void showSubtitle(String previous, String current) {
        previousSubtitle = previous;
        binding.tvPreviousText.setText(previous);
        binding.tvCurrentText.setText(current);
    }

    // ─── Avatar ───────────────────────────────────────────────────────────────

    private void restoreMyAvatar() {
        binding.avatarVisualizer.setAvatar(
                ProfileManager.getAvatarShape(this),
                ProfileManager.getColorTop(this),
                ProfileManager.getColorBottom(this));
    }

    private void setPeerAvatar() {
        if (peerProfile != null) {
            binding.avatarVisualizer.setAvatar(
                    peerProfile.avatarShape,
                    peerProfile.colorTop,
                    peerProfile.colorBottom);
        }
    }

    // ─── Dialogs ──────────────────────────────────────────────────────────────

    private void showConnectionLostDialog(String reason) {
        new AlertDialog.Builder(this)
                .setTitle("Connection Lost")
                .setMessage("The connection was interrupted.\n\n" + reason
                        + "\n\nGo back and reconnect.")
                .setPositiveButton("Go Back", (d, w) -> finish())
                .setCancelable(false).show();
    }

    private void showPeerDisconnectedDialog() {
        String peerName = (peerProfile != null) ? peerProfile.name : "Your partner";
        new AlertDialog.Builder(this)
                .setTitle("Session Ended")
                .setMessage(peerName + " has ended the session.")
                .setPositiveButton("OK", (d, w) -> finish())
                .setCancelable(false).show();
    }

    private void showMicPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Microphone Access Needed")
                .setMessage("Go to Settings → Apps → BabelMic → Permissions and enable Microphone.")
                .setPositiveButton("Open Settings", (d, w) -> {
                    android.content.Intent i = new android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    i.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void checkTtsLanguagePack(String langTag) {
        final android.speech.tts.TextToSpeech[] probe = new android.speech.tts.TextToSpeech[1];
        probe[0] = new android.speech.tts.TextToSpeech(this, status -> {
            if (status != android.speech.tts.TextToSpeech.SUCCESS) return;
            java.util.Locale locale = java.util.Locale.forLanguageTag(langTag);
            mainHandler.postDelayed(() -> {
                try {
                    if (probe[0].isLanguageAvailable(locale)
                            == android.speech.tts.TextToSpeech.LANG_MISSING_DATA) {
                        showTtsPackDialog(langTag);
                    }
                } catch (Exception ignored) {}
                probe[0].shutdown();
            }, 500);
        });
    }

    private void showTtsPackDialog(String langTag) {
        Language lang = LanguageManager.findByTag(langTag);
        String name = (lang != null) ? lang.getDisplayName() : langTag;
        new AlertDialog.Builder(this)
                .setTitle("Voice Pack Missing")
                .setMessage("Install the " + name + " voice pack for better audio quality.\n\n"
                        + "Settings → Accessibility → Text-to-Speech")
                .setPositiveButton("Open Settings", (d, w) -> {
                    android.content.Intent i = new android.content.Intent();
                    i.setAction("com.android.settings.TTS_SETTINGS");
                    try { startActivity(i); } catch (Exception ignored) {}
                })
                .setNegativeButton("Ignore", null).show();
    }
}
