import org.mindrot.jbcrypt.BCrypt;

public class CreateManagerHash {
    public static void main(String[] args) {
        String password = "Admin@123"; // choose your password
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        System.out.println("Hashed password: " + hash);
    }
}