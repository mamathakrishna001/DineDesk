# 🍽️ Restaurant Booking App

A robust Java-based restaurant table booking system with MySQL backend, featuring secure manager authentication and intuitive table reservation management.

## ✨ Features

### 🎯 Core Functionality
- **Table Reservation System**: Complete booking workflow with date/time validation
- **Manager Dashboard**: Secure login with role-based access control
- **Real-time Availability**: Check table availability before booking
- **Customer Management**: Store and manage customer information
- **Booking History**: Track all reservations with detailed logs

### 🔐 Security Features
- **BCrypt Password Hashing**: Industry-standard password encryption
- **SQL Injection Prevention**: Prepared statements for database security
- **Input Validation**: Comprehensive data validation and sanitization

## 🛠️ Tech Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Language** | Java | OpenJDK 24+ |
| **Database** | MySQL | 8.0+ |
| **Security** | jBCrypt | 0.4 |
| **Database Driver** | MySQL Connector/J | Latest |
| **IDE** | IntelliJ IDEA | Any |

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/mamathakrishna001/restaurant-booking-app.git
cd restaurant-booking-app
```

### 2. Database Setup
```sql
-- Create database
CREATE DATABASE restaurant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE restaurant;

-- Create managers table
CREATE TABLE managers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create tables table
CREATE TABLE tables (
    id INT AUTO_INCREMENT PRIMARY KEY,
    table_number INT NOT NULL UNIQUE,
    capacity INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create bookings table
CREATE TABLE bookings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    email VARCHAR(255),
    table_id INT NOT NULL,
    booking_time DATETIME NOT NULL,
    party_size INT NOT NULL,
    status ENUM('confirmed', 'cancelled', 'completed') DEFAULT 'confirmed',
    special_requests TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (table_id) REFERENCES tables(id) ON DELETE RESTRICT
);

-- Insert sample data
INSERT INTO tables (table_number, capacity) VALUES
(1, 2), (2, 4), (3, 6), (4, 8), (5, 2), (6, 4);
```

### 3. Create Manager Account
Run the `CreateManagerHash.java` utility to generate a hashed password:
```java
// Example: Create admin user
String plainPassword = "admin123";
String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
```

Then insert into database:
```sql
INSERT INTO managers (username, password_hash) 
VALUES ('admin', 'your_generated_hash_here');
```

### 4. Configure Database Connection
Update database credentials in `RestaurantBookingSystem.java`:
```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant";
private static final String DB_USERNAME = "your_db_username";
private static final String DB_PASSWORD = "your_db_password";
```

### 5. Add Dependencies
Download and add to classpath:
- [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/)
- [jBCrypt](https://github.com/jeremyh/jBCrypt)

## 🗄️ Database Schema

### Entity Relationship Diagram
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    managers     │     │     tables      │     │    bookings     │
├─────────────────┤     ├─────────────────┤     ├─────────────────┤
│ id (PK)         │     │ id (PK)         │     │ id (PK)         │
│ username        │     │ table_number    │     │ customer_name   │
│ password_hash   │     │ capacity        │     │ phone           │
│ created_at      │     │ is_active       │     │ email           │
└─────────────────┘     │ created_at      │     │ table_id (FK)   │
                        └─────────────────┘     │ booking_time    │
                                    │           │ party_size      │
                                    │           │ status          │
                                    │           │ special_requests│
                                    │           │ created_at      │
                                    └───────────│                 │
                                                └─────────────────┘
```

### Table Descriptions

#### `managers`
Stores secure manager credentials for system access.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | Unique manager identifier |
| `username` | VARCHAR(255) | NOT NULL, UNIQUE | Manager login username |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt hashed password |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Account creation time |

#### `tables`
Restaurant table inventory with capacity information.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | Unique table identifier |
| `table_number` | INT | NOT NULL, UNIQUE | Physical table number |
| `capacity` | INT | NOT NULL | Maximum seating capacity |
| `is_active` | BOOLEAN | DEFAULT TRUE | Table availability status |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Table registration time |

#### `bookings`
Customer reservations with comprehensive booking details.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INT | PRIMARY KEY, AUTO_INCREMENT | Unique booking identifier |
| `customer_name` | VARCHAR(255) | NOT NULL | Customer full name |
| `phone` | VARCHAR(15) | NOT NULL | Contact phone number |
| `email` | VARCHAR(255) | NULLABLE | Customer email address |
| `table_id` | INT | FOREIGN KEY → tables(id) | Reserved table reference |
| `booking_time` | DATETIME | NOT NULL | Reservation date and time |
| `party_size` | INT | NOT NULL | Number of guests |
| `status` | ENUM | confirmed/cancelled/completed | Booking status |
| `special_requests` | TEXT | NULLABLE | Additional customer requests |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Booking creation time |

## 💡 Usage

### Manager Functions
1. **Secure Login**: Authenticate using username and password
2. **View Bookings**: Display all reservations with filters
3. **Manage Tables**: Add, update, or deactivate tables
4. **Booking Analytics**: View booking statistics and reports

### Customer Functions
1. **Make Reservation**: Book available tables with preferred time
2. **View Availability**: Check real-time table availability
3. **Update Booking**: Modify existing reservations (if permitted)
4. **Cancel Booking**: Cancel reservations with proper notification

## 🔒 Security

### Password Security
- **BCrypt Hashing**: All passwords stored using BCrypt with salt rounds of 12
- **No Plaintext Storage**: Zero plaintext password storage in database
- **Secure Verification**: Password verification using `BCrypt.checkpw()`

### Database Security
- **Prepared Statements**: All SQL queries use prepared statements
- **Input Validation**: Comprehensive sanitization of user inputs
- **SQL Injection Prevention**: Protection against common SQL attacks

## 📁 Project Structure

```
restaurant-booking-app/
├── 📂 src/
│   ├── 📄 RestaurantBookingSystem.java    # Main application logic
│   ├── 📄 CreateManagerHash.java          # Password hashing utility
├── 📂 lib/
│   ├── 📄 mysql-connector-j.jar           # MySQL JDBC driver
│   └── 📄 jbcrypt-0.4.jar                 # BCrypt library
├── 📄 .gitignore                          # Git ignore rules
└── 📄 README.md                           # This file
```