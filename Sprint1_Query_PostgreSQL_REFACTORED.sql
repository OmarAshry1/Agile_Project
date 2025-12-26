-- ============================================================================
-- REFACTORED Database Schema - Following Database Excellence Rules
-- PostgreSQL Version
-- ============================================================================
-- EXECUTION ORDER:
-- 1. Lookup tables are created and populated
-- 2. Main tables are created (or skipped if they exist)
-- 3. Migration blocks add missing columns to existing tables
-- 4. Indexes are created (will fail if columns don't exist - see final verification)
-- 5. Final verification ensures all columns exist (runs at the end)
--
-- If you get "column does not exist" errors, the final verification section
-- at the end will add missing columns. Re-run the script after that section.
-- ============================================================================
-- Helper function to safely create index if column exists
CREATE OR REPLACE FUNCTION create_index_if_column_exists(
    index_name TEXT,
    table_name TEXT,
    column_name TEXT
)
RETURNS VOID AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = LOWER(create_index_if_column_exists.table_name)
        AND column_name = LOWER(create_index_if_column_exists.column_name)
    ) THEN
        EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I(%I)', 
            create_index_if_column_exists.index_name,
            create_index_if_column_exists.table_name,
            create_index_if_column_exists.column_name);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Departments lookup table
CREATE TABLE IF NOT EXISTS Departments (
    DepartmentID SERIAL PRIMARY KEY,
    Code VARCHAR(20) NOT NULL UNIQUE,
    Name VARCHAR(100) NOT NULL,
    IsActive BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS IX_Departments_Code ON Departments(Code);
CREATE INDEX IF NOT EXISTS IX_Departments_IsActive ON Departments(IsActive);

-- Status lookup table (for various entity statuses)
CREATE TABLE IF NOT EXISTS StatusTypes (
    StatusTypeID SERIAL PRIMARY KEY,
    EntityType VARCHAR(50) NOT NULL, -- 'STUDENT', 'ROOM', 'TICKET', 'BOOKING', 'EQUIPMENT', 'LICENSE', 'APPLICATION', 'TRANSCRIPT', 'ENROLLMENT', 'LEAVE', 'BENEFIT'
    StatusCode VARCHAR(50) NOT NULL,
    StatusName VARCHAR(100) NOT NULL,
    DisplayOrder INT NOT NULL DEFAULT 0,
    IsActive BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (EntityType, StatusCode)
);

CREATE INDEX IF NOT EXISTS IX_StatusTypes_EntityType ON StatusTypes(EntityType);
CREATE INDEX IF NOT EXISTS IX_StatusTypes_StatusCode ON StatusTypes(StatusCode);

-- UserTypes lookup (for RBAC - Rule 7)
CREATE TABLE IF NOT EXISTS UserTypes (
    UserTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(20) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL,
    IsActive BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS IX_UserTypes_TypeCode ON UserTypes(TypeCode);

-- RelationshipTypes lookup
CREATE TABLE IF NOT EXISTS RelationshipTypes (
    RelationshipTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(50) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- LeaveTypes lookup
CREATE TABLE IF NOT EXISTS LeaveTypes (
    LeaveTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(50) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL,
    MaxDaysPerYear INT NULL,
    RequiresApproval BOOLEAN NOT NULL DEFAULT TRUE
);

-- BenefitTypes lookup
CREATE TABLE IF NOT EXISTS BenefitTypes (
    BenefitTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(50) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- EventTypes lookup
CREATE TABLE IF NOT EXISTS EventTypes (
    EventTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(50) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- Semester lookup
CREATE TABLE IF NOT EXISTS Semesters (
    SemesterID SERIAL PRIMARY KEY,
    Code VARCHAR(50) NOT NULL UNIQUE,
    Name VARCHAR(100) NOT NULL,
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    AcademicYear INT NOT NULL,
    IsActive BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS IX_Semesters_Code ON Semesters(Code);
CREATE INDEX IF NOT EXISTS IX_Semesters_AcademicYear ON Semesters(AcademicYear);

-- YearLevels lookup
CREATE TABLE IF NOT EXISTS YearLevels (
    YearLevelID SERIAL PRIMARY KEY,
    LevelCode VARCHAR(20) NOT NULL UNIQUE,
    LevelName VARCHAR(100) NOT NULL,
    DisplayOrder INT NOT NULL DEFAULT 0
);

-- CourseTypes lookup
CREATE TABLE IF NOT EXISTS CourseTypes (
    CourseTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(20) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- RoomTypes lookup
CREATE TABLE IF NOT EXISTS RoomTypes (
    RoomTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(20) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- PaymentMethods lookup
CREATE TABLE IF NOT EXISTS PaymentMethods (
    PaymentMethodID SERIAL PRIMARY KEY,
    MethodCode VARCHAR(50) NOT NULL UNIQUE,
    MethodName VARCHAR(100) NOT NULL
);

-- ReminderTypes lookup
CREATE TABLE IF NOT EXISTS ReminderTypes (
    ReminderTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(20) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- RSVPStatuses lookup
CREATE TABLE IF NOT EXISTS RSVPStatuses (
    RSVPStatusID SERIAL PRIMARY KEY,
    StatusCode VARCHAR(20) NOT NULL UNIQUE,
    StatusName VARCHAR(100) NOT NULL
);

-- PayFrequencies lookup (for PayrollInformation)
CREATE TABLE IF NOT EXISTS PayFrequencies (
    PayFrequencyID SERIAL PRIMARY KEY,
    FrequencyCode VARCHAR(20) NOT NULL UNIQUE,
    FrequencyName VARCHAR(100) NOT NULL
);

-- ============================================================================
-- POPULATE LOOKUP TABLES WITH INITIAL DATA (must run before main tables)
-- ============================================================================

-- Populate StatusTypes with all required status values
DO $$
BEGIN
    -- STUDENT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('STUDENT', 'ACTIVE', 'Active'),
        ('STUDENT', 'INACTIVE', 'Inactive'),
        ('STUDENT', 'GRADUATED', 'Graduated'),
        ('STUDENT', 'SUSPENDED', 'Suspended'),
        ('STUDENT', 'WITHDRAWN', 'Withdrawn')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- ROOM statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('ROOM', 'AVAILABLE', 'Available'),
        ('ROOM', 'OCCUPIED', 'Occupied'),
        ('ROOM', 'MAINTENANCE', 'Maintenance')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- TICKET statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('TICKET', 'NEW', 'New'),
        ('TICKET', 'IN_PROGRESS', 'In Progress'),
        ('TICKET', 'RESOLVED', 'Resolved')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- BOOKING statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('BOOKING', 'CONFIRMED', 'Confirmed'),
        ('BOOKING', 'CANCELLED', 'Cancelled')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- EQUIPMENT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('EQUIPMENT', 'AVAILABLE', 'Available'),
        ('EQUIPMENT', 'ALLOCATED', 'Allocated'),
        ('EQUIPMENT', 'MAINTENANCE', 'Maintenance'),
        ('EQUIPMENT', 'RETIRED', 'Retired')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- LICENSE statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('LICENSE', 'ACTIVE', 'Active'),
        ('LICENSE', 'EXPIRED', 'Expired'),
        ('LICENSE', 'CANCELLED', 'Cancelled')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- APPLICATION statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('APPLICATION', 'SUBMITTED', 'Submitted'),
        ('APPLICATION', 'UNDER_REVIEW', 'Under Review'),
        ('APPLICATION', 'ACCEPTED', 'Accepted'),
        ('APPLICATION', 'REJECTED', 'Rejected')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- TRANSCRIPT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('TRANSCRIPT', 'PENDING', 'Pending'),
        ('TRANSCRIPT', 'IN_PROGRESS', 'In Progress'),
        ('TRANSCRIPT', 'READY_FOR_PICKUP', 'Ready for Pickup'),
        ('TRANSCRIPT', 'COMPLETED', 'Completed'),
        ('TRANSCRIPT', 'CANCELLED', 'Cancelled')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- ENROLLMENT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('ENROLLMENT', 'ENROLLED', 'Enrolled'),
        ('ENROLLMENT', 'DROPPED', 'Dropped'),
        ('ENROLLMENT', 'COMPLETED', 'Completed'),
        ('ENROLLMENT', 'FAILED', 'Failed')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- LEAVE statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('LEAVE', 'PENDING', 'Pending'),
        ('LEAVE', 'APPROVED', 'Approved'),
        ('LEAVE', 'REJECTED', 'Rejected'),
        ('LEAVE', 'CANCELLED', 'Cancelled')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- BENEFIT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('BENEFIT', 'ACTIVE', 'Active'),
        ('BENEFIT', 'INACTIVE', 'Inactive'),
        ('BENEFIT', 'EXPIRED', 'Expired'),
        ('BENEFIT', 'CANCELLED', 'Cancelled')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- SUBMISSION statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('SUBMISSION', 'SUBMITTED', 'Submitted'),
        ('SUBMISSION', 'GRADED', 'Graded')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- QUIZ_ATTEMPT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('QUIZ_ATTEMPT', 'IN_PROGRESS', 'In Progress'),
        ('QUIZ_ATTEMPT', 'COMPLETED', 'Completed'),
        ('QUIZ_ATTEMPT', 'TIMED_OUT', 'Timed Out')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- ALLOCATION statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('ALLOCATION', 'ACTIVE', 'Active'),
        ('ALLOCATION', 'RETURNED', 'Returned')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
END $$;

-- Populate other lookup tables
INSERT INTO RoomTypes (TypeCode, TypeName) VALUES 
    ('CLASSROOM', 'Classroom'),
    ('LAB', 'Laboratory')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO UserTypes (TypeCode, TypeName) VALUES 
    ('STUDENT', 'Student'),
    ('PROFESSOR', 'Professor'),
    ('STAFF', 'Staff'),
    ('ADMIN', 'Administrator'),
    ('PARENT', 'Parent'),
    ('HR_ADMIN', 'HR Administrator'),
    ('GUEST', 'Guest')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO RelationshipTypes (TypeCode, TypeName) VALUES 
    ('PARENT', 'Parent'),
    ('GUARDIAN', 'Guardian'),
    ('EMERGENCY_CONTACT', 'Emergency Contact')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO LeaveTypes (TypeCode, TypeName) VALUES 
    ('SICK', 'Sick Leave'),
    ('VACATION', 'Vacation'),
    ('PERSONAL', 'Personal'),
    ('MATERNITY', 'Maternity'),
    ('PATERNITY', 'Paternity'),
    ('BEREAVEMENT', 'Bereavement'),
    ('OTHER', 'Other')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO BenefitTypes (TypeCode, TypeName) VALUES 
    ('HEALTH_INSURANCE', 'Health Insurance'),
    ('DENTAL_INSURANCE', 'Dental Insurance'),
    ('VISION_INSURANCE', 'Vision Insurance'),
    ('LIFE_INSURANCE', 'Life Insurance'),
    ('RETIREMENT', 'Retirement'),
    ('VACATION_DAYS', 'Vacation Days'),
    ('SICK_DAYS', 'Sick Days'),
    ('OTHER', 'Other')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO EventTypes (TypeCode, TypeName) VALUES 
    ('GENERAL', 'General'),
    ('ACADEMIC', 'Academic'),
    ('SOCIAL', 'Social'),
    ('SPORTS', 'Sports'),
    ('ADMINISTRATIVE', 'Administrative'),
    ('HOLIDAY', 'Holiday')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO CourseTypes (TypeCode, TypeName) VALUES 
    ('CORE', 'Core'),
    ('ELECTIVE', 'Elective')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO PaymentMethods (MethodCode, MethodName) VALUES 
    ('DIRECT_DEPOSIT', 'Direct Deposit'),
    ('CHECK', 'Check'),
    ('WIRE_TRANSFER', 'Wire Transfer')
ON CONFLICT (MethodCode) DO NOTHING;

INSERT INTO ReminderTypes (TypeCode, TypeName) VALUES 
    ('EMAIL', 'Email'),
    ('IN_APP', 'In-App'),
    ('BOTH', 'Both')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO RSVPStatuses (StatusCode, StatusName) VALUES 
    ('PENDING', 'Pending'),
    ('ATTENDING', 'Attending'),
    ('NOT_ATTENDING', 'Not Attending'),
    ('MAYBE', 'Maybe')
ON CONFLICT (StatusCode) DO NOTHING;

INSERT INTO PayFrequencies (FrequencyCode, FrequencyName) VALUES 
    ('WEEKLY', 'Weekly'),
    ('BIWEEKLY', 'Biweekly'),
    ('MONTHLY', 'Monthly'),
    ('QUARTERLY', 'Quarterly'),
    ('ANNUAL', 'Annual')
ON CONFLICT (FrequencyCode) DO NOTHING;

INSERT INTO YearLevels (LevelCode, LevelName) VALUES 
    ('FRESHMAN', 'Freshman'),
    ('SOPHOMORE', 'Sophomore'),
    ('JUNIOR', 'Junior'),
    ('SENIOR', 'Senior'),
    ('GRADUATE', 'Graduate')
ON CONFLICT (LevelCode) DO NOTHING;

-- ============================================================================
-- EARLY MIGRATION: Add missing columns to existing tables BEFORE indexes
-- This runs early to ensure columns exist before any CREATE INDEX statements
-- ============================================================================

DO $$
BEGIN
    -- Add StatusTypeID to existing tables that might have old schema
    -- Students
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'students') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'students' AND column_name = 'statustypeid') THEN
            ALTER TABLE Students ADD COLUMN StatusTypeID INT;
            UPDATE Students SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'STUDENT' AND StatusCode = 'ACTIVE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE Students ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- Rooms
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'rooms') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'rooms' AND column_name = 'statustypeid') THEN
            ALTER TABLE Rooms ADD COLUMN StatusTypeID INT;
            UPDATE Rooms SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'ROOM' AND StatusCode = 'AVAILABLE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE Rooms ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'rooms' AND column_name = 'roomtypeid') THEN
            ALTER TABLE Rooms ADD COLUMN RoomTypeID INT;
            UPDATE Rooms SET RoomTypeID = (SELECT RoomTypeID FROM RoomTypes WHERE TypeCode = 'CLASSROOM' LIMIT 1) WHERE RoomTypeID IS NULL;
            ALTER TABLE Rooms ALTER COLUMN RoomTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- MaintenanceTickets
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'maintenancetickets') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'maintenancetickets' AND column_name = 'statustypeid') THEN
            ALTER TABLE MaintenanceTickets ADD COLUMN StatusTypeID INT;
            UPDATE MaintenanceTickets SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'TICKET' AND StatusCode = 'NEW' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE MaintenanceTickets ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- Bookings
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'bookings') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'bookings' AND column_name = 'statustypeid') THEN
            ALTER TABLE Bookings ADD COLUMN StatusTypeID INT;
            UPDATE Bookings SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'BOOKING' AND StatusCode = 'CONFIRMED' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE Bookings ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- Equipment
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'equipment') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'equipment' AND column_name = 'statustypeid') THEN
            ALTER TABLE Equipment ADD COLUMN StatusTypeID INT;
            UPDATE Equipment SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'EQUIPMENT' AND StatusCode = 'AVAILABLE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE Equipment ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- SoftwareLicenses
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'softwarelicenses') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'softwarelicenses' AND column_name = 'statustypeid') THEN
            ALTER TABLE SoftwareLicenses ADD COLUMN StatusTypeID INT;
            UPDATE SoftwareLicenses SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'LICENSE' AND StatusCode = 'ACTIVE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE SoftwareLicenses ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- AdmissionApplications
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'admissionapplications') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admissionapplications' AND column_name = 'statustypeid') THEN
            ALTER TABLE AdmissionApplications ADD COLUMN StatusTypeID INT;
            UPDATE AdmissionApplications SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'APPLICATION' AND StatusCode = 'SUBMITTED' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE AdmissionApplications ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- TranscriptRequests
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'transcriptrequests') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'transcriptrequests' AND column_name = 'statustypeid') THEN
            ALTER TABLE TranscriptRequests ADD COLUMN StatusTypeID INT;
            UPDATE TranscriptRequests SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'TRANSCRIPT' AND StatusCode = 'PENDING' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE TranscriptRequests ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- Enrollments
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'enrollments') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'enrollments' AND column_name = 'statustypeid') THEN
            ALTER TABLE Enrollments ADD COLUMN StatusTypeID INT;
            UPDATE Enrollments SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'ENROLLMENT' AND StatusCode = 'ENROLLED' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE Enrollments ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- AssignmentSubmissions
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'assignmentsubmissions') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'assignmentsubmissions' AND column_name = 'statustypeid') THEN
            ALTER TABLE AssignmentSubmissions ADD COLUMN StatusTypeID INT;
            UPDATE AssignmentSubmissions SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'SUBMISSION' AND StatusCode = 'SUBMITTED' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE AssignmentSubmissions ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- QuizAttempts
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'quizattempts') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quizattempts' AND column_name = 'statustypeid') THEN
            ALTER TABLE QuizAttempts ADD COLUMN StatusTypeID INT;
            UPDATE QuizAttempts SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'QUIZ_ATTEMPT' AND StatusCode = 'IN_PROGRESS' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE QuizAttempts ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- LeaveRequests
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'leaverequests') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'leaverequests' AND column_name = 'statustypeid') THEN
            ALTER TABLE LeaveRequests ADD COLUMN StatusTypeID INT;
            UPDATE LeaveRequests SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'LEAVE' AND StatusCode = 'PENDING' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE LeaveRequests ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- BenefitsInformation
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'benefitsinformation') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'benefitsinformation' AND column_name = 'statustypeid') THEN
            ALTER TABLE BenefitsInformation ADD COLUMN StatusTypeID INT;
            UPDATE BenefitsInformation SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'BENEFIT' AND StatusCode = 'ACTIVE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE BenefitsInformation ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- EquipmentUserAllocations
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'equipmentuserallocations') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'equipmentuserallocations' AND column_name = 'statustypeid') THEN
            ALTER TABLE EquipmentUserAllocations ADD COLUMN StatusTypeID INT;
            UPDATE EquipmentUserAllocations SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'ALLOCATION' AND StatusCode = 'ACTIVE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE EquipmentUserAllocations ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
END $$;

-- Users table (must be created before UserRoles)
CREATE TABLE IF NOT EXISTS Users (
    UserID   SERIAL PRIMARY KEY,
    USERNAME VARCHAR(50) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Email    VARCHAR(100) NOT NULL DEFAULT '',  -- Empty string instead of NULL
    CreatedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LastLoginDate TIMESTAMP NULL,
    IsActive BOOLEAN NOT NULL DEFAULT TRUE
);

-- Add IsActive column if it doesn't exist (for migration from old schema)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' 
            AND table_name = 'users' 
            AND column_name = 'isactive'
        ) THEN
            ALTER TABLE Users ADD COLUMN IsActive BOOLEAN NOT NULL DEFAULT TRUE;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS IX_Users_Username ON Users(USERNAME);
CREATE INDEX IF NOT EXISTS IX_Users_Email ON Users(Email);
CREATE INDEX IF NOT EXISTS IX_Users_IsActive ON Users(IsActive);

-- UserRoles junction table for RBAC (Rule 7: Multi-role support)
-- Must be created after Users and UserTypes tables
CREATE TABLE IF NOT EXISTS UserRoles (
    UserRoleID SERIAL PRIMARY KEY,
    UserID INT NOT NULL,
    UserTypeID INT NOT NULL,
    IsPrimary BOOLEAN NOT NULL DEFAULT FALSE,
    AssignedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (UserTypeID) REFERENCES UserTypes(UserTypeID),
    UNIQUE (UserID, UserTypeID)
);

CREATE INDEX IF NOT EXISTS IX_UserRoles_UserID ON UserRoles(UserID);
CREATE INDEX IF NOT EXISTS IX_UserRoles_UserTypeID ON UserRoles(UserTypeID);
CREATE INDEX IF NOT EXISTS IX_UserRoles_IsPrimary ON UserRoles(IsPrimary);

-- Students table 
CREATE TABLE IF NOT EXISTS Students (
    UserID        INT PRIMARY KEY,
    StudentNumber VARCHAR(20) NOT NULL UNIQUE,  -- NOT NULL (Rule 1)
    DepartmentID  INT NULL,  -- FK to Departments
    Major         VARCHAR(100) NOT NULL DEFAULT '',
    EnrollmentDate DATE NOT NULL DEFAULT CURRENT_DATE,  -- NOT NULL with default
    GPA           DECIMAL(3,2) NOT NULL DEFAULT 0.00,  -- NOT NULL with default
    StatusTypeID  INT NOT NULL,  -- FK to StatusTypes (EntityType='STUDENT')
    AdmissionDate DATE NULL,
    YearLevelID   INT NULL,  -- FK to YearLevels
    Notes         TEXT NOT NULL DEFAULT '',  -- Empty string instead of NULL
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID),
    FOREIGN KEY (YearLevelID) REFERENCES YearLevels(YearLevelID)
);

