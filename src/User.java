public class User {

    private String username;
    private int level;

    public User(String username, int level) {
        this.username = username;
        this.level = level;
    }

    public void displayProfile(){
        System.out.println("Use: " + username + " | Level: " + level);
    }

    public String getUsername(){
        return username;
    }
}
