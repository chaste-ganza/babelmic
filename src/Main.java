public class Main {
    public static void main(String[] args) {
        User user1 = new User("Alexis", 5);
        User user2 = new User("Chaste", 11);

        user1.displayProfile();
        user2.displayProfile();

        System.out.println("Fetched name: " + user1.getUsername());
    }
}
