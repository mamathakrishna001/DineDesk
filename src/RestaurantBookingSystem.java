import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import org.mindrot.jbcrypt.BCrypt;

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
    private static final String BLUE = "\u001B[34m";

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
                    case 7 -> managerLogin(connection, scanner);
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

    // ===================== MANAGER LOGIN & FUNCTIONS =====================
    private static void managerLogin(Connection connection, Scanner scanner) {
        try {
            System.out.println(BLUE + "===== MANAGER LOGIN =====" + RESET);
            System.out.print("Enter username: ");
            String username = readToken(scanner);
            System.out.print("Enter password: ");
            String password = readToken(scanner);

            String sql = "SELECT password_hash FROM managers WHERE username = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        if (BCrypt.checkpw(password, storedHash)) {
                            System.out.println(GREEN + "✅ Login successful! Welcome, " + username + "!" + RESET);
                            managerMenu(connection, scanner);
                        } else {
                            System.out.println(RED + "❌ Invalid credentials!" + RESET);
                        }
                    } else {
                        System.out.println(RED + "❌ User not found!" + RESET);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println(RED + "Database error: " + e.getMessage() + RESET);
        }
    }

    private static void managerMenu(Connection connection, Scanner scanner) {
        while (true) {
            System.out.println();
            System.out.println(BLUE + "======== MANAGER PANEL ========" + RESET);
            System.out.println("1. View All Bookings");
            System.out.println("2. Manage Tables");
            System.out.println("3. Manage Meals");
            System.out.println("4. Booking Analytics");
            System.out.println("5. Add New Table");
            System.out.println("6. Update Table Status");
            System.out.println("0. Back to Main Menu");
            System.out.print(YELLOW + "Choose an option: " + RESET);

            int choice = readInt(scanner);

            switch (choice) {
                case 1 -> managerViewBookings(connection);
                case 2 -> manageTables(connection, scanner);
                case 3 -> manageMeals(connection, scanner);
                case 4 -> bookingAnalytics(connection);
                case 5 -> addNewTable(connection, scanner);
                case 6 -> updateTableStatus(connection, scanner);
                case 0 -> { return; }
                default -> System.out.println(RED + "❌ Invalid choice. Try again." + RESET);
            }
        }
    }

    private static void managerViewBookings(Connection connection) {
        String sql = """
            SELECT b.booking_id, b.customer_name, b.contact_number, b.table_number, 
                   b.booking_time, m.meal_name, b.quantity, b.total_price
            FROM bookings b
            JOIN meals m ON b.meal_id = m.meal_id
            ORDER BY b.booking_time DESC
            """;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println(CYAN + "All Bookings (Manager View):" + RESET);
            System.out.println("+----+-----------------+------------+-------+-------------------+----------------+-----+---------+");
            System.out.println("| ID | Customer        | Contact    | Table | Time              | Meal           | Qty | Total   |");
            System.out.println("+----+-----------------+------------+-------+-------------------+----------------+-----+---------+");
            while (rs.next()) {
                System.out.printf("| %-2d | %-15s | %-10s | %-5d | %-17s | %-14s | %-3d | ₹%-6.2f |\n",
                        rs.getInt("booking_id"), rs.getString("customer_name"), rs.getString("contact_number"),
                        rs.getInt("table_number"), rs.getString("booking_time"), rs.getString("meal_name"),
                        rs.getInt("quantity"), rs.getDouble("total_price"));
            }
            System.out.println("+----+-----------------+------------+-------+-------------------+----------------+-----+---------+");
        } catch (SQLException e) {
            System.out.println(RED + "Error: " + e.getMessage() + RESET);
        }
    }

    private static void manageTables(Connection connection, Scanner scanner) {
        String sql = "SELECT table_number, seats, is_available FROM tables ORDER BY table_number";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println(CYAN + "Table Management:" + RESET);
            System.out.println("+-------+-------+-----------+");
            System.out.println("| Table | Seats | Available |");
            System.out.println("+-------+-------+-----------+");
            while (rs.next()) {
                String status = rs.getBoolean("is_available") ? "✅ Yes" : "❌ No";
                System.out.printf("| %-5d | %-5d | %-9s |\n",
                        rs.getInt("table_number"), rs.getInt("seats"), status);
            }
            System.out.println("+-------+-------+-----------+");
        } catch (SQLException e) {
            System.out.println(RED + "Error: " + e.getMessage() + RESET);
        }
    }

    private static void manageMeals(Connection connection, Scanner scanner) {
        String sql = "SELECT meal_id, meal_name, price FROM meals ORDER BY meal_id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println(CYAN + "Meal Management:" + RESET);
            System.out.println("+----+--------------------+---------+");
            System.out.println("| ID | Meal Name          | Price   |");
            System.out.println("+----+--------------------+---------+");
            while (rs.next()) {
                System.out.printf("| %-2d | %-18s | ₹%-6.2f |\n",
                        rs.getInt("meal_id"), rs.getString("meal_name"), rs.getDouble("price"));
            }
            System.out.println("+----+--------------------+---------+");
        } catch (SQLException e) {
            System.out.println(RED + "Error: " + e.getMessage() + RESET);
        }
    }

    private static void bookingAnalytics(Connection connection) {
        try {
            // Total bookings
            String totalSql = "SELECT COUNT(*) as total FROM bookings";
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(totalSql)) {
                rs.next();
                int totalBookings = rs.getInt("total");
                System.out.println(GREEN + "📊 Total Bookings: " + totalBookings + RESET);
            }

            // Total revenue
            String revenueSql = "SELECT SUM(total_price) as revenue FROM bookings";
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(revenueSql)) {
                rs.next();
                double totalRevenue = rs.getDouble("revenue");
                System.out.println(GREEN + "💰 Total Revenue: ₹" + String.format("%.2f", totalRevenue) + RESET);
            }

            // Most popular table
            String popularTableSql = """
                SELECT table_number, COUNT(*) as bookings 
                FROM bookings 
                GROUP BY table_number 
                ORDER BY bookings DESC 
                LIMIT 1
                """;
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(popularTableSql)) {
                if (rs.next()) {
                    System.out.println(GREEN + "🏆 Most Popular Table: " + rs.getInt("table_number") +
                            " (" + rs.getInt("bookings") + " bookings)" + RESET);
                }
            }

        } catch (SQLException e) {
            System.out.println(RED + "Error: " + e.getMessage() + RESET);
        }
    }

    private static void addNewTable(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter table number: ");
            int tableNumber = readInt(scanner);

            if (tableExists(connection, tableNumber)) {
                System.out.println(RED + "❌ Table already exists!" + RESET);
                return;
            }

            System.out.print("Enter number of seats: ");
            int seats = readInt(scanner);

            String sql = "INSERT INTO tables (table_number, seats, is_available) VALUES (?, ?, TRUE)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, tableNumber);
                ps.setInt(2, seats);
                int rows = ps.executeUpdate();
                System.out.println(rows > 0 ? GREEN + "✅ Table added successfully!" + RESET :
                        RED + "❌ Failed to add table." + RESET);
            }
        } catch (SQLException e) {
            System.out.println(RED + "Error: " + e.getMessage() + RESET);
        }
    }

    private static void updateTableStatus(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter table number: ");
            int tableNumber = readInt(scanner);

            if (!tableExists(connection, tableNumber)) {
                System.out.println(RED + "❌ Table not found!" + RESET);
                return;
            }

            System.out.print("Set table as available? (1=Yes, 0=No): ");
            int status = readInt(scanner);
            boolean isAvailable = status == 1;

            String sql = "UPDATE tables SET is_available = ? WHERE table_number = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setBoolean(1, isAvailable);
                ps.setInt(2, tableNumber);
                int rows = ps.executeUpdate();
                System.out.println(rows > 0 ? GREEN + "✅ Table status updated!" + RESET :
                        RED + "❌ Failed to update table." + RESET);
            }
        } catch (SQLException e) {
            System.out.println(RED + "Error: " + e.getMessage() + RESET);
        }
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