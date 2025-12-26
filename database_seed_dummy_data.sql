-- ========================================
-- DATABASE SEEDING SCRIPT - DUMMY DATA
-- For Testing University Management System
-- ========================================

-- IMPORTANT: Run Sprint1_Query_PostgreSQL_CLEAN.sql first!
-- This script inserts test data for all tables

-- ========================================
-- NOTE: Most lookup tables (UserTypes, StatusTypes, RoomTypes, 
-- EquipmentType, CourseTypes) are already populated in 
-- Sprint1_Query_PostgreSQL_CLEAN.sql. 
-- However, Departments and Semesters need to be inserted here.
-- ========================================

-- ========================================
-- 0. INSERT DEPARTMENTS (Required for Courses)
-- ========================================
INSERT INTO Departments (Code, Name) VALUES
('CS', 'Computer Science'),
('MATH', 'Mathematics'),
('ENG', 'English'),
('PHYS', 'Physics'),
('CHEM', 'Chemistry'),
('BIO', 'Biology'),
('HIST', 'History'),
('ART', 'Art'),
('MUS', 'Music'),
('PE', 'Physical Education')
ON CONFLICT (Code) DO NOTHING;

-- ========================================
-- 0.5. INSERT EQUIPMENT TYPES (Required for Equipment)
-- ========================================
INSERT INTO EquipmentType (Name) VALUES
('Laptop'),
('Projector'),
('Microscope'),
('Printer'),
('Tablet'),
('Camera'),
('Whiteboard')
ON CONFLICT (Name) DO NOTHING;

-- ========================================
-- 1. INSERT USERS
-- ========================================
-- Password for all users: "password123" (you should hash this in production)
INSERT INTO Users (Username, Password, Email) VALUES
-- Admin
('admin', 'password123', 'admin@university.edu'),
-- Professors
('prof.smith', 'password123', 'john.smith@university.edu'),
('prof.johnson', 'password123', 'mary.johnson@university.edu'),
('prof.williams', 'password123', 'robert.williams@university.edu'),
-- Staff
('staff.brown', 'password123', 'sarah.brown@university.edu'),
('staff.davis', 'password123', 'michael.davis@university.edu'),
-- Students
('student.alice', 'password123', 'alice.student@university.edu'),
('student.bob', 'password123', 'bob.student@university.edu'),
('student.charlie', 'password123', 'charlie.student@university.edu'),
('student.diana', 'password123', 'diana.student@university.edu'),
('student.eve', 'password123', 'eve.student@university.edu'),
-- Parents
('parent.jones', 'password123', 'parent.jones@email.com'),
('parent.miller', 'password123', 'parent.miller@email.com')
ON CONFLICT (Username) DO NOTHING;

-- ========================================
-- 2. INSERT USER ROLES
-- ========================================
-- Get UserTypeIDs and UserIDs for role assignment
DO $$
DECLARE
    admin_type_id INT;
    prof_type_id INT;
    staff_type_id INT;
    student_type_id INT;
    parent_type_id INT;
BEGIN
    -- Get type IDs
    SELECT UserTypeID INTO admin_type_id FROM UserTypes WHERE TypeCode = 'ADMIN';
    SELECT UserTypeID INTO prof_type_id FROM UserTypes WHERE TypeCode = 'PROFESSOR';
    SELECT UserTypeID INTO staff_type_id FROM UserTypes WHERE TypeCode = 'STAFF';
    SELECT UserTypeID INTO student_type_id FROM UserTypes WHERE TypeCode = 'STUDENT';
    SELECT UserTypeID INTO parent_type_id FROM UserTypes WHERE TypeCode = 'PARENT';
    
    -- Assign roles
    INSERT INTO UserRoles (UserID, UserTypeID, IsPrimary)
    SELECT u.UserID, admin_type_id, TRUE
    FROM Users u WHERE u.Username = 'admin'
    ON CONFLICT DO NOTHING;
    
    INSERT INTO UserRoles (UserID, UserTypeID, IsPrimary)
    SELECT u.UserID, prof_type_id, TRUE
    FROM Users u WHERE u.Username IN ('prof.smith', 'prof.johnson', 'prof.williams')
    ON CONFLICT DO NOTHING;
    
    INSERT INTO UserRoles (UserID, UserTypeID, IsPrimary)
    SELECT u.UserID, staff_type_id, TRUE
    FROM Users u WHERE u.Username IN ('staff.brown', 'staff.davis')
    ON CONFLICT DO NOTHING;
    
    INSERT INTO UserRoles (UserID, UserTypeID, IsPrimary)
    SELECT u.UserID, student_type_id, TRUE
    FROM Users u WHERE u.Username LIKE 'student.%'
    ON CONFLICT DO NOTHING;
    
    INSERT INTO UserRoles (UserID, UserTypeID, IsPrimary)
    SELECT u.UserID, parent_type_id, TRUE
    FROM Users u WHERE u.Username LIKE 'parent.%'
    ON CONFLICT DO NOTHING;
END $$;

-- ========================================
-- 3. INSERT ROOMS
-- ========================================
DO $$
DECLARE
    classroom_type_id INT;
    lab_type_id INT;
    available_status_id INT;
BEGIN
    SELECT RoomTypeID INTO classroom_type_id FROM RoomTypes WHERE TypeCode = 'CLASSROOM';
    SELECT RoomTypeID INTO lab_type_id FROM RoomTypes WHERE TypeCode = 'LAB';
    SELECT StatusTypeID INTO available_status_id FROM StatusTypes WHERE StatusCode = 'AVAILABLE' AND EntityType = 'ROOM';
    
    INSERT INTO Rooms (Code, Name, RoomTypeID, Capacity, Location, StatusTypeID) VALUES
    ('A101', 'Room A101', classroom_type_id, 30, 'Main Building Floor 1', available_status_id),
    ('A102', 'Room A102', classroom_type_id, 25, 'Main Building Floor 1', available_status_id),
    ('A201', 'Room A201', classroom_type_id, 40, 'Main Building Floor 2', available_status_id),
    ('B101', 'Lab B101', lab_type_id, 20, 'Science Building Floor 1', available_status_id),
    ('B102', 'Lab B102', lab_type_id, 20, 'Science Building Floor 1', available_status_id),
    ('C301', 'Room C301', classroom_type_id, 50, 'Engineering Building Floor 3', available_status_id)
    ON CONFLICT DO NOTHING;
END $$;

-- ========================================
-- 4. INSERT SEMESTERS (Required for Courses)
-- ========================================
INSERT INTO Semesters (Code, Name, StartDate, EndDate, AcademicYear) VALUES
('FALL2024', 'Fall 2024', '2024-09-01', '2024-12-15', 2024),
('SPRING2025', 'Spring 2025', '2025-01-15', '2025-05-15', 2025)
ON CONFLICT (Code) DO NOTHING;

-- ========================================
-- 5. INSERT COURSES
-- ========================================
DO $$
DECLARE
    cs_dept_id INT;
    math_dept_id INT;
    eng_dept_id INT;
    core_type_id INT;
    elective_type_id INT;
    fall2024_id INT;
    prof1_id INT;
    prof2_id INT;
    prof3_id INT;
BEGIN
    SELECT DepartmentID INTO cs_dept_id FROM Departments WHERE Code = 'CS';
    SELECT DepartmentID INTO math_dept_id FROM Departments WHERE Code = 'MATH';
    SELECT DepartmentID INTO eng_dept_id FROM Departments WHERE Code = 'ENG';
    SELECT CourseTypeID INTO core_type_id FROM CourseTypes WHERE TypeCode = 'CORE';
    SELECT CourseTypeID INTO elective_type_id FROM CourseTypes WHERE TypeCode = 'ELECTIVE';
    SELECT SemesterID INTO fall2024_id FROM Semesters WHERE Code = 'FALL2024';
    SELECT UserID INTO prof1_id FROM Users WHERE Username = 'prof.smith';
    SELECT UserID INTO prof2_id FROM Users WHERE Username = 'prof.johnson';
    SELECT UserID INTO prof3_id FROM Users WHERE Username = 'prof.williams';
    
    -- Verify all required IDs were found
    IF cs_dept_id IS NULL OR math_dept_id IS NULL OR eng_dept_id IS NULL THEN
        RAISE EXCEPTION 'Department IDs not found. Make sure Departments are inserted first.';
    END IF;
    IF core_type_id IS NULL OR elective_type_id IS NULL THEN
        RAISE EXCEPTION 'CourseType IDs not found.';
    END IF;
    IF fall2024_id IS NULL THEN
        RAISE EXCEPTION 'Semester ID not found. Make sure Semesters are inserted first.';
    END IF;
    IF prof1_id IS NULL OR prof2_id IS NULL OR prof3_id IS NULL THEN
        RAISE EXCEPTION 'Professor User IDs not found. Make sure Users are inserted first.';
    END IF;
    
    INSERT INTO Courses (Code, Name, Description, DepartmentID, Credits, SemesterID, CourseTypeID, MaxSeats, CurrentSeats, ProfessorUserID) VALUES
    ('CS101', 'Introduction to Computer Science', 'Learn the basics of programming and computer science', cs_dept_id, 3, fall2024_id, core_type_id, 30, 5, prof1_id),
    ('CS201', 'Data Structures', 'Study fundamental data structures and algorithms', cs_dept_id, 4, fall2024_id, core_type_id, 25, 3, prof1_id),
    ('MATH101', 'Calculus I', 'Introduction to differential and integral calculus', math_dept_id, 4, fall2024_id, core_type_id, 40, 8, prof2_id),
    ('MATH201', 'Linear Algebra', 'Study of vector spaces and linear transformations', math_dept_id, 3, fall2024_id, core_type_id, 30, 4, prof2_id),
    ('ENG101', 'English Composition', 'Develop writing and communication skills', eng_dept_id, 3, fall2024_id, core_type_id, 35, 6, prof3_id),
    ('CS301', 'Database Systems', 'Learn database design and SQL', cs_dept_id, 3, fall2024_id, elective_type_id, 20, 0, prof1_id)
    ON CONFLICT DO NOTHING;
END $$;

-- ========================================
-- 6. INSERT ENROLLMENTS
-- ========================================
DO $$
DECLARE
    enrolled_status_id INT;