-- Migration: Add new columns if table exists from old schema
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'students') THEN
        -- Add DepartmentID if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'students' AND column_name = 'departmentid'
        ) THEN
            ALTER TABLE Students ADD COLUMN DepartmentID INT NULL;
            -- Add foreign key constraint if it doesn't exist
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.table_constraints 
                WHERE table_schema = 'public' 
                AND table_name = 'students' 
                AND constraint_type = 'FOREIGN KEY'
                AND constraint_name LIKE '%departmentid%'
            ) THEN
                ALTER TABLE Students ADD CONSTRAINT students_departmentid_fkey 
                FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID);
            END IF;
        END IF;
        
        -- Add StatusTypeID if it doesn't exist (will need default value)
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'students' AND column_name = 'statustypeid'
        ) THEN
            -- Get a default status type ID (assuming 'ACTIVE' status exists)
            ALTER TABLE Students ADD COLUMN StatusTypeID INT;
            -- Set a default value - you may need to adjust this based on your StatusTypes data
            UPDATE Students SET StatusTypeID = (
                SELECT StatusTypeID FROM StatusTypes 
                WHERE EntityType = 'STUDENT' AND StatusCode = 'ACTIVE' 
                LIMIT 1
            ) WHERE StatusTypeID IS NULL;
            ALTER TABLE Students ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
        
        -- Add YearLevelID if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'students' AND column_name = 'yearlevelid'
        ) THEN
            ALTER TABLE Students ADD COLUMN YearLevelID INT NULL;
        END IF;
    END IF;
END $$;

-- kol dah 3ashan n7asen el look up 
CREATE INDEX IF NOT EXISTS IX_Students_StudentNumber ON Students(StudentNumber);
CREATE INDEX IF NOT EXISTS IX_Students_DepartmentID ON Students(DepartmentID);
CREATE INDEX IF NOT EXISTS IX_Students_StatusTypeID ON Students(StatusTypeID);
CREATE INDEX IF NOT EXISTS IX_Students_YearLevelID ON Students(YearLevelID);

-- Professors table 
CREATE TABLE IF NOT EXISTS Professors (
    UserID    INT PRIMARY KEY,
    DepartmentID INT NULL,  -- FK to Departments
    OfficeRoom VARCHAR(20) NOT NULL DEFAULT '',
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID)
);

-- Migration: Add DepartmentID if it doesn't exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'professors') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'professors' AND column_name = 'departmentid'
        ) THEN
            ALTER TABLE Professors ADD COLUMN DepartmentID INT NULL;
            -- Add foreign key constraint if it doesn't exist
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.table_constraints 
                WHERE table_schema = 'public' 
                AND table_name = 'professors' 
                AND constraint_type = 'FOREIGN KEY'
                AND constraint_name LIKE '%departmentid%'
            ) THEN
                ALTER TABLE Professors ADD CONSTRAINT professors_departmentid_fkey 
                FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID);
            END IF;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS IX_Professors_DepartmentID ON Professors(DepartmentID);

-- Staff table 
CREATE TABLE IF NOT EXISTS Staff (
    UserID    INT PRIMARY KEY,
    DepartmentID INT NULL,  -- FK to Departments
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID)
);

-- Migration: Add DepartmentID if it doesn't exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'staff') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'staff' AND column_name = 'departmentid'
        ) THEN
            ALTER TABLE Staff ADD COLUMN DepartmentID INT NULL;
            -- Add foreign key constraint if it doesn't exist
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.table_constraints 
                WHERE table_schema = 'public' 
                AND table_name = 'staff' 
                AND constraint_type = 'FOREIGN KEY'
                AND constraint_name LIKE '%departmentid%'
            ) THEN
                ALTER TABLE Staff ADD CONSTRAINT staff_departmentid_fkey 
                FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID);
            END IF;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS IX_Staff_DepartmentID ON Staff(DepartmentID);

-- Admins table
CREATE TABLE IF NOT EXISTS Admins (
    UserID    INT PRIMARY KEY,
    RoleTitle VARCHAR(100) NOT NULL DEFAULT '',
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE
);

-- Rooms table 
CREATE TABLE IF NOT EXISTS Rooms (
    RoomID   SERIAL PRIMARY KEY,
    Code     VARCHAR(20) NOT NULL UNIQUE,
    Name     VARCHAR(100) NOT NULL,
    RoomTypeID INT NOT NULL,  -- FK to RoomTypes
    Capacity INT NOT NULL DEFAULT 0,  -- NOT NULL with default
    Location VARCHAR(100) NOT NULL DEFAULT '',
    StatusTypeID INT NOT NULL,  -- FK to StatusTypes (EntityType='ROOM')
    FOREIGN KEY (RoomTypeID) REFERENCES RoomTypes(RoomTypeID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID)
);

-- Migration: Handle transition from old schema (Type VARCHAR, Status VARCHAR) to new schema (RoomTypeID, StatusTypeID)
DO $$
DECLARE
    has_old_type BOOLEAN;
    has_old_status BOOLEAN;
    default_room_type_id INT;
    default_status_type_id INT;
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'rooms') THEN
        -- Check if old columns exist
        has_old_type := EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'rooms' AND column_name = 'type'
        );
        has_old_status := EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'rooms' AND column_name = 'status'
        );
        
        -- Ensure RoomTypes lookup table has required values
        INSERT INTO RoomTypes (TypeCode, TypeName) 
        VALUES ('CLASSROOM', 'Classroom') 
        ON CONFLICT (TypeCode) DO NOTHING;
        
        INSERT INTO RoomTypes (TypeCode, TypeName) 
        VALUES ('LAB', 'Laboratory') 
        ON CONFLICT (TypeCode) DO NOTHING;
        
        -- Ensure StatusTypes lookup table has required values for ROOM entity
        INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) 
        VALUES ('ROOM', 'AVAILABLE', 'Available') 
        ON CONFLICT (EntityType, StatusCode) DO NOTHING;
        
        INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) 
        VALUES ('ROOM', 'OCCUPIED', 'Occupied') 
        ON CONFLICT (EntityType, StatusCode) DO NOTHING;
        
        INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) 
        VALUES ('ROOM', 'MAINTENANCE', 'Maintenance') 
        ON CONFLICT (EntityType, StatusCode) DO NOTHING;
        
        -- Add RoomTypeID if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'rooms' AND column_name = 'roomtypeid'
        ) THEN
            ALTER TABLE Rooms ADD COLUMN RoomTypeID INT;
            
            -- Migrate data from old Type column if it exists
            IF has_old_type THEN
                UPDATE Rooms SET RoomTypeID = (
                    SELECT RoomTypeID FROM RoomTypes WHERE TypeCode = UPPER(Rooms.Type)
                ) WHERE RoomTypeID IS NULL;
            END IF;
            
            -- Set default if still NULL
            SELECT RoomTypeID INTO default_room_type_id FROM RoomTypes WHERE TypeCode = 'CLASSROOM' LIMIT 1;
            UPDATE Rooms SET RoomTypeID = default_room_type_id WHERE RoomTypeID IS NULL;
            
            -- Make NOT NULL after populating
            ALTER TABLE Rooms ALTER COLUMN RoomTypeID SET NOT NULL;
            
            -- Add foreign key constraint
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.table_constraints 
                WHERE table_schema = 'public' 
                AND table_name = 'rooms' 
                AND constraint_type = 'FOREIGN KEY'
                AND constraint_name LIKE '%roomtypeid%'
            ) THEN
                ALTER TABLE Rooms ADD CONSTRAINT rooms_roomtypeid_fkey 
                FOREIGN KEY (RoomTypeID) REFERENCES RoomTypes(RoomTypeID);
            END IF;
        END IF;
        
        -- Add StatusTypeID if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'rooms' AND column_name = 'statustypeid'
        ) THEN
            ALTER TABLE Rooms ADD COLUMN StatusTypeID INT;
            
            -- Migrate data from old Status column if it exists
            IF has_old_status THEN
                UPDATE Rooms SET StatusTypeID = (
                    SELECT StatusTypeID FROM StatusTypes 
                    WHERE EntityType = 'ROOM' AND StatusCode = UPPER(Rooms.Status)
                ) WHERE StatusTypeID IS NULL;
            END IF;
            
            -- Set default if still NULL
            SELECT StatusTypeID INTO default_status_type_id 
            FROM StatusTypes 
            WHERE EntityType = 'ROOM' AND StatusCode = 'AVAILABLE' 
            LIMIT 1;
            UPDATE Rooms SET StatusTypeID = default_status_type_id WHERE StatusTypeID IS NULL;
            
            -- Make NOT NULL after populating
            ALTER TABLE Rooms ALTER COLUMN StatusTypeID SET NOT NULL;
            
            -- Add foreign key constraint
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.table_constraints 
                WHERE table_schema = 'public' 
                AND table_name = 'rooms' 
                AND constraint_type = 'FOREIGN KEY'
                AND constraint_name LIKE '%statustypeid%'
            ) THEN
                ALTER TABLE Rooms ADD CONSTRAINT rooms_statustypeid_fkey 
                FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID);
            END IF;
        END IF;
        
        -- Add other new columns if they don't exist
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'rooms' AND column_name = 'capacity'
        ) THEN
            ALTER TABLE Rooms ADD COLUMN Capacity INT NOT NULL DEFAULT 0;
        ELSIF EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'rooms' 
            AND column_name = 'capacity' AND is_nullable = 'YES'
        ) THEN
            UPDATE Rooms SET Capacity = 0 WHERE Capacity IS NULL;
            ALTER TABLE Rooms ALTER COLUMN Capacity SET NOT NULL;
            ALTER TABLE Rooms ALTER COLUMN Capacity SET DEFAULT 0;
        END IF;
        
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'rooms' AND column_name = 'location'
        ) THEN
            ALTER TABLE Rooms ADD COLUMN Location VARCHAR(100) NOT NULL DEFAULT '';
        ELSIF EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'rooms' 
            AND column_name = 'location' AND is_nullable = 'YES'
        ) THEN
            UPDATE Rooms SET Location = '' WHERE Location IS NULL;
            ALTER TABLE Rooms ALTER COLUMN Location SET NOT NULL;
            ALTER TABLE Rooms ALTER COLUMN Location SET DEFAULT '';
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS IX_Rooms_Code ON Rooms(Code);
CREATE INDEX IF NOT EXISTS IX_Rooms_RoomTypeID ON Rooms(RoomTypeID);
CREATE INDEX IF NOT EXISTS IX_Rooms_StatusTypeID ON Rooms(StatusTypeID);

-- EquipmentType table 
CREATE TABLE IF NOT EXISTS EquipmentType (
    EquipmentTypeID SERIAL PRIMARY KEY,
    Name            VARCHAR(100) NOT NULL UNIQUE
);

-- RoomEquipment table 
CREATE TABLE IF NOT EXISTS RoomEquipment (
    RoomID          INT NOT NULL,
    EquipmentTypeID INT NOT NULL,
    Quantity        INT NOT NULL DEFAULT 1,
    PRIMARY KEY (RoomID, EquipmentTypeID),
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID) ON DELETE CASCADE,
    FOREIGN KEY (EquipmentTypeID) REFERENCES EquipmentType(EquipmentTypeID) ON DELETE CASCADE
);

