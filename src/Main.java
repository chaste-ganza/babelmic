public class Main {
    public static void main(String[] args) {
        System.out.println("=== BabelMic Setup Check ===");

        User user1 = new User("Alexis", 5);
        User user2 = new User("Chaste", 11);

        user1.displayProfile();
        user2.displayProfile();

        System.out.println("\n=== Testing Mock Speech Recognizer ===");

        SpeechRecognizer recognizer = new MockSpeechRecognizer();
        String transcript = recognizer.recognizeSpeech("alice_sample.wav");
        System.out.println("Recognized Text: \"" + transcript + "\"");

    }
}
