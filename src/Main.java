public class Main {
    public static void main(String[] args) {
        int a = 10;
        int b = 10;

        System.out.println("Primitive comparison (a == b): " + (a == b));

        String str1 = new String("Java");
        String str2 = new String("Java");

        System.out.println("Reference comparison (str1 == str2): " + (str1 == str2));

        System.out.println("Content comparison (str1.equals(str2)): " + str1.equals(str2));
    }
}
