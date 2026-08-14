# University Management System — Sprint 1

This repository is the executable increment for the one and only completed Scrum sprint (August 1–14, 2026), totaling 8 stories and 41 story points. It implements exactly these eight stories: US 4.1 authentication, US 1.1 room availability, US 1.3 room records, US 1.6 maintenance reporting, US 1.7 ticket tracking, US 1.8 course-room assignment, US 2.1 course catalog, and US 2.2 core-subject registration.

## Stack

Java 21, JavaFX 21, Maven, PostgreSQL JDBC, and HikariCP. The database is Supabase PostgreSQL; no SQL Server syntax or credentials are used.

## Database setup

1. Run `Sprint1_Query_PostgreSQL.sql` in the Supabase SQL editor.
2. Copy `src/main/resources/database.properties.example` to `database.properties` (ignored), or set `SUPABASE_DB_HOST`, `SUPABASE_DB_PORT`, `SUPABASE_DB_NAME`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD`, and `SUPABASE_DB_SSL`.
3. Add users with SHA-256 password hashes produced by `AuthService.hash`; public registration is restricted to the STUDENT role.

## Run

`mvn clean compile` then `mvn javafx:run`. Without Supabase credentials the UI can launch, but database workflows cannot be verified.

ADMIN manages rooms, course-room assignments, and all tickets. STUDENT views the catalog, registers for courses, reports issues, and sees their tickets. PROFESSOR and STAFF can view rooms and use maintenance workflows; staff support ticket assignment/status changes through the service layer.

Future backlog items—bookings, equipment, announcements, messaging, admissions, transcripts, grading, assignments, quizzes, exams, payroll, leave, research, and staff profiles—are intentionally not implemented.