-- MaintenanceTickets table 
CREATE TABLE IF NOT EXISTS MaintenanceTickets (
    TicketID            SERIAL PRIMARY KEY,
    RoomID              INT NOT NULL,
    ReporterUserID      INT NOT NULL,
    AssignedToUserID    INT NOT NULL DEFAULT 0,  
    Description         TEXT NOT NULL,
    StatusTypeID        INT NOT NULL,  
    CreatedDate         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ResolvedDate        TIMESTAMP NULL,  
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (ReporterUserID) REFERENCES Users(UserID),
    FOREIGN KEY (AssignedToUserID) REFERENCES Users(UserID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID),
    CHECK (AssignedToUserID = 0 OR AssignedToUserID > 0)  
);

CREATE INDEX IF NOT EXISTS IX_MaintenanceTickets_AssignedToUserID ON MaintenanceTickets(AssignedToUserID) WHERE AssignedToUserID > 0;
CREATE INDEX IF NOT EXISTS IX_MaintenanceTickets_RoomID ON MaintenanceTickets(RoomID);
CREATE INDEX IF NOT EXISTS IX_MaintenanceTickets_ReporterUserID ON MaintenanceTickets(ReporterUserID);
CREATE INDEX IF NOT EXISTS IX_MaintenanceTickets_StatusTypeID ON MaintenanceTickets(StatusTypeID);

-- Bookings table 
CREATE TABLE IF NOT EXISTS Bookings (
    BookingID     SERIAL PRIMARY KEY,
    RoomID         INT NOT NULL,
    UserID         INT NOT NULL,
    BookingDate    TIMESTAMP NOT NULL,
    EndDate        TIMESTAMP NOT NULL,
    Purpose        VARCHAR(200) NOT NULL DEFAULT '',
    StatusTypeID   INT NOT NULL,  -- FK to StatusTypes (EntityType='BOOKING')
    CreatedDate    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (UserID) REFERENCES Users(UserID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID),
    CHECK (EndDate > BookingDate)
);

CREATE INDEX IF NOT EXISTS IX_Bookings_RoomID ON Bookings(RoomID);
CREATE INDEX IF NOT EXISTS IX_Bookings_UserID ON Bookings(UserID);
CREATE INDEX IF NOT EXISTS IX_Bookings_BookingDate ON Bookings(BookingDate);
CREATE INDEX IF NOT EXISTS IX_Bookings_StatusTypeID ON Bookings(StatusTypeID);

-- Equipment table 
CREATE TABLE IF NOT EXISTS Equipment (
    EquipmentID      SERIAL PRIMARY KEY,
    EquipmentTypeID INT NOT NULL,
    SerialNumber    VARCHAR(100) NOT NULL DEFAULT '',  
    StatusTypeID    INT NOT NULL,  
    Location        VARCHAR(200) NOT NULL DEFAULT '',
    Notes           TEXT NOT NULL DEFAULT '',
    CreatedDate     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (EquipmentTypeID) REFERENCES EquipmentType(EquipmentTypeID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID)
);

CREATE INDEX IF NOT EXISTS IX_Equipment_EquipmentTypeID ON Equipment(EquipmentTypeID);
CREATE INDEX IF NOT EXISTS IX_Equipment_StatusTypeID ON Equipment(StatusTypeID);
CREATE INDEX IF NOT EXISTS IX_Equipment_SerialNumber ON Equipment(SerialNumber) WHERE SerialNumber != '';

-- EquipmentAllocation table 
CREATE TABLE IF NOT EXISTS EquipmentUserAllocations (
    AllocationID    SERIAL PRIMARY KEY,
    EquipmentID     INT NOT NULL,
    AllocatedToUserID INT NOT NULL,  
    AllocatedByUserID INT NOT NULL,
    AllocationDate  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ReturnDate      TIMESTAMP NULL,
    Notes           TEXT NOT NULL DEFAULT '',
    StatusTypeID    INT NOT NULL, 
    FOREIGN KEY (EquipmentID) REFERENCES Equipment(EquipmentID),
    FOREIGN KEY (AllocatedToUserID) REFERENCES Users(UserID),
    FOREIGN KEY (AllocatedByUserID) REFERENCES Users(UserID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID)
);

CREATE TABLE IF NOT EXISTS EquipmentDepartmentAllocations (
    AllocationID    SERIAL PRIMARY KEY,
    EquipmentID     INT NOT NULL,
    DepartmentID    INT NOT NULL,  
    AllocatedByUserID INT NOT NULL,
    AllocationDate  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ReturnDate      TIMESTAMP NULL,
    Notes           TEXT NOT NULL DEFAULT '',
    StatusTypeID    INT NOT NULL,
    FOREIGN KEY (EquipmentID) REFERENCES Equipment(EquipmentID),
    FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID),
    FOREIGN KEY (AllocatedByUserID) REFERENCES Users(UserID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID)
);

CREATE INDEX IF NOT EXISTS IX_EquipmentUserAllocations_EquipmentID ON EquipmentUserAllocations(EquipmentID);
CREATE INDEX IF NOT EXISTS IX_EquipmentUserAllocations_AllocatedToUserID ON EquipmentUserAllocations(AllocatedToUserID);
CREATE INDEX IF NOT EXISTS IX_EquipmentUserAllocations_StatusTypeID ON EquipmentUserAllocations(StatusTypeID);
CREATE INDEX IF NOT EXISTS IX_EquipmentDepartmentAllocations_EquipmentID ON EquipmentDepartmentAllocations(EquipmentID);
CREATE INDEX IF NOT EXISTS IX_EquipmentDepartmentAllocations_DepartmentID ON EquipmentDepartmentAllocations(DepartmentID);

-- SoftwareLicenses table 
CREATE TABLE IF NOT EXISTS SoftwareLicenses (
    LicenseID       SERIAL PRIMARY KEY,
    SoftwareName   VARCHAR(200) NOT NULL,
    LicenseKey      VARCHAR(500) NOT NULL DEFAULT '',  
    Vendor          VARCHAR(200) NOT NULL DEFAULT '',
    PurchaseDate    DATE NULL,
    ExpiryDate      DATE NULL,
    Cost            DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    Quantity        INT NOT NULL DEFAULT 1,
    UsedQuantity    INT NOT NULL DEFAULT 0,
    StatusTypeID    INT NOT NULL,  
    Notes           TEXT NOT NULL DEFAULT '',
    CreatedDate     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID)
);

CREATE INDEX IF NOT EXISTS IX_SoftwareLicenses_ExpiryDate ON SoftwareLicenses(ExpiryDate);
CREATE INDEX IF NOT EXISTS IX_SoftwareLicenses_StatusTypeID ON SoftwareLicenses(StatusTypeID);

-- AdmissionApplications table 
CREATE TABLE IF NOT EXISTS AdmissionApplications (
    ApplicationID    SERIAL PRIMARY KEY,
    FirstName        VARCHAR(100) NOT NULL,
    LastName         VARCHAR(100) NOT NULL,
    Email            VARCHAR(100) NOT NULL,
    PhoneNumber      VARCHAR(20) NOT NULL DEFAULT '',
    DateOfBirth      DATE NULL,
    Address          TEXT NOT NULL DEFAULT '',
    City             VARCHAR(100) NOT NULL DEFAULT '',
    State            VARCHAR(50) NOT NULL DEFAULT '',
    ZipCode          VARCHAR(20) NOT NULL DEFAULT '',
    Country          VARCHAR(100) NOT NULL DEFAULT '',
    Program          VARCHAR(100) NOT NULL DEFAULT '',
    PreviousEducation TEXT NOT NULL DEFAULT '',
    Documents        TEXT NOT NULL DEFAULT '',
    StatusTypeID     INT NOT NULL,  
    SubmittedDate    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ReviewedDate      TIMESTAMP NULL,
    ReviewedByUserID INT NULL,
    Notes            TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (ReviewedByUserID) REFERENCES Users(UserID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID)
);

CREATE INDEX IF NOT EXISTS IX_AdmissionApplications_StatusTypeID ON AdmissionApplications(StatusTypeID);
CREATE INDEX IF NOT EXISTS IX_AdmissionApplications_SubmittedDate ON AdmissionApplications(SubmittedDate);
CREATE INDEX IF NOT EXISTS IX_AdmissionApplications_Email ON AdmissionApplications(Email);

-- TranscriptRequests table 
CREATE TABLE IF NOT EXISTS TranscriptRequests (
    RequestID       SERIAL PRIMARY KEY,
    StudentUserID   INT NOT NULL,
    RequestDate     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    StatusTypeID    INT NOT NULL,  
    RequestedByUserID INT NOT NULL,
    ProcessedByUserID INT NULL,
    ProcessedDate   TIMESTAMP NULL,
    CompletedDate   TIMESTAMP NULL,
    PickupDate      TIMESTAMP NULL,
    Purpose         VARCHAR(500) NOT NULL DEFAULT '',
    Notes           TEXT NOT NULL DEFAULT '',
    PDFPath         VARCHAR(500) NOT NULL DEFAULT '',
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    FOREIGN KEY (RequestedByUserID) REFERENCES Users(UserID),
    FOREIGN KEY (ProcessedByUserID) REFERENCES Users(UserID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID)
);

CREATE INDEX IF NOT EXISTS IX_TranscriptRequests_StudentUserID ON TranscriptRequests(StudentUserID);
CREATE INDEX IF NOT EXISTS IX_TranscriptRequests_StatusTypeID ON TranscriptRequests(StatusTypeID);
CREATE INDEX IF NOT EXISTS IX_TranscriptRequests_RequestDate ON TranscriptRequests(RequestDate);

-- Courses table 
CREATE TABLE IF NOT EXISTS Courses (
    CourseID      SERIAL PRIMARY KEY,
    Code          VARCHAR(20) NOT NULL UNIQUE,
    Name          VARCHAR(200) NOT NULL,
    Description   TEXT NOT NULL DEFAULT '',
    Credits       INT NOT NULL DEFAULT 3 CHECK (Credits > 0 AND Credits <= 6),
    DepartmentID  INT NOT NULL,  
    SemesterID    INT NOT NULL,  
    CourseTypeID  INT NOT NULL,  
    ProfessorUserID INT NULL,  
    MaxSeats      INT NOT NULL DEFAULT 30 CHECK (MaxSeats > 0),
    CurrentSeats  INT NOT NULL DEFAULT 0 CHECK (CurrentSeats >= 0),
    IsActive      BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedDate   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID),
    FOREIGN KEY (SemesterID) REFERENCES Semesters(SemesterID),
    FOREIGN KEY (CourseTypeID) REFERENCES CourseTypes(CourseTypeID),
    FOREIGN KEY (ProfessorUserID) REFERENCES Users(UserID)
);

