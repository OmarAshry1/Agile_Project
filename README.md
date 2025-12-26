# University Management System – Phase 1 (Facilities Module)

## 1. Overview

This repository contains **Phase 1** of the *University Management System* developed as part of the **Agile Software Engineering Project**.  
The goal of the system is to streamline administrative and academic processes and provide a centralized platform for students, professors, staff, and administrators. :contentReference[oaicite:0]{index=0}  

Phase 1 focuses on the **Facilities Module**, including:

- Classroom & Laboratory Management  
- Basic Maintenance Reporting  
- Core system access via role-based login  

The project is developed using the **Scrum** framework over multiple sprints, delivering a working increment at the end of each sprint.

---

## 2. Phase 1 Scope

### 2.1 Facilities Module (Phase 1 Backlog)

According to the Phase 1 product backlog (Facilities Module), the high-level features include: :contentReference[oaicite:1]{index=1}  

1. **Classroom & Laboratory Management**
   - US 1.1 – View Room Availability  
   - US 1.2 – Filter Rooms by Capacity / Equipment  
   - US 1.3 – Manage Classroom / Lab Records  
   - US 1.4 – Book a Classroom/Lab  
   - US 1.5 – Cancel/Edit a Booking  
   - US 1.6 – Report Maintenance Issue  
   - US 1.7 – Maintenance Ticket Tracking  

2. **Administrative Office Automation**
   - US 2.1 – Log Student Records  
   - US 2.2 – Student Request Transcript  
   - US 2.3 – Transcript Request Status  
   - US 2.4 – Generate Student Transcript  
   - US 2.5 – Admission Application Management :contentReference[oaicite:2]{index=2}  

3. **Resource Allocation**
   - US 3.1 – View Available Equipment  
   - US 3.2 – Allocate Equipment  
   - US 3.3 – Track Software Licenses  
   - US 3.4 – Equipment Return Process :contentReference[oaicite:3]{index=3}  

4. **Overall System**
   - US 4.1 – User Authentication (Login)  
   - US 4.2 – Notification System :contentReference[oaicite:4]{index=4}  

> **Note:** Phase 1 = Facilities Module + system-level features that support it (authentication & notifications), as defined in the project brief. :contentReference[oaicite:5]{index=5}  

---

## 3. Sprint 1 – Implemented Features

Sprint 1 delivers a working increment that focuses on **core access + basic facilities management**:

### Implemented User Stories (Sprint 1)

- **US 4.1 – User Authentication (Login & Registration)**  
  As a user, I want to log in and register based on my role, so that I can use services available to me based on my role.

- **US 1.1 – View Room Availability**  
  As a user (student/professor/staff/admin), I want to view available classrooms/labs so that I can find a free room.

- **US 1.3 – Manage Classroom / Lab Records**  
  As an admin, I want to create, edit, and delete classroom/lab records so that room data used for booking is accurate and up to date.

- **US 1.6 – Report Maintenance Issue**  
  As a student/professor/staff, I want to report maintenance issues, so that they could be resolved by a responsible staff.

- **US 1.7 – Maintenance Ticket Tracking**  
  As a user, I want to track maintenance tickets so that I can see the status of reported issues.

### Sprint 1 Increment (What you can do)

#### User Roles & Permissions

**Admin:**
- View all rooms and their availability
- Create, edit, and delete room records
- View all maintenance tickets
- Assign tickets to staff members
- Cannot create maintenance tickets
- Cannot view individual user tickets

**Staff:**
- View all rooms and their availability
- Create maintenance tickets for rooms
- View tickets assigned to them
- Update ticket status (including marking as "Resolved")
- View tickets they created

**Student:**
- View all rooms and their availability
- Create maintenance tickets for rooms
- View tickets they created and track their status
- Cannot create, edit, or delete room records

**Professor:**
- View all rooms and their availability
- Create maintenance tickets for rooms
- View tickets they created and track their status
- Cannot create, edit, or delete room records

**Guest (Not Logged In):**
- Can only access login and registration screens
- All other features are disabled

#### Key Features

1. **User Authentication**
   - Login with username and password
   - Registration for new users
   - Role-based access control
   - Session management with logout functionality

2. **Room Management**
   - View all rooms in a table format
   - Filter by room code, name, type, capacity, location, and status
   - Admin-only: Create new rooms with all attributes
   - Admin-only: Edit existing room details
   - Admin-only: Delete rooms
   - Room types: CLASSROOM, LAB
   - Room statuses: AVAILABLE, OCCUPIED, MAINTENANCE

3. **Maintenance Ticket System**
   - Create tickets with room selection (dropdown of available rooms only)
   - Ticket status tracking: PENDING, IN_PROGRESS, RESOLVED
   - Staff can update ticket status to "Resolved"
   - Users can view tickets they created
   - Admins can view all tickets and assign them to staff
   - Staff can view tickets assigned to them

4. **Dashboard**
   - Role-based button visibility
   - Quick access to all features
   - User information display
   - Navigation to all modules

Other backlog items (bookings, resource allocation, transcripts, notifications, etc.) are planned for later sprints within Phase 1.

---

## 4. Architecture

The codebase is organized around the **Facilities Module** under the base package `edu.facilities`:

```text
edu.facilities
 ├─ Main.java                    (Application entry point)
 │
 ├─ model
 │   ├─ User                     (Abstract base class)
 │   ├─ Student                  (Concrete implementation)
 │   ├─ Professor                (Concrete implementation)
 │   ├─ Staff                    (Concrete implementation)
 │   ├─ Admin                    (Concrete implementation)
 │   ├─ Room                     (Room data model)
 │   ├─ RoomType                 (Enum: CLASSROOM, LAB)
 │   ├─ RoomStatus               (Enum: AVAILABLE, OCCUPIED, MAINTENANCE)
 │   ├─ MaintenanceTicket        (Ticket data model)
 │   └─ TicketStatus             (Enum: PENDING, IN_PROGRESS, RESOLVED)
 │
 ├─ service
 │   ├─ AuthService              (Authentication & authorization)
 │   ├─ RoomService              (Room CRUD operations)
 │   ├─ MaintenanceService       (Ticket management)
 │   ├─ BookingService           (Reserved for future use)
 │   └─ DatabaseConnection       (Singleton database connection manager)
 │
 ├─ data
 │   └─ Database                 (Database initialization)
 │
 └─ ui
     ├─ LoginController          (Login screen)
     ├─ RegisterController       (Registration screen)
     ├─ dashboardcontroller      (Main dashboard)
     ├─ RoomsController          (Room viewing/management)
     ├─ AddRoomController        (Room creation)
     ├─ EditRoomController       (Room editing)
     ├─ MaintenanceController    (Ticket creation)
     └─ TicketsViewController    (Ticket viewing & management)
```

### Technology Stack

- **Language:** Java 21
- **UI Framework:** JavaFX 21
- **Database:** Microsoft SQL Server
- **Build Tool:** Maven
- **Architecture Pattern:** MVC (Model-View-Controller)
- **Design Patterns:** Singleton (DatabaseConnection, AuthService), Factory (User creation)

---

## 5. Database Configuration

The application expects the following environment variables to establish a Microsoft SQL Server connection:

- `MSSQL_HOST` – server hostname or IP (e.g., `localhost`)
- `MSSQL_INSTANCE` – optional named instance (omit for default)
- `MSSQL_PORT` – TCP port, usually `1433`
- `MSSQL_DB` – database name (e.g., `agile`)
- `MSSQL_USER` / `MSSQL_PASSWORD` – SQL authentication credentials
- `MSSQL_ENCRYPT` – `true` or `false` (defaults to SQL Server driver behavior)
- `MSSQL_TRUST_SERVER_CERT` – set `true` when using self-signed certs in dev
- `MSSQL_LOGIN_TIMEOUT` – optional login timeout in seconds
- `MSSQL_JDBC_EXTRA` – any additional `key=value;` pairs to append to the JDBC URL

Example (PowerShell):

```powershell
setx MSSQL_HOST "localhost"
setx MSSQL_INSTANCE ""
setx MSSQL_PORT "1433"
setx MSSQL_DB "agile"
setx MSSQL_USER "agile_user"
setx MSSQL_PASSWORD "agile123!"
setx MSSQL_ENCRYPT "true"
setx MSSQL_TRUST_SERVER_CERT "true"
```

Restart your IDE/terminal after setting the variables so the JVM can read them.

---

## 6. Running the Application

### Prerequisites

1. **Java JDK 21 or higher**
2. **JavaFX SDK 21** - Download from https://openjfx.io/
3. **Microsoft SQL Server** with the database schema initialized
4. **Maven** (for building, or use IDE)

### Quick Start

#### Option 1: Using IntelliJ IDEA (Recommended)

1. Open the project in IntelliJ IDEA
2. Ensure JavaFX is configured in Project Structure → Libraries
3. Set up database environment variables (see Database Configuration above)
4. Initialize the database using `Sprint1_Query.sql`
5. Right-click on `src/main/java/edu/facilities/Main.java`
6. Select "Run 'Main.main()'"

#### Option 2: Using Maven

```bash
# Compile the project
mvn clean compile

# Run the application
mvn javafx:run
```

#### Option 3: Manual Compilation

```bash
# Set JavaFX path (adjust as needed)
set JAVAFX_PATH=C:\javafx-sdk-21\lib

# Compile
javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -d target/classes -sourcepath src/main/java src/main/java/edu/facilities/Main.java

# Copy resources
xcopy /Y /E src\main\resources\* target\classes\

# Run
java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -cp "target/classes" edu.facilities.Main
```

### Database Setup

1. Run the SQL script `Sprint1_Query.sql` to create the database schema
2. Ensure all tables are created (Users, Rooms, MaintenanceTickets, etc.)
3. Configure environment variables for database connection (see Database Configuration above)

---

## 7. Project Structure

```
Agile_project/
├── src/
│   └── main/
│       ├── java/
│       │   └── edu/facilities/
│       │       ├── Main.java
│       │       ├── model/          (Data models)
│       │       ├── service/        (Business logic)
│       │       ├── ui/             (Controllers)
│       │       └── data/           (Database utilities)
│       └── resources/
│           ├── fxml/               (FXML UI files)
│           └── css/                (Stylesheets)
├── pom.xml                         (Maven configuration)
├── Sprint1_Query.sql               (Database schema)
└── README.md                       (This file)
```

---

## 8. Development Notes

- The `target` folder contains compiled classes and can be safely deleted. Maven will regenerate it on the next build.
- All database operations use prepared statements to prevent SQL injection.
- User authentication uses a singleton pattern for session management.
- The application follows role-based access control (RBAC) throughout the UI and business logic.