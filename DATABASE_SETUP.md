# Database Setup Guide

## Overview
All demo data has been removed and the application now uses Microsoft SQL Server database. All queries are written in SQL Server syntax and match the schema defined in `Sprint(1)_Query.sql`.

## Changes Made

### 1. Removed Demo Data
- ✅ Removed `seedSampleRooms()` method from `RoomService`
- ✅ All services now use database queries instead of in-memory data

### 2. Database Connection
- ✅ Created `DatabaseConnection.java` utility class
- ✅ Added SQL Server JDBC driver dependency to `pom.xml`
- ✅ Database name set to `agile` (matching Sprint(1)_Query.sql)

### 3. Updated Services
- ✅ **RoomService**: All CRUD operations now use SQL Server queries matching the actual schema
- ✅ **AuthService**: Login and registration now query SQL Server database
- ✅ **BookingService**: Prepared for future Bookings table (not in current schema)

## Database Schema (from Sprint(1)_Query.sql)

### Users Table
- `UserID` (INT IDENTITY(1,1), PK) - Auto-increment
- `Username` (VARCHAR(50), UNIQUE)
- `Password` (VARCHAR(255)) - Note: Reserved word, use [Password] in queries
- `UserType` (VARCHAR(20)) - Values: STUDENT, PROFESSOR, STAFF, ADMIN

### Rooms Table
- `RoomID` (INT IDENTITY(1,1), PK) - Auto-increment
- `Code` (VARCHAR(20), NOT NULL) - **This is the identifier used in the application** (e.g., 'R101', 'LAB1')
- `Name` (VARCHAR(100))
- `Type` (VARCHAR(20)) - Values: CLASSROOM, LAB (Note: RoomType enum also has OFFICE, CONFERENCE)
- `Capacity` (INT)
- `Location` (VARCHAR(100))
- `Status` (VARCHAR(20), DEFAULT 'AVAILABLE') - Values: AVAILABLE, OCCUPIED, MAINTENANCE

### Additional Tables (in schema but not yet used in services)
- `Students`, `Professors`, `Staff`, `Admins` - Extended user information
- `EquipmentType`, `RoomEquipment` - Equipment management
- `MaintenanceTickets` - Maintenance ticket system

## SQL Server Queries Used

All queries are SQL Server compatible and match the actual schema:

### RoomService Queries
- Uses `Code` field as identifier (not RoomID which is auto-increment)
- `SELECT Code, Name, Type, Capacity, Location, Status FROM Rooms WHERE Code = ?` - Get room by Code
- `SELECT Code, Name, Type, Capacity, Location, Status FROM Rooms ORDER BY Code` - Get all rooms
- `SELECT Code, Name, Type, Capacity, Location, Status FROM Rooms WHERE Status = 'AVAILABLE'` - Get available rooms
- `INSERT INTO Rooms (Code, Name, Type, Capacity, Location, Status) VALUES (?, ?, ?, ?, ?, ?)` - Create room
- `UPDATE Rooms SET [Field] = ? WHERE Code = ?` - Update room fields
- `DELETE FROM Rooms WHERE Code = ?` - Delete room

### AuthService Queries
- Uses `UserID` as INT (auto-increment)
- `SELECT UserID, Username, [Password], UserType FROM Users WHERE Username = ? AND [Password] = ?` - Login
- `SELECT COUNT(*) FROM Users WHERE Username = ?` - Check username exists
- `INSERT INTO Users (Username, [Password], UserType) VALUES (?, ?, ?)` - Register user (UserID auto-generated)

### BookingService Queries
- **NOTE**: Bookings table does not exist in Sprint(1)_Query.sql
- Service is prepared for when the table is added
- Will use `GETDATE()` function for SQL Server date/time

## Setup Instructions

### 1. Create Database
```sql
CREATE DATABASE agile;
GO
```

### 2. Run Schema Script
Execute `Sprint(1)_Query.sql` in SQL Server Management Studio or Azure Data Studio:
```bash
sqlcmd -S localhost -d agile -i "Sprint(1)_Query.sql"
```

### 3. Configure Database Connection
Edit `src/main/java/edu/facilities/service/DatabaseConnection.java` and update:
- `DB_URL`: Your SQL Server connection string (database name is `agile`)
- `DB_USER`: Your SQL Server username
- `DB_PASSWORD`: Your SQL Server password

Example connection string:
```java
private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=agile;encrypt=true;trustServerCertificate=true;";
```

### 4. Test Connection
The application will automatically connect when you run it. If connection fails, check:
- SQL Server is running
- Database `agile` exists
- Credentials are correct
- Firewall allows connections on port 1433

## Important Notes

### Room Identification
- **Room.id** in Java code maps to **Rooms.Code** in database
- **Rooms.RoomID** is auto-increment INT and is not used as identifier
- When creating rooms, provide the `Code` (e.g., "R101", "LAB1")

### User Identification
- **User.id** in Java code maps to **Users.UserID** (INT) converted to String
- **Users.UserID** is auto-increment INT
- When registering users, do NOT provide UserID (it's auto-generated)

### Password Field
- `Password` is a reserved word in SQL Server
- Always use `[Password]` in queries (with square brackets)

### Type Mismatches
- RoomType enum has: CLASSROOM, LAB, OFFICE, CONFERENCE
- Database Rooms.Type CHECK constraint only allows: CLASSROOM, LAB
- Consider updating database schema or enum to match

## Troubleshooting

### Connection Errors
- Verify SQL Server is running: `SELECT @@VERSION`
- Check connection string format
- Ensure SQL Server authentication is enabled
- Verify firewall settings

### Query Errors
- All queries use SQL Server syntax (`GETDATE()`, `VARCHAR`, `INT IDENTITY`, etc.)
- Ensure tables exist and have correct structure
- Check that `Code` field exists in Rooms table
- Verify `[Password]` syntax for Password field

### Module Errors
- `module-info.java` includes `requires java.sql;`
- Rebuild project after changes
