public class Main {
    public static void main(String[] args) {
        System.out.println("=== BabelMic Pipeline Check ===");

        SpeechRecognizer recognizer = new MockSpeechRecognizer();
        Translator translator = new MockTranslator();

        ((MockSpeechRecognizer) recognizer).setMicrophoneSensitivity(100);
        String audioInput = "alice_sample.wav";
        String recognizedText = recognizer.recognizeSpeech(audioInput);
        System.out.println("Alice said (recognized): \"" + recognizedText + "\"");

        String translatedText = translator.translate(recognizedText, "English", "French");
        System.out.println("Translated for Bob (French): \"" + translatedText + "\"");

        MockTranslator mockTransRef = (MockTranslator) translator;
        System.out.println("Total translations performed: " + mockTransRef.getTranslationCount());

        String audioInput2 = "bob_sample.wav";
        String recognizedTextBob = recognizer.recognizeSpeech(audioInput2);
        System.out.println("Bob said (recognized):: \""+ recognizedTextBob + "\"");
        
        String translatedTextBob = translator.translate(recognizedTextBob, "French", "English");
        System.out.println("Translated for Alice (English): \"" + translatedTextBob + "\"");
        System.out.println("Total translations performed: " + mockTransRef.getTranslationCount());

    }
}
