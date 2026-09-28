public class Main {
    public static void main(String[] args) {
        int intVal = 42;
        double doubleVal = intVal;
        System.out.println("Original int: " + intVal);
        System.out.println("Converted to double: " + doubleVal);

        double price = 19.99;
        int wholePrice = (int) price;

        System.out.println("\nOriginal double: " + price);
        System.out.println("Casted to int: " + wholePrice);
    }
}