BEGIN
    SELECT StatusTypeID INTO enrolled_status_id FROM StatusTypes WHERE StatusCode = 'ENROLLED' AND EntityType = 'ENROLLMENT';
    
    -- Enroll students in courses (Grade has DEFAULT '' so we don't need to include it)
    INSERT INTO Enrollments (CourseID, StudentUserID, EnrollmentDate, StatusTypeID)
    SELECT c.CourseID, u.UserID, CURRENT_TIMESTAMP, enrolled_status_id
    FROM Courses c
    CROSS JOIN Users u
    WHERE c.Code IN ('CS101', 'MATH101', 'ENG101')
    AND u.Username LIKE 'student.%'
    AND u.Username IN ('student.alice', 'student.bob', 'student.charlie')
    ON CONFLICT DO NOTHING;
END $$;

-- ========================================
-- 7. INSERT EQUIPMENT
-- ========================================
DO $$
DECLARE
    laptop_type_id INT;
    projector_type_id INT;
    available_status_id INT;
BEGIN
    SELECT EquipmentTypeID INTO laptop_type_id FROM EquipmentType WHERE Name = 'Laptop';
    SELECT EquipmentTypeID INTO projector_type_id FROM EquipmentType WHERE Name = 'Projector';
    SELECT StatusTypeID INTO available_status_id FROM StatusTypes WHERE StatusCode = 'AVAILABLE' AND EntityType = 'EQUIPMENT';
    
    INSERT INTO Equipment (EquipmentTypeID, SerialNumber, Location, StatusTypeID, Notes) VALUES
    (laptop_type_id, 'LAP001', 'IT Department', available_status_id, 'Dell Latitude'),
    (laptop_type_id, 'LAP002', 'IT Department', available_status_id, 'Dell Latitude'),
    (laptop_type_id, 'LAP003', 'IT Department', available_status_id, 'HP EliteBook'),
    (projector_type_id, 'PROJ001', 'Room A101', available_status_id, 'Epson'),
    (projector_type_id, 'PROJ002', 'Room A201', available_status_id, 'Epson'),
    (projector_type_id, 'PROJ003', 'Room C301', available_status_id, 'BenQ')
    ON CONFLICT DO NOTHING;
END $$;

-- ========================================
-- 8. INSERT MAINTENANCE TICKETS
-- ========================================
DO $$
DECLARE
    open_status_id INT;
    student1_id INT;
    staff1_id INT;
    staff2_id INT;
    room1_id INT;
BEGIN
    SELECT StatusTypeID INTO open_status_id FROM StatusTypes WHERE StatusCode = 'NEW' AND EntityType = 'TICKET';
    SELECT UserID INTO student1_id FROM Users WHERE Username = 'student.alice';
    SELECT UserID INTO staff1_id FROM Users WHERE Username = 'staff.brown';
    SELECT UserID INTO staff2_id FROM Users WHERE Username = 'staff.davis';
    SELECT RoomID INTO room1_id FROM Rooms WHERE Code = 'A101';
    
    -- Note: AssignedToUserID must reference a valid UserID (cannot be 0 due to FK constraint)
    INSERT INTO MaintenanceTickets (RoomID, ReporterUserID, AssignedToUserID, Description, StatusTypeID) VALUES
    (room1_id, student1_id, staff1_id, 'Projector not working in Room A101', open_status_id),
    (room1_id, student1_id, staff2_id, 'Air conditioning too cold', open_status_id)
    ON CONFLICT DO NOTHING;
END $$;

-- ========================================
-- 9. INSERT ADMISSION APPLICATIONS
-- ========================================
DO $$
DECLARE
    pending_status_id INT;
BEGIN
    SELECT StatusTypeID INTO pending_status_id FROM StatusTypes WHERE StatusCode = 'SUBMITTED' AND EntityType = 'APPLICATION';
    
    INSERT INTO AdmissionApplications (FirstName, LastName, Email, PhoneNumber, DateOfBirth, Address, City, State, ZipCode, Country, Program, StatusTypeID) VALUES
    ('John', 'Doe', 'john.doe@email.com', '555-0101', '2005-03-15', '123 Main St', 'Springfield', 'IL', '62701', 'USA', 'Computer Science', pending_status_id),
    ('Jane', 'Smith', 'jane.smith@email.com', '555-0102', '2005-07-22', '456 Oak Ave', 'Chicago', 'IL', '60601', 'USA', 'Mathematics', pending_status_id)
    ON CONFLICT DO NOTHING;
END $$;

-- ========================================
-- VERIFICATION QUERIES
-- ========================================
SELECT 'Users created:' as info, COUNT(*) as count FROM Users;
SELECT 'User roles assigned:' as info, COUNT(*) as count FROM UserRoles;
SELECT 'Rooms created:' as info, COUNT(*) as count FROM Rooms;
SELECT 'Courses created:' as info, COUNT(*) as count FROM Courses;
SELECT 'Enrollments created:' as info, COUNT(*) as count FROM Enrollments;
SELECT 'Equipment created:' as info, COUNT(*) as count FROM Equipment;
SELECT 'Tickets created:' as info, COUNT(*) as count FROM MaintenanceTickets;

-- ========================================
-- DONE!
-- ========================================
SELECT '✅ Database seeding completed successfully!' as status;

