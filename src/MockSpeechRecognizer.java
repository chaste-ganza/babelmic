public class MockSpeechRecognizer implements SpeechRecognizer {
    private int microphoneSensitivity = 50;

    public int getMicrophoneSensitivity(){
        return microphoneSensitivity;
    }

    public void setMicrophoneSensitivity(int sensitivity) {
        if (sensitivity < 0 || sensitivity > 100) {
            throw new IllegalArgumentException("Sensitivity must be between 0 and 100.");
        }
        this.microphoneSensitivity = sensitivity;
    }

    @Override
    public String recognizeSpeech(String audioSource) {
        if (audioSource == null || audioSource.trim().isEmpty()) {
            throw new IllegalArgumentException("Audio source file cannot be empty.");
        }
        System.out.println("Processing audio file: " + audioSource + " (Sensitivity: " + microphoneSensitivity + "%)");

        if (audioSource.equals("alice_sample.wav")) {
            return "Where is the train station?";
        } else if (audioSource.equals("bob_sample.wav")) {
            return "C'est tout droit.";
        }

        return "Unrecognized speech audio.";
    }

}
