public class MockTextToSpeech implements TextToSpeech{
    private boolean isMuted = false;

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }

    @Override
    public void speak(String text, String language) {
        if (text ==null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to speak cannot be empty.");
        }

        if(isMuted){
            System.out.println("[TTS Output Muted]: Cannot pronounce \"" + text + "\"");
            return;
        }

        System.out.println("[TTS Output (" + language + ")]: Speaking aloud -> \"" + text + "\"");
    }
}
