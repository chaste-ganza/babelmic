public class Main {
    public static void main(String[] args) {
        System.out.println("=== BabelMic: Full 4-Block Pipeline Test ===");

        SpeechRecognizer recognizer = new MockSpeechRecognizer();
        Translator translator = new MockTranslator();
        TextToSpeech tts = new MockTextToSpeech();
        PeerConnection connection = new MockPeerConnection();

        connection.connect("Bob-Pixel16");

        String audioInput = "alice_sample.wav";
        String englishText = recognizer.recognizeSpeech(audioInput);
        System.out.println("Aline (EN): " + englishText);

        String frenchText = translator.translate(englishText, "English", "French");

        connection.sendData(frenchText);

        tts.speak(frenchText, "French");

        connection.disconnect();

    }
}
