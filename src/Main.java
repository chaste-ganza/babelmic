public class Main {
    public static void main(String[] args) {
        char mode = 'B';

        switch(mode) {
            case 'A':
                System.out.println("Mode A Selected");
                break;
            case 'B':
                System.out.println("Mode B Selected");
                break;
            default:
                System.out.println("Default mode");
        }

        System.out.println("\nRunning Loop:");
        for (int i = 1; i <= 3; i++){
            System.out.println("Iteration count: " + i);
        }
    }
}