-- Migration: Add new columns if table exists from old schema
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'courses') THEN
        -- Add DepartmentID if it doesn't exist (NOT NULL, so need default)
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'courses' AND column_name = 'departmentid'
        ) THEN
            -- First add as nullable, then set a default, then make NOT NULL
            ALTER TABLE Courses ADD COLUMN DepartmentID INT;
            -- Set a default department ID (you may need to adjust this)
            UPDATE Courses SET DepartmentID = (
                SELECT DepartmentID FROM Departments LIMIT 1
            ) WHERE DepartmentID IS NULL;
            -- If no departments exist, we can't make it NOT NULL - leave it nullable for now
            -- ALTER TABLE Courses ALTER COLUMN DepartmentID SET NOT NULL;
        END IF;
        
        -- Add SemesterID if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'courses' AND column_name = 'semesterid'
        ) THEN
            ALTER TABLE Courses ADD COLUMN SemesterID INT;
            -- Set default semester
            UPDATE Courses SET SemesterID = (
                SELECT SemesterID FROM Semesters WHERE IsActive = TRUE LIMIT 1
            ) WHERE SemesterID IS NULL;
        END IF;
        
        -- Add CourseTypeID if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'courses' AND column_name = 'coursetypeid'
        ) THEN
            ALTER TABLE Courses ADD COLUMN CourseTypeID INT;
            -- Set default course type
            UPDATE Courses SET CourseTypeID = (
                SELECT CourseTypeID FROM CourseTypes LIMIT 1
            ) WHERE CourseTypeID IS NULL;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS IX_Courses_Code ON Courses(Code);
CREATE INDEX IF NOT EXISTS IX_Courses_DepartmentID ON Courses(DepartmentID);
CREATE INDEX IF NOT EXISTS IX_Courses_SemesterID ON Courses(SemesterID);
CREATE INDEX IF NOT EXISTS IX_Courses_CourseTypeID ON Courses(CourseTypeID);
CREATE INDEX IF NOT EXISTS IX_Courses_IsActive ON Courses(IsActive);

-- CourseProfessors table 
CREATE TABLE IF NOT EXISTS CourseProfessors (
    CourseProfessorID SERIAL PRIMARY KEY,
    CourseID          INT NOT NULL,
    ProfessorUserID   INT NOT NULL,
    CreatedDate       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (ProfessorUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    UNIQUE (CourseID, ProfessorUserID)
);

CREATE INDEX IF NOT EXISTS IX_CourseProfessors_CourseID ON CourseProfessors(CourseID);
CREATE INDEX IF NOT EXISTS IX_CourseProfessors_ProfessorUserID ON CourseProfessors(ProfessorUserID);

-- Prerequisites table 
CREATE TABLE IF NOT EXISTS Prerequisites (
    PrerequisiteID SERIAL PRIMARY KEY,
    CourseID       INT NOT NULL,
    PrerequisiteCourseID INT NOT NULL,
    CreatedDate    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (PrerequisiteCourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    UNIQUE (CourseID, PrerequisiteCourseID),
    CHECK (CourseID != PrerequisiteCourseID)
);

CREATE INDEX IF NOT EXISTS IX_Prerequisites_CourseID ON Prerequisites(CourseID);
CREATE INDEX IF NOT EXISTS IX_Prerequisites_PrerequisiteCourseID ON Prerequisites(PrerequisiteCourseID);

-- CourseAttributes table 
CREATE TABLE IF NOT EXISTS CourseAttributes (
    AttributeID   SERIAL PRIMARY KEY,
    CourseID      INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue TEXT NOT NULL DEFAULT '',  
    AttributeType VARCHAR(20) DEFAULT 'TEXT' CHECK (AttributeType IN ('TEXT', 'NUMBER', 'BOOLEAN', 'DATE', 'JSON')),
    CreatedDate   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    UNIQUE (CourseID, AttributeName)
);

CREATE INDEX IF NOT EXISTS IX_CourseAttributes_CourseID ON CourseAttributes(CourseID);
CREATE INDEX IF NOT EXISTS IX_CourseAttributes_AttributeName ON CourseAttributes(AttributeName);

-- Enrollments table 
CREATE TABLE IF NOT EXISTS Enrollments (
    EnrollmentID  SERIAL PRIMARY KEY,
    StudentUserID INT NOT NULL,
    CourseID      INT NOT NULL,
    EnrollmentDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    StatusTypeID  INT NOT NULL,  
    Grade         VARCHAR(5) NOT NULL DEFAULT '',  
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID)
);

CREATE UNIQUE INDEX IF NOT EXISTS IX_Enrollments_UniqueActiveEnrollment 
ON Enrollments(StudentUserID, CourseID) 
WHERE StatusTypeID IN (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'ENROLLMENT' AND StatusCode = 'ENROLLED');

CREATE INDEX IF NOT EXISTS IX_Enrollments_StudentUserID ON Enrollments(StudentUserID);
CREATE INDEX IF NOT EXISTS IX_Enrollments_CourseID ON Enrollments(CourseID);
CREATE INDEX IF NOT EXISTS IX_Enrollments_StatusTypeID ON Enrollments(StatusTypeID);
CREATE INDEX IF NOT EXISTS IX_Enrollments_Student_Course_Status ON Enrollments(StudentUserID, CourseID, StatusTypeID);

-- Assignments table 
CREATE TABLE IF NOT EXISTS Assignments (
    AssignmentID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    Instructions TEXT NOT NULL,
    DueDate TIMESTAMP NOT NULL,
    TotalPoints INT NOT NULL,
    SubmissionType VARCHAR(50) DEFAULT 'TEXT' CHECK (SubmissionType IN ('FILE', 'TEXT', 'BOTH')),
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID)
);

CREATE INDEX IF NOT EXISTS IX_Assignments_CourseID ON Assignments(CourseID);
CREATE INDEX IF NOT EXISTS IX_Assignments_DueDate ON Assignments(DueDate);

-- AssignmentAttributes table 
CREATE TABLE IF NOT EXISTS AssignmentAttributes (
    AttributeID SERIAL PRIMARY KEY,
    AssignmentID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (AssignmentID) REFERENCES Assignments(AssignmentID)
);

CREATE INDEX IF NOT EXISTS IX_AssignmentAttributes_AssignmentID ON AssignmentAttributes(AssignmentID);

-- AssignmentSubmissions table 
CREATE TABLE IF NOT EXISTS AssignmentSubmissions (
    SubmissionID SERIAL PRIMARY KEY,
    AssignmentID INT NOT NULL,
    StudentUserID INT NOT NULL,
    SubmissionText TEXT NOT NULL DEFAULT '',
    FileName VARCHAR(255) NOT NULL DEFAULT '',
    SubmittedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    Score INT NULL,  
    Feedback TEXT NOT NULL DEFAULT '',
    StatusTypeID INT NOT NULL, 
    GradedDate TIMESTAMP NULL,
    FOREIGN KEY (AssignmentID) REFERENCES Assignments(AssignmentID),
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID)
);

CREATE INDEX IF NOT EXISTS IX_AssignmentSubmissions_AssignmentID ON AssignmentSubmissions(AssignmentID);
CREATE INDEX IF NOT EXISTS IX_AssignmentSubmissions_StudentUserID ON AssignmentSubmissions(StudentUserID);
CREATE INDEX IF NOT EXISTS IX_AssignmentSubmissions_StatusTypeID ON AssignmentSubmissions(StatusTypeID);

-- CourseMaterials table 
CREATE TABLE IF NOT EXISTS CourseMaterials (
    MaterialID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    Title VARCHAR(255) NOT NULL,
    Description TEXT NOT NULL DEFAULT '',
    MaterialType VARCHAR(20) NOT NULL CHECK (MaterialType IN ('LECTURE', 'READING', 'VIDEO', 'LINK')),
    FileName VARCHAR(255) NOT NULL DEFAULT '',
    FilePath VARCHAR(500) NOT NULL,
    FileSizeBytes BIGINT DEFAULT 0,
    UploadDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UploadedByUserID INT NOT NULL,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (UploadedByUserID) REFERENCES Users(UserID)
);

CREATE INDEX IF NOT EXISTS IX_CourseMaterials_CourseID ON CourseMaterials(CourseID);
CREATE INDEX IF NOT EXISTS IX_CourseMaterials_UploadDate ON CourseMaterials(UploadDate DESC);
CREATE INDEX IF NOT EXISTS IX_CourseMaterials_UploadedBy ON CourseMaterials(UploadedByUserID);

