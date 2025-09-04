import org.mindrot.jbcrypt.BCrypt;
import java.util.Scanner;

/**
 * Utility class to generate BCrypt hashed passwords for manager accounts
 * Use this to create secure password hashes before inserting into the database
 */
public class CreateManagerHash {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("===== MANAGER PASSWORD HASH GENERATOR =====");
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("❌ Username and password cannot be empty!");
            return;
        }

        // Generate hash with salt rounds of 12 (recommended for good security)
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

        System.out.println("\n===== GENERATED HASH =====");
        System.out.println("Username: " + username);
        System.out.println("Password Hash: " + hashedPassword);

        System.out.println("\n===== SQL COMMAND =====");
        System.out.println("INSERT INTO managers (username, password_hash) VALUES ('" +
                username + "', '" + hashedPassword + "');");

        System.out.println("\n===== VERIFICATION =====");
        System.out.print("Enter password again to verify: ");
        String verifyPassword = scanner.nextLine().trim();

        if (BCrypt.checkpw(verifyPassword, hashedPassword)) {
            System.out.println("✅ Password verification successful!");
        } else {
            System.out.println("❌ Password verification failed!");
        }

        scanner.close();
    }
}