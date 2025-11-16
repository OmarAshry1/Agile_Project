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

- **US 4.1 – User Authentication (Login)**  
  As a user, I want to log in based on my role, so that I can use services available to me based on my role. :contentReference[oaicite:6]{index=6}  

- **US 1.1 – View Room Availability**  
  As a student/professor, I want to view available classrooms/labs so that I can find a free room. :contentReference[oaicite:7]{index=7}  

- **US 1.3 – Manage Classroom / Lab Records**  
  As an admin, I want to create, edit, and delete classroom/lab records so that room data used for booking is accurate and up to date. :contentReference[oaicite:8]{index=8}  

- **US 1.6 – Report Maintenance Issue**  
  As a student/professor/staff, I want to report maintenance issues, so that they could be resolved by a responsible staff. :contentReference[oaicite:9]{index=9}  

### Sprint 1 Increment (What you can do)

- Log in as different roles (student, professor, staff, admin).
- View a list of classrooms/labs and see their availability.
- As an admin:
  - Create, update, and delete room records.
- As a student/professor/staff:
  - Submit maintenance tickets for rooms.

Other backlog items (bookings, ticket tracking, resource allocation, transcripts, notifications, etc.) are planned for later sprints within Phase 1.

---

## 4. Architecture

The codebase is organized around the **Facilities Module** under the base package `edu.facilities`:

```text
edu.facilities
 ├─ model
 │   ├─ User
 │   ├─ StudentRecord
 │   ├─ Room
 │   ├─ Booking
 │   ├─ Equipment
 │   └─ MaintenanceTicket
 │
 ├─ service
 │   ├─ AuthService
 │   ├─ RoomService
 │   ├─ BookingService
 │   ├─ MaintenanceService
 │   ├─ EquipmentService
 │   └─ AdminService
 │
 ├─ data
 │   └─ InMemoryDataStore
 │
 └─ ui
     ├─ MainApp
     ├─ ScreenRouter
     ├─ LoginScreen
     ├─ DashboardScreen
     ├─ RoomsScreen
     ├─ RoomManagementScreen
     └─ MaintenanceScreen
