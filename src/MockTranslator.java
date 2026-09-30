public class MockTranslator implements Translator {
    private int translationCount = 0;

    public int getTranslationCount(){
        return translationCount;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to translate cannot be empty.");
        }

        translationCount++;

        if (sourceLang.equalsIgnoreCase("English") && targetLang.equalsIgnoreCase("French")){
            if (text.equalsIgnoreCase("Where is the train station?")) {
                return "Ou est la gare?";
            }
            return "FR Mock translation: " + text;
        }

        if (sourceLang.equalsIgnoreCase("French") && targetLang.equalsIgnoreCase("English")){
            if(text.equalsIgnoreCase("C'est tout droit.")) {
                return "It is straight ahead";
            }
            return "English mock translation: " + text;
        }
        return text;
    }
}