-- Quizzes table 
CREATE TABLE IF NOT EXISTS Quizzes (
    QuizID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    Instructions TEXT NOT NULL DEFAULT '',
    TotalPoints INT NOT NULL,
    DueDate TIMESTAMP NOT NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS IX_Quizzes_CourseID ON Quizzes(CourseID);
CREATE INDEX IF NOT EXISTS IX_Quizzes_DueDate ON Quizzes(DueDate);

-- QuizAttributes table 
CREATE TABLE IF NOT EXISTS QuizAttributes (
    AttributeID SERIAL PRIMARY KEY,
    QuizID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (QuizID) REFERENCES Quizzes(QuizID) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS IX_QuizAttributes_QuizID ON QuizAttributes(QuizID);

-- QuizQuestions table 
CREATE TABLE IF NOT EXISTS QuizQuestions (
    QuestionID SERIAL PRIMARY KEY,
    QuizID INT NOT NULL,
    QuestionNumber INT NOT NULL,
    QuestionText TEXT NOT NULL,
    QuestionType VARCHAR(20) NOT NULL CHECK (QuestionType IN ('MCQ', 'WRITTEN')),
    Points INT NOT NULL DEFAULT 1,
    FOREIGN KEY (QuizID) REFERENCES Quizzes(QuizID) ON DELETE CASCADE,
    UNIQUE(QuizID, QuestionNumber)
);

CREATE INDEX IF NOT EXISTS IX_QuizQuestions_QuizID ON QuizQuestions(QuizID);

-- QuizQuestionOptions table 
CREATE TABLE IF NOT EXISTS QuizQuestionOptions (
    OptionID SERIAL PRIMARY KEY,
    QuestionID INT NOT NULL,
    OptionText TEXT NOT NULL,
    IsCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    OptionOrder INT NOT NULL,
    FOREIGN KEY (QuestionID) REFERENCES QuizQuestions(QuestionID) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS IX_QuizQuestionOptions_QuestionID ON QuizQuestionOptions(QuestionID);

-- QuizAttempts table 
CREATE TABLE IF NOT EXISTS QuizAttempts (
    AttemptID SERIAL PRIMARY KEY,
    QuizID INT NOT NULL,
    StudentUserID INT NOT NULL,
    AttemptNumber INT NOT NULL DEFAULT 1,
    StartedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CompletedDate TIMESTAMP NULL,
    Score INT NULL,
    StatusTypeID INT NOT NULL,  
    FOREIGN KEY (QuizID) REFERENCES Quizzes(QuizID) ON DELETE CASCADE,
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID),
    UNIQUE(QuizID, StudentUserID, AttemptNumber)
);

CREATE INDEX IF NOT EXISTS IX_QuizAttempts_QuizID ON QuizAttempts(QuizID);
CREATE INDEX IF NOT EXISTS IX_QuizAttempts_StudentUserID ON QuizAttempts(StudentUserID);
CREATE INDEX IF NOT EXISTS IX_QuizAttempts_StatusTypeID ON QuizAttempts(StatusTypeID);

-- Exams table 
CREATE TABLE IF NOT EXISTS Exams (
    ExamID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    ExamDate TIMESTAMP NOT NULL,
    DurationMinutes INT NOT NULL,
    Location VARCHAR(200) NOT NULL DEFAULT '',
    TotalPoints INT NOT NULL,
    Instructions TEXT NOT NULL DEFAULT '',
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS IX_Exams_CourseID ON Exams(CourseID);
CREATE INDEX IF NOT EXISTS IX_Exams_ExamDate ON Exams(ExamDate);

-- ExamAttributes table 
CREATE TABLE IF NOT EXISTS ExamAttributes (
    AttributeID SERIAL PRIMARY KEY,
    ExamID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (ExamID) REFERENCES Exams(ExamID) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS IX_ExamAttributes_ExamID ON ExamAttributes(ExamID);

-- ExamGrades table 
CREATE TABLE IF NOT EXISTS ExamGrades (
    ExamGradeID SERIAL PRIMARY KEY,
    ExamID INT NOT NULL,
    StudentUserID INT NOT NULL,
    PointsEarned INT NULL,  
    Comments TEXT NOT NULL DEFAULT '',
    GradedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ExamID) REFERENCES Exams(ExamID) ON DELETE CASCADE,
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    UNIQUE(ExamID, StudentUserID)
);

CREATE INDEX IF NOT EXISTS IX_ExamGrades_ExamID ON ExamGrades(ExamID);
CREATE INDEX IF NOT EXISTS IX_ExamGrades_StudentUserID ON ExamGrades(StudentUserID);

-- CourseGradeWeights table 
CREATE TABLE IF NOT EXISTS CourseGradeWeights (
    CourseID INT PRIMARY KEY,
    AssignmentsWeight DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    QuizzesWeight DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    ExamsWeight DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    CONSTRAINT CHK_WeightsSum CHECK (AssignmentsWeight + QuizzesWeight + ExamsWeight = 100.00)
);

CREATE INDEX IF NOT EXISTS IX_CourseGradeWeights_CourseID ON CourseGradeWeights(CourseID);

-- StaffProfiles table 
CREATE TABLE IF NOT EXISTS StaffProfiles (
    StaffID SERIAL PRIMARY KEY,
    UserID INT NULL,
    Name VARCHAR(100) NOT NULL,
    Role VARCHAR(100) NOT NULL,
    DepartmentID INT NOT NULL,  
    Email VARCHAR(100) UNIQUE NOT NULL,
    OfficeHours VARCHAR(50) NOT NULL DEFAULT '',
    OfficeLocation VARCHAR(100) NOT NULL DEFAULT '',
    Phone VARCHAR(20) NOT NULL DEFAULT '',
    HireDate DATE NULL,
    Bio TEXT NOT NULL DEFAULT '',
    IsActive BOOLEAN DEFAULT TRUE,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE SET NULL,
    FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID)
);

-- Migration: Add DepartmentID if it doesn't exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'staffprofiles') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'staffprofiles' AND column_name = 'departmentid'
        ) THEN
            -- Add as nullable first
            ALTER TABLE StaffProfiles ADD COLUMN DepartmentID INT;
            -- Set a default department ID
            UPDATE StaffProfiles SET DepartmentID = (
                SELECT DepartmentID FROM Departments LIMIT 1
            ) WHERE DepartmentID IS NULL;
            -- Note: Can't make NOT NULL if there are rows without a valid department
            -- You may need to handle this manually based on your data
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_staff_department ON StaffProfiles(DepartmentID);
CREATE INDEX IF NOT EXISTS idx_staff_name ON StaffProfiles(Name);
CREATE INDEX IF NOT EXISTS idx_staff_active ON StaffProfiles(IsActive);

-- Messages table 
CREATE TABLE IF NOT EXISTS Messages (
    MessageID SERIAL PRIMARY KEY,
    SenderUserID INT NOT NULL,
    ReceiverUserID INT NOT NULL,
    Subject VARCHAR(200) NOT NULL,
    MessageBody TEXT NOT NULL,
    SentDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    IsRead BOOLEAN DEFAULT FALSE,
    ParentMessageID INT NULL,
    ThreadID INT NULL,
    MessageType VARCHAR(20) DEFAULT 'GENERAL' CHECK (MessageType IN ('GENERAL', 'PARENT_TEACHER', 'STUDENT_STAFF', 'SYSTEM')),
    FOREIGN KEY (SenderUserID) REFERENCES Users(UserID),
    FOREIGN KEY (ReceiverUserID) REFERENCES Users(UserID),
    FOREIGN KEY (ParentMessageID) REFERENCES Messages(MessageID),
    FOREIGN KEY (ThreadID) REFERENCES MessageThreads(ThreadID) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS IX_Messages_Receiver ON Messages(ReceiverUserID);
CREATE INDEX IF NOT EXISTS IX_Messages_Sender ON Messages(SenderUserID);
CREATE INDEX IF NOT EXISTS IX_Messages_ThreadID ON Messages(ThreadID);
CREATE INDEX IF NOT EXISTS IX_Messages_MessageType ON Messages(MessageType);

-- CourseStaff table 
CREATE TABLE IF NOT EXISTS CourseStaff (
    CourseStaffID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    StaffUserID INT NOT NULL,
    AssignmentDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (StaffUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    UNIQUE (CourseID, StaffUserID)
);

CREATE INDEX IF NOT EXISTS IX_CourseStaff_CourseID ON CourseStaff(CourseID);
CREATE INDEX IF NOT EXISTS IX_CourseStaff_StaffUserID ON CourseStaff(StaffUserID);

-- LeaveRequests table 
CREATE TABLE IF NOT EXISTS LeaveRequests (
    LeaveRequestID SERIAL PRIMARY KEY,
    StaffUserID INT NOT NULL,
    LeaveTypeID INT NOT NULL,  
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    NumberOfDays INT NOT NULL,
    Reason TEXT NOT NULL DEFAULT '',
    StatusTypeID INT NOT NULL,  
    SubmittedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ReviewedByUserID INT NULL,
    ReviewedDate TIMESTAMP NULL,
    RejectionReason TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (StaffUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (ReviewedByUserID) REFERENCES Users(UserID),
    FOREIGN KEY (LeaveTypeID) REFERENCES LeaveTypes(LeaveTypeID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID),
    CHECK (EndDate >= StartDate),
    CHECK (NumberOfDays > 0)
);

CREATE INDEX IF NOT EXISTS IX_LeaveRequests_StaffUserID ON LeaveRequests(StaffUserID);
CREATE INDEX IF NOT EXISTS IX_LeaveRequests_StatusTypeID ON LeaveRequests(StatusTypeID);
CREATE INDEX IF NOT EXISTS IX_LeaveRequests_StartDate ON LeaveRequests(StartDate);
CREATE INDEX IF NOT EXISTS IX_LeaveRequests_ReviewedByUserID ON LeaveRequests(ReviewedByUserID);
CREATE INDEX IF NOT EXISTS IX_LeaveRequests_LeaveTypeID ON LeaveRequests(LeaveTypeID);

-- PayrollInformation table 
CREATE TABLE IF NOT EXISTS PayrollInformation (
    PayrollID SERIAL PRIMARY KEY,
    StaffUserID INT NOT NULL,
    PayPeriodStart DATE NOT NULL,
    PayPeriodEnd DATE NOT NULL,
    PayDate DATE NOT NULL,
    PayFrequencyID INT NOT NULL,  
    EffectiveDate DATE NOT NULL,
    BaseSalary DECIMAL(10,2) NOT NULL,
    OvertimePay DECIMAL(10,2) DEFAULT 0.00,
    Bonuses DECIMAL(10,2) DEFAULT 0.00,
    GrossPay DECIMAL(10,2) NOT NULL,
    TaxDeduction DECIMAL(10,2) DEFAULT 0.00,
    InsuranceDeduction DECIMAL(10,2) DEFAULT 0.00,
    OtherDeductions DECIMAL(10,2) DEFAULT 0.00,
    TotalDeductions DECIMAL(10,2) DEFAULT 0.00,
    NetPay DECIMAL(10,2) NOT NULL,
    PaymentMethodID INT NOT NULL,  
    Notes TEXT NOT NULL DEFAULT '',
    CreatedByUserID INT NULL,
    CreatedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (StaffUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (CreatedByUserID) REFERENCES Users(UserID),
    FOREIGN KEY (PayFrequencyID) REFERENCES PayFrequencies(PayFrequencyID),
    FOREIGN KEY (PaymentMethodID) REFERENCES PaymentMethods(PaymentMethodID),
    CHECK (PayPeriodEnd >= PayPeriodStart),
    CHECK (GrossPay >= 0),
    CHECK (NetPay >= 0),
    CHECK (TotalDeductions >= 0)
);



CREATE INDEX IF NOT EXISTS IX_PayrollInformation_StaffUserID ON PayrollInformation(StaffUserID);
CREATE INDEX IF NOT EXISTS IX_PayrollInformation_PayDate ON PayrollInformation(PayDate DESC);
CREATE INDEX IF NOT EXISTS IX_PayrollInformation_PayPeriodStart ON PayrollInformation(PayPeriodStart);

-- BenefitsInformation table 
CREATE TABLE IF NOT EXISTS BenefitsInformation (
    BenefitID SERIAL PRIMARY KEY,
    StaffUserID INT NOT NULL,
    BenefitTypeID INT NOT NULL,  
    BenefitName VARCHAR(200) NOT NULL,
    CoverageAmount DECIMAL(10,2) NULL,
    CoverageDetails TEXT NOT NULL DEFAULT '',
    StartDate DATE NOT NULL,
    EndDate DATE NULL,
    StatusTypeID INT NOT NULL,  
    Provider VARCHAR(200) NOT NULL DEFAULT '',
    PolicyNumber VARCHAR(100) NOT NULL DEFAULT '',
    Notes TEXT NOT NULL DEFAULT '',
    CreatedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedByUserID INT NULL,
    FOREIGN KEY (StaffUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (UpdatedByUserID) REFERENCES Users(UserID),
    FOREIGN KEY (BenefitTypeID) REFERENCES BenefitTypes(BenefitTypeID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID),
    CHECK (EndDate IS NULL OR EndDate >= StartDate)
);

CREATE INDEX IF NOT EXISTS IX_BenefitsInformation_StaffUserID ON BenefitsInformation(StaffUserID);
CREATE INDEX IF NOT EXISTS IX_BenefitsInformation_BenefitTypeID ON BenefitsInformation(BenefitTypeID);
CREATE INDEX IF NOT EXISTS IX_BenefitsInformation_StatusTypeID ON BenefitsInformation(StatusTypeID);

-- Parents table 
CREATE TABLE IF NOT EXISTS Parents (
    ParentID SERIAL PRIMARY KEY,
    UserID INT NOT NULL UNIQUE,
    FirstName VARCHAR(100) NOT NULL,
    LastName VARCHAR(100) NOT NULL,
    PhoneNumber VARCHAR(20) NOT NULL DEFAULT '',
    Email VARCHAR(100) NOT NULL DEFAULT '',
    Address TEXT NOT NULL DEFAULT '',
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS IX_Parents_UserID ON Parents(UserID);

-- StudentParentRelationship table 
CREATE TABLE IF NOT EXISTS StudentParentRelationship (
    RelationshipID SERIAL PRIMARY KEY,
    StudentUserID INT NOT NULL,
    ParentUserID INT NOT NULL,
    RelationshipTypeID INT NOT NULL,  
    IsPrimary BOOLEAN DEFAULT FALSE,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (ParentUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (RelationshipTypeID) REFERENCES RelationshipTypes(RelationshipTypeID),
    UNIQUE (StudentUserID, ParentUserID)
);

CREATE INDEX IF NOT EXISTS IX_StudentParentRelationship_Student ON StudentParentRelationship(StudentUserID);
CREATE INDEX IF NOT EXISTS IX_StudentParentRelationship_Parent ON StudentParentRelationship(ParentUserID);
CREATE INDEX IF NOT EXISTS IX_StudentParentRelationship_RelationshipTypeID ON StudentParentRelationship(RelationshipTypeID);

-- MessageThreads table 
CREATE TABLE IF NOT EXISTS MessageThreads (
    ThreadID SERIAL PRIMARY KEY,
    ParentUserID INT NOT NULL,
    TeacherUserID INT NOT NULL,
    StudentUserID INT NOT NULL,
    Subject VARCHAR(200) NOT NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    LastMessageDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ParentUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (TeacherUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS IX_MessageThreads_Parent ON MessageThreads(ParentUserID);
CREATE INDEX IF NOT EXISTS IX_MessageThreads_Teacher ON MessageThreads(TeacherUserID);
CREATE INDEX IF NOT EXISTS IX_MessageThreads_Student ON MessageThreads(StudentUserID);

-- ForumPosts table 
CREATE TABLE IF NOT EXISTS ForumPosts (
    PostID SERIAL PRIMARY KEY,
    AuthorUserID INT NOT NULL,
    CourseID INT NULL,
    Topic VARCHAR(100) NOT NULL DEFAULT '',
    Title VARCHAR(200) NOT NULL,
    Content TEXT NOT NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    LastModifiedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    IsPinned BOOLEAN DEFAULT FALSE,
    IsLocked BOOLEAN DEFAULT FALSE,
    ViewCount INT DEFAULT 0,
    ReplyCount INT DEFAULT 0,
    FOREIGN KEY (AuthorUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS IX_ForumPosts_Author ON ForumPosts(AuthorUserID);
CREATE INDEX IF NOT EXISTS IX_ForumPosts_Course ON ForumPosts(CourseID);
CREATE INDEX IF NOT EXISTS IX_ForumPosts_CreatedDate ON ForumPosts(CreatedDate DESC);
CREATE INDEX IF NOT EXISTS IX_ForumPosts_Topic ON ForumPosts(Topic);

-- ForumComments table 
CREATE TABLE IF NOT EXISTS ForumComments (
    CommentID SERIAL PRIMARY KEY,
    PostID INT NOT NULL,
    AuthorUserID INT NOT NULL,
    Content TEXT NOT NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    IsEdited BOOLEAN DEFAULT FALSE,
    EditedDate TIMESTAMP NULL,
    FOREIGN KEY (PostID) REFERENCES ForumPosts(PostID) ON DELETE CASCADE,
    FOREIGN KEY (AuthorUserID) REFERENCES Users(UserID) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS IX_ForumComments_Post ON ForumComments(PostID);
CREATE INDEX IF NOT EXISTS IX_ForumComments_Author ON ForumComments(AuthorUserID);
CREATE INDEX IF NOT EXISTS IX_ForumComments_CreatedDate ON ForumComments(CreatedDate);

-- Events table 
CREATE TABLE IF NOT EXISTS Events (
    EventID SERIAL PRIMARY KEY,
    CreatedByUserID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    Description TEXT NOT NULL DEFAULT '',
    EventDate DATE NOT NULL,
    StartTime TIME NOT NULL,
    EndTime TIME NULL,
    Location VARCHAR(200) NOT NULL DEFAULT '',
    EventTypeID INT NOT NULL, 
    IsPublic BOOLEAN DEFAULT TRUE,
    IsRecurring BOOLEAN DEFAULT FALSE,
    RecurrencePattern VARCHAR(50) NOT NULL DEFAULT '',
    RecurrenceEndDate DATE NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    LastModifiedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CreatedByUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (EventTypeID) REFERENCES EventTypes(EventTypeID)
);

CREATE INDEX IF NOT EXISTS IX_Events_EventDate ON Events(EventDate);
CREATE INDEX IF NOT EXISTS IX_Events_CreatedBy ON Events(CreatedByUserID);
CREATE INDEX IF NOT EXISTS IX_Events_EventTypeID ON Events(EventTypeID);
CREATE INDEX IF NOT EXISTS IX_Events_IsPublic ON Events(IsPublic);

-- EventReminders table 
CREATE TABLE IF NOT EXISTS EventReminders (
    ReminderID SERIAL PRIMARY KEY,
    EventID INT NOT NULL,
    UserID INT NOT NULL,
    ReminderTime TIMESTAMP NOT NULL,
    IsSent BOOLEAN DEFAULT FALSE,
    SentDate TIMESTAMP NULL,
    ReminderTypeID INT NOT NULL,  
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (EventID) REFERENCES Events(EventID) ON DELETE CASCADE,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (ReminderTypeID) REFERENCES ReminderTypes(ReminderTypeID),
    UNIQUE (EventID, UserID, ReminderTime)
);

CREATE INDEX IF NOT EXISTS IX_EventReminders_Event ON EventReminders(EventID);
CREATE INDEX IF NOT EXISTS IX_EventReminders_User ON EventReminders(UserID);
CREATE INDEX IF NOT EXISTS IX_EventReminders_ReminderTime ON EventReminders(ReminderTime);
CREATE INDEX IF NOT EXISTS IX_EventReminders_IsSent ON EventReminders(IsSent);

-- EventAttendees table 
CREATE TABLE IF NOT EXISTS EventAttendees (
    AttendeeID SERIAL PRIMARY KEY,
    EventID INT NOT NULL,
    UserID INT NOT NULL,
    RSVPStatusID INT NOT NULL,  
    RSVPDate TIMESTAMP NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (EventID) REFERENCES Events(EventID) ON DELETE CASCADE,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (RSVPStatusID) REFERENCES RSVPStatuses(RSVPStatusID),
    UNIQUE (EventID, UserID)
);

CREATE INDEX IF NOT EXISTS IX_EventAttendees_Event ON EventAttendees(EventID);
CREATE INDEX IF NOT EXISTS IX_EventAttendees_User ON EventAttendees(UserID);
CREATE INDEX IF NOT EXISTS IX_EventAttendees_RSVPStatusID ON EventAttendees(RSVPStatusID);

-- ============================================================================
-- FINAL VERIFICATION: Ensure all required columns exist (safety net)
-- This runs at the end to catch any columns that might have been missed
-- ============================================================================
DO $$
BEGIN
    -- STUDENT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('STUDENT', 'ACTIVE', 'Active'),
        ('STUDENT', 'INACTIVE', 'Inactive'),
        ('STUDENT', 'GRADUATED', 'Graduated'),
        ('STUDENT', 'SUSPENDED', 'Suspended'),
        ('STUDENT', 'WITHDRAWN', 'Withdrawn')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- ROOM statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('ROOM', 'AVAILABLE', 'Available'),
        ('ROOM', 'OCCUPIED', 'Occupied'),
        ('ROOM', 'MAINTENANCE', 'Maintenance')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- TICKET statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('TICKET', 'NEW', 'New'),
        ('TICKET', 'IN_PROGRESS', 'In Progress'),
        ('TICKET', 'RESOLVED', 'Resolved')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- BOOKING statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('BOOKING', 'CONFIRMED', 'Confirmed'),
        ('BOOKING', 'CANCELLED', 'Cancelled')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- EQUIPMENT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('EQUIPMENT', 'AVAILABLE', 'Available'),
        ('EQUIPMENT', 'ALLOCATED', 'Allocated'),
        ('EQUIPMENT', 'MAINTENANCE', 'Maintenance'),
        ('EQUIPMENT', 'RETIRED', 'Retired')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- LICENSE statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('LICENSE', 'ACTIVE', 'Active'),
        ('LICENSE', 'EXPIRED', 'Expired'),
        ('LICENSE', 'CANCELLED', 'Cancelled')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- APPLICATION statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('APPLICATION', 'SUBMITTED', 'Submitted'),
        ('APPLICATION', 'UNDER_REVIEW', 'Under Review'),
        ('APPLICATION', 'ACCEPTED', 'Accepted'),
        ('APPLICATION', 'REJECTED', 'Rejected')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- TRANSCRIPT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('TRANSCRIPT', 'PENDING', 'Pending'),
        ('TRANSCRIPT', 'IN_PROGRESS', 'In Progress'),
        ('TRANSCRIPT', 'READY_FOR_PICKUP', 'Ready for Pickup'),
        ('TRANSCRIPT', 'COMPLETED', 'Completed'),
        ('TRANSCRIPT', 'CANCELLED', 'Cancelled')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- ENROLLMENT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('ENROLLMENT', 'ENROLLED', 'Enrolled'),
        ('ENROLLMENT', 'DROPPED', 'Dropped'),
        ('ENROLLMENT', 'COMPLETED', 'Completed'),
        ('ENROLLMENT', 'FAILED', 'Failed')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- LEAVE statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('LEAVE', 'PENDING', 'Pending'),
        ('LEAVE', 'APPROVED', 'Approved'),
        ('LEAVE', 'REJECTED', 'Rejected'),
        ('LEAVE', 'CANCELLED', 'Cancelled')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- BENEFIT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('BENEFIT', 'ACTIVE', 'Active'),
        ('BENEFIT', 'INACTIVE', 'Inactive'),
        ('BENEFIT', 'EXPIRED', 'Expired'),
        ('BENEFIT', 'CANCELLED', 'Cancelled')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- SUBMISSION statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('SUBMISSION', 'SUBMITTED', 'Submitted'),
        ('SUBMISSION', 'GRADED', 'Graded')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- QUIZ_ATTEMPT statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('QUIZ_ATTEMPT', 'IN_PROGRESS', 'In Progress'),
        ('QUIZ_ATTEMPT', 'COMPLETED', 'Completed'),
        ('QUIZ_ATTEMPT', 'TIMED_OUT', 'Timed Out')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
    
    -- ALLOCATION statuses
    INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
        ('ALLOCATION', 'ACTIVE', 'Active'),
        ('ALLOCATION', 'RETURNED', 'Returned')
    ON CONFLICT (EntityType, StatusCode) DO NOTHING;
END $$;

-- Populate RoomTypes
INSERT INTO RoomTypes (TypeCode, TypeName) VALUES 
    ('CLASSROOM', 'Classroom'),
    ('LAB', 'Laboratory')
ON CONFLICT (TypeCode) DO NOTHING;

-- Populate other essential lookup tables
INSERT INTO UserTypes (TypeCode, TypeName) VALUES 
    ('STUDENT', 'Student'),
    ('PROFESSOR', 'Professor'),
    ('STAFF', 'Staff'),
    ('ADMIN', 'Administrator'),
    ('PARENT', 'Parent'),
    ('HR_ADMIN', 'HR Administrator'),
    ('GUEST', 'Guest')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO RelationshipTypes (TypeCode, TypeName) VALUES 
    ('PARENT', 'Parent'),
    ('GUARDIAN', 'Guardian'),
    ('EMERGENCY_CONTACT', 'Emergency Contact')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO LeaveTypes (TypeCode, TypeName) VALUES 
    ('SICK', 'Sick Leave'),
    ('VACATION', 'Vacation'),
    ('PERSONAL', 'Personal'),
    ('MATERNITY', 'Maternity'),
    ('PATERNITY', 'Paternity'),
    ('BEREAVEMENT', 'Bereavement'),
    ('OTHER', 'Other')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO BenefitTypes (TypeCode, TypeName) VALUES 
    ('HEALTH_INSURANCE', 'Health Insurance'),
    ('DENTAL_INSURANCE', 'Dental Insurance'),
    ('VISION_INSURANCE', 'Vision Insurance'),
    ('LIFE_INSURANCE', 'Life Insurance'),
    ('RETIREMENT', 'Retirement'),
    ('VACATION_DAYS', 'Vacation Days'),
    ('SICK_DAYS', 'Sick Days'),
    ('OTHER', 'Other')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO EventTypes (TypeCode, TypeName) VALUES 
    ('GENERAL', 'General'),
    ('ACADEMIC', 'Academic'),
    ('SOCIAL', 'Social'),
    ('SPORTS', 'Sports'),
    ('ADMINISTRATIVE', 'Administrative'),
    ('HOLIDAY', 'Holiday')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO CourseTypes (TypeCode, TypeName) VALUES 
    ('CORE', 'Core'),
    ('ELECTIVE', 'Elective')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO PaymentMethods (MethodCode, MethodName) VALUES 
    ('DIRECT_DEPOSIT', 'Direct Deposit'),
    ('CHECK', 'Check'),
    ('WIRE_TRANSFER', 'Wire Transfer')
ON CONFLICT (MethodCode) DO NOTHING;

INSERT INTO ReminderTypes (TypeCode, TypeName) VALUES 
    ('EMAIL', 'Email'),
    ('IN_APP', 'In-App'),
    ('BOTH', 'Both')
ON CONFLICT (TypeCode) DO NOTHING;

INSERT INTO RSVPStatuses (StatusCode, StatusName) VALUES 
    ('PENDING', 'Pending'),
    ('ATTENDING', 'Attending'),
    ('NOT_ATTENDING', 'Not Attending'),
    ('MAYBE', 'Maybe')
ON CONFLICT (StatusCode) DO NOTHING;

INSERT INTO PayFrequencies (FrequencyCode, FrequencyName) VALUES 
    ('WEEKLY', 'Weekly'),
    ('BIWEEKLY', 'Biweekly'),
    ('MONTHLY', 'Monthly'),
    ('QUARTERLY', 'Quarterly'),
    ('ANNUAL', 'Annual')
ON CONFLICT (FrequencyCode) DO NOTHING;

INSERT INTO YearLevels (LevelCode, LevelName) VALUES 
    ('FRESHMAN', 'Freshman'),
    ('SOPHOMORE', 'Sophomore'),
    ('JUNIOR', 'Junior'),
    ('SENIOR', 'Senior'),
    ('GRADUATE', 'Graduate')
ON CONFLICT (LevelCode) DO NOTHING;

-- ============================================================================
-- FINAL VERIFICATION: Ensure all required columns exist before creating indexes
-- This runs after all migrations to catch any missed columns
-- ============================================================================

DO $$
BEGIN
    -- Verify and create missing StatusTypeID columns for all tables that need them
    -- Students
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'students') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'students' AND column_name = 'statustypeid') THEN
            ALTER TABLE Students ADD COLUMN StatusTypeID INT;
            UPDATE Students SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'STUDENT' AND StatusCode = 'ACTIVE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE Students ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- Rooms (already has migration, but double-check)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'rooms') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'rooms' AND column_name = 'statustypeid') THEN
            ALTER TABLE Rooms ADD COLUMN StatusTypeID INT;
            UPDATE Rooms SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'ROOM' AND StatusCode = 'AVAILABLE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE Rooms ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'rooms' AND column_name = 'roomtypeid') THEN
            ALTER TABLE Rooms ADD COLUMN RoomTypeID INT;
            UPDATE Rooms SET RoomTypeID = (SELECT RoomTypeID FROM RoomTypes WHERE TypeCode = 'CLASSROOM' LIMIT 1) WHERE RoomTypeID IS NULL;
            ALTER TABLE Rooms ALTER COLUMN RoomTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- MaintenanceTickets
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'maintenancetickets') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'maintenancetickets' AND column_name = 'statustypeid') THEN
            ALTER TABLE MaintenanceTickets ADD COLUMN StatusTypeID INT;
            UPDATE MaintenanceTickets SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'TICKET' AND StatusCode = 'NEW' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE MaintenanceTickets ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- Bookings
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'bookings') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'bookings' AND column_name = 'statustypeid') THEN
            ALTER TABLE Bookings ADD COLUMN StatusTypeID INT;
            UPDATE Bookings SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'BOOKING' AND StatusCode = 'CONFIRMED' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE Bookings ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- Equipment
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'equipment') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'equipment' AND column_name = 'statustypeid') THEN
            ALTER TABLE Equipment ADD COLUMN StatusTypeID INT;
            UPDATE Equipment SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'EQUIPMENT' AND StatusCode = 'AVAILABLE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE Equipment ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- SoftwareLicenses
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'softwarelicenses') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'softwarelicenses' AND column_name = 'statustypeid') THEN
            ALTER TABLE SoftwareLicenses ADD COLUMN StatusTypeID INT;
            UPDATE SoftwareLicenses SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'LICENSE' AND StatusCode = 'ACTIVE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE SoftwareLicenses ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- AdmissionApplications
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'admissionapplications') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'admissionapplications' AND column_name = 'statustypeid') THEN
            ALTER TABLE AdmissionApplications ADD COLUMN StatusTypeID INT;
            UPDATE AdmissionApplications SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'APPLICATION' AND StatusCode = 'SUBMITTED' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE AdmissionApplications ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- TranscriptRequests
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'transcriptrequests') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'transcriptrequests' AND column_name = 'statustypeid') THEN
            ALTER TABLE TranscriptRequests ADD COLUMN StatusTypeID INT;
            UPDATE TranscriptRequests SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'TRANSCRIPT' AND StatusCode = 'PENDING' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE TranscriptRequests ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- Enrollments
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'enrollments') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'enrollments' AND column_name = 'statustypeid') THEN
            ALTER TABLE Enrollments ADD COLUMN StatusTypeID INT;
            UPDATE Enrollments SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'ENROLLMENT' AND StatusCode = 'ENROLLED' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE Enrollments ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- AssignmentSubmissions
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'assignmentsubmissions') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'assignmentsubmissions' AND column_name = 'statustypeid') THEN
            ALTER TABLE AssignmentSubmissions ADD COLUMN StatusTypeID INT;
            UPDATE AssignmentSubmissions SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'SUBMISSION' AND StatusCode = 'SUBMITTED' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE AssignmentSubmissions ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- QuizAttempts
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'quizattempts') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quizattempts' AND column_name = 'statustypeid') THEN
            ALTER TABLE QuizAttempts ADD COLUMN StatusTypeID INT;
            UPDATE QuizAttempts SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'QUIZ_ATTEMPT' AND StatusCode = 'IN_PROGRESS' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE QuizAttempts ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- LeaveRequests
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'leaverequests') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'leaverequests' AND column_name = 'statustypeid') THEN
            ALTER TABLE LeaveRequests ADD COLUMN StatusTypeID INT;
            UPDATE LeaveRequests SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'LEAVE' AND StatusCode = 'PENDING' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE LeaveRequests ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- BenefitsInformation
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'benefitsinformation') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'benefitsinformation' AND column_name = 'statustypeid') THEN
            ALTER TABLE BenefitsInformation ADD COLUMN StatusTypeID INT;
            UPDATE BenefitsInformation SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'BENEFIT' AND StatusCode = 'ACTIVE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE BenefitsInformation ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
    
    -- EquipmentUserAllocations
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'equipmentuserallocations') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'equipmentuserallocations' AND column_name = 'statustypeid') THEN
            ALTER TABLE EquipmentUserAllocations ADD COLUMN StatusTypeID INT;
            UPDATE EquipmentUserAllocations SET StatusTypeID = (SELECT StatusTypeID FROM StatusTypes WHERE EntityType = 'ALLOCATION' AND StatusCode = 'ACTIVE' LIMIT 1) WHERE StatusTypeID IS NULL;
            ALTER TABLE EquipmentUserAllocations ALTER COLUMN StatusTypeID SET NOT NULL;
        END IF;
    END IF;
END $$;

-- ============================================================================
-- END OF SCHEMA CREATION
-- ============================================================================
-- Note: All lookup tables are now populated with initial data
-- Migration logic for individual tables is included above where needed
-- Final verification ensures all StatusTypeID columns exist before indexes are used
-- ============================================================================

