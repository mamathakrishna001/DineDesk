import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class RestaurantBookingSystem {
    // ====== DB CONFIG ======
    private static final String url = "jdbc:mysql://localhost:3306/restaurant";
    private static final String username = "root";
    private static final String password = "mamatha@2005";

    // ====== UI COLORS ======
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }

        try (Connection connection = DriverManager.getConnection(url, username, password);
             Scanner scanner = new Scanner(System.in)) {

            while (true) {
                System.out.println();
                System.out.println(CYAN + "=============================");
                System.out.println("  RESTAURANT BOOKING SYSTEM");
                System.out.println("=============================" + RESET);
                System.out.println("1. Book a Table");
                System.out.println("2. View Bookings");
                System.out.println("3. Find Table for Booking");
                System.out.println("4. Update Booking");
                System.out.println("5. Cancel Booking");
                System.out.println("6. Search Available Tables");
                System.out.println("7. Manager Login");
                System.out.println("8. Generate Bill");
                System.out.println("0. Exit");
                System.out.print(YELLOW + "Choose an option: " + RESET);

                int choice = readInt(scanner);

                switch (choice) {
                    case 1 -> bookTable(connection, scanner);
                    case 2 -> viewBookings(connection);
                    case 3 -> getTableForBooking(connection, scanner);
                    case 4 -> updateBooking(connection, scanner);
                    case 5 -> cancelBooking(connection, scanner);
                    case 6 -> searchAvailableTables(connection, scanner);
                    case 7 -> managerMenu(connection, scanner);
                    case 8 -> generateBill(connection, scanner);
                    case 0 -> { exit(); return; }
                    default -> System.out.println(RED + "❌ Invalid choice. Try again." + RESET);
                }
            }
        } catch (SQLException | InterruptedException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    // ====================== CORE FUNCTIONS ======================
    private static void bookTable(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter customer name: ");
            String name = readLine(scanner);
            System.out.print("Enter contact number: ");
            String contact = readToken(scanner);
            System.out.print("Enter table number: ");
            int tableNo = readInt(scanner);

            if (!tableExists(connection, tableNo)) {
                System.out.println(RED + "❌ Table not found." + RESET);
                return;
            }

            if (!isTableAvailable(connection, tableNo)) {
                System.out.println(RED + "❌ Table not available." + RESET);
                return;
            }

            System.out.print("Enter booking time (YYYY-MM-DDTHH:MM): ");
            LocalDateTime bookingTime = readDateTime(scanner);

            System.out.print("Enter meal ID: ");
            int mealId = readInt(scanner);

            double mealPrice = getMealPrice(connection, mealId);
            if (mealPrice == 0.0) {
                System.out.println(RED + "❌ Invalid meal ID." + RESET);
                return;
            }

            System.out.print("Enter quantity: ");
            int qty = readInt(scanner);
            double totalPrice = mealPrice * qty;

            String sql = """
                INSERT INTO bookings (customer_name, contact_number, table_number, booking_time, meal_id, quantity, total_price)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, contact);
                ps.setInt(3, tableNo);
                ps.setString(4, bookingTime.toString());
                ps.setInt(5, mealId);
                ps.setInt(6, qty);
                ps.setDouble(7, totalPrice);
                int rows = ps.executeUpdate();
                System.out.println(rows > 0 ? GREEN + "✅ Booking successful!" + RESET : RED + "❌ Booking failed." + RESET);
            }
        } catch (SQLException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    private static void viewBookings(Connection connection) {
        String sql = """
            SELECT b.booking_id, b.customer_name, b.contact_number, b.table_number, b.booking_time, m.meal_name, b.quantity, b.total_price
            FROM bookings b
            JOIN meals m ON b.meal_id = m.meal_id
            ORDER BY b.booking_id
            """;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println(CYAN + "Current Bookings:" + RESET);
            System.out.println("+----+-----------------+---------+-------+-------------------+----------------+-----+--------+");
            System.out.println("| ID | Customer        | Contact | Table | Time              | Meal           | Qty | Total  |");
            System.out.println("+----+-----------------+---------+-------+-------------------+----------------+-----+--------+");
            while (rs.next()) {
                System.out.printf("| %-2d | %-15s | %-7s | %-5d | %-17s | %-14s | %-3d | %-6.2f |\n",
                        rs.getInt("booking_id"), rs.getString("customer_name"), rs.getString("contact_number"),
                        rs.getInt("table_number"), rs.getString("booking_time"), rs.getString("meal_name"),
                        rs.getInt("quantity"), rs.getDouble("total_price"));
            }
            System.out.println("+----+-----------------+---------+-------+-------------------+----------------+-----+--------+");
        } catch (SQLException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    private static void getTableForBooking(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter booking ID: ");
            int bookingId = readInt(scanner);
            String sql = "SELECT table_number FROM bookings WHERE booking_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println(GREEN + "Table Number: " + rs.getInt("table_number") + RESET);
                    } else {
                        System.out.println(RED + "❌ Booking not found." + RESET);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    private static void updateBooking(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter booking ID to update: ");
            int bookingId = readInt(scanner);
            if (!bookingExists(connection, bookingId)) {
                System.out.println(RED + "❌ Booking not found." + RESET);
                return;
            }

            System.out.print("Enter new table number: ");
            int tableNo = readInt(scanner);
            if (!tableExists(connection, tableNo)) {
                System.out.println(RED + "❌ Invalid table." + RESET);
                return;
            }

            System.out.print("Enter new booking time (YYYY-MM-DDTHH:MM): ");
            LocalDateTime bookingTime = readDateTime(scanner);

            System.out.print("Enter new meal ID: ");
            int mealId = readInt(scanner);
            double mealPrice = getMealPrice(connection, mealId);

            System.out.print("Enter new quantity: ");
            int qty = readInt(scanner);

            double total = mealPrice * qty;

            String sql = """
                UPDATE bookings SET table_number=?, booking_time=?, meal_id=?, quantity=?, total_price=?
                WHERE booking_id=?
                """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, tableNo);
                ps.setString(2, bookingTime.toString());
                ps.setInt(3, mealId);
                ps.setInt(4, qty);
                ps.setDouble(5, total);
                ps.setInt(6, bookingId);
                System.out.println(ps.executeUpdate() > 0 ? GREEN + "Booking updated!" + RESET : RED + "Update failed." + RESET);
            }
        } catch (SQLException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    private static void cancelBooking(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter booking ID to cancel: ");
            int bookingId = readInt(scanner);
            if (!bookingExists(connection, bookingId)) {
                System.out.println(RED + "❌ Booking not found." + RESET);
                return;
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM bookings WHERE booking_id=?")) {
                ps.setInt(1, bookingId);
                System.out.println(ps.executeUpdate() > 0 ? GREEN + "Booking cancelled!" + RESET : RED + "Cancel failed." + RESET);
            }
        } catch (SQLException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    private static void searchAvailableTables(Connection connection, Scanner scanner) {
        String sql = "SELECT table_number, seats FROM tables WHERE is_available=TRUE ORDER BY table_number";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println(CYAN + "Available Tables:" + RESET);
            System.out.println("+-------+-------+");
            System.out.println("| Table | Seats |");
            System.out.println("+-------+-------+");
            while (rs.next()) {
                System.out.printf("| %-5d | %-5d |\n", rs.getInt("table_number"), rs.getInt("seats"));
            }
            System.out.println("+-------+-------+");
        } catch (SQLException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    // ===================== MANAGER FUNCTIONS =====================
    private static void managerMenu(Connection connection, Scanner scanner) {
        System.out.println(YELLOW + "Manager features coming soon (like menu & table mgmt)." + RESET);
        // Implement similarly to hotel manager menu
    }

    private static void generateBill(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter booking ID: ");
            int bookingId = readInt(scanner);
            String sql = """
                SELECT b.booking_id, b.customer_name, m.meal_name, m.price, b.quantity, b.total_price
                FROM bookings b
                JOIN meals m ON b.meal_id = m.meal_id
                WHERE booking_id=?
                """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println(RED + "❌ Booking not found." + RESET);
                        return;
                    }
                    String name = rs.getString("customer_name");
                    String meal = rs.getString("meal_name");
                    double price = rs.getDouble("price");
                    int qty = rs.getInt("quantity");
                    double subtotal = rs.getDouble("total_price");
                    double tax = subtotal * 0.12;
                    double service = subtotal * 0.05;
                    double total = subtotal + tax + service;

                    System.out.println(CYAN + "----- BILL -----" + RESET);
                    System.out.println("Customer : " + name);
                    System.out.println("Meal     : " + meal);
                    System.out.printf("Price    : ₹%.2f x %d\n", price, qty);
                    System.out.printf("Subtotal : ₹%.2f\nTax(12%%): ₹%.2f\nService: ₹%.2f\n", subtotal, tax, service);
                    System.out.println("----------------");
                    System.out.printf(GREEN + "Total    : ₹%.2f\n" + RESET, total);
                    System.out.println(CYAN + "----------------" + RESET);
                }
            }
        } catch (SQLException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    // ===================== HELPERS =====================
    private static boolean tableExists(Connection connection, int tableNo) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM tables WHERE table_number=? LIMIT 1")) {
            ps.setInt(1, tableNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean isTableAvailable(Connection connection, int tableNo) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT is_available FROM tables WHERE table_number=?")) {
            ps.setInt(1, tableNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("is_available");
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static double getMealPrice(Connection connection, int mealId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT price FROM meals WHERE meal_id=?")) {
            ps.setInt(1, mealId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("price") : 0.0;
            }
        } catch (SQLException e) {
            return 0.0;
        }
    }

    private static boolean bookingExists(Connection connection, int bookingId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM bookings WHERE booking_id=? LIMIT 1")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static int readInt(Scanner sc) {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.print(RED + "Enter valid number: " + RESET);
            }
        }
    }

    private static String readLine(Scanner sc) {
        String s = sc.nextLine();
        return s.isEmpty() ? readLine(sc) : s;
    }

    private static String readToken(Scanner sc) {
        String s = sc.nextLine().trim();
        return s.isEmpty() ? readToken(sc) : s;
    }

    private static LocalDateTime readDateTime(Scanner sc) {
        while (true) {
            try {
                return LocalDateTime.parse(sc.nextLine().trim());
            } catch (DateTimeParseException e) {
                System.out.print(RED + "Enter in format YYYY-MM-DDTHH:MM: " + RESET);
            }
        }
    }

    private static void exit() throws InterruptedException {
        System.out.print(YELLOW + "Exiting System" + RESET);
        for (int i = 0; i < 5; i++) {
            System.out.print(".");
            Thread.sleep(500);
        }
        System.out.println("\n" + GREEN + "Thank you for using Restaurant Booking System! 👋" + RESET);
    }
}
