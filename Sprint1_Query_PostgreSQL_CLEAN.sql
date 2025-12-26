-- ============================================================================
-- REFACTORED Database Schema - Following Database Excellence Rules
-- PostgreSQL Version - CLEAN INSTALL (Drops all tables first)
-- ============================================================================
-- This script:
-- 1. Drops all existing tables (CASCADE to handle foreign keys)
-- 2. Creates all lookup tables
-- 3. Populates lookup tables with initial data
-- 4. Creates all main tables with proper foreign keys
-- 5. Creates all indexes
--
-- WARNING: This will DELETE ALL DATA in the database!
-- Use only for fresh installations or when you want to start over.
-- ============================================================================

-- ============================================================================
-- STEP 1: DROP ALL EXISTING TABLES (in reverse dependency order)
-- ============================================================================

DROP TABLE IF EXISTS EventAttendees CASCADE;
DROP TABLE IF EXISTS EventReminders CASCADE;
DROP TABLE IF EXISTS Events CASCADE;
DROP TABLE IF EXISTS ForumComments CASCADE;
DROP TABLE IF EXISTS ForumPosts CASCADE;
DROP TABLE IF EXISTS MessageThreads CASCADE;
DROP TABLE IF EXISTS Meetings CASCADE;
DROP TABLE IF EXISTS StudentParentRelationship CASCADE;
DROP TABLE IF EXISTS Parents CASCADE;
DROP TABLE IF EXISTS BenefitsInformation CASCADE;
DROP TABLE IF EXISTS PayrollInformation CASCADE;
DROP TABLE IF EXISTS LeaveRequests CASCADE;
DROP TABLE IF EXISTS CourseStaff CASCADE;
DROP TABLE IF EXISTS Messages CASCADE;
DROP TABLE IF EXISTS StaffProfiles CASCADE;
DROP TABLE IF EXISTS CourseGradeWeights CASCADE;
DROP TABLE IF EXISTS ExamGrades CASCADE;
DROP TABLE IF EXISTS ExamAttributes CASCADE;
DROP TABLE IF EXISTS Exams CASCADE;
DROP TABLE IF EXISTS QuizAttempts CASCADE;
DROP TABLE IF EXISTS QuizQuestionOptions CASCADE;
DROP TABLE IF EXISTS QuizQuestions CASCADE;
DROP TABLE IF EXISTS QuizAttributes CASCADE;
DROP TABLE IF EXISTS Quizzes CASCADE;
DROP TABLE IF EXISTS CourseMaterials CASCADE;
DROP TABLE IF EXISTS AssignmentSubmissions CASCADE;
DROP TABLE IF EXISTS AssignmentAttributes CASCADE;
DROP TABLE IF EXISTS Assignments CASCADE;
DROP TABLE IF EXISTS Enrollments CASCADE;
DROP TABLE IF EXISTS CourseAttributes CASCADE;
DROP TABLE IF EXISTS Prerequisites CASCADE;
DROP TABLE IF EXISTS CourseProfessors CASCADE;
DROP TABLE IF EXISTS Courses CASCADE;
DROP TABLE IF EXISTS TranscriptRequests CASCADE;
DROP TABLE IF EXISTS AdmissionApplications CASCADE;
DROP TABLE IF EXISTS SoftwareLicenses CASCADE;
DROP TABLE IF EXISTS EquipmentDepartmentAllocations CASCADE;
DROP TABLE IF EXISTS EquipmentUserAllocations CASCADE;
DROP TABLE IF EXISTS Equipment CASCADE;
DROP TABLE IF EXISTS Bookings CASCADE;
DROP TABLE IF EXISTS MaintenanceTickets CASCADE;
DROP TABLE IF EXISTS RoomEquipment CASCADE;
DROP TABLE IF EXISTS Rooms CASCADE;
DROP TABLE IF EXISTS EquipmentType CASCADE;
DROP TABLE IF EXISTS UserRoles CASCADE;
DROP TABLE IF EXISTS Admins CASCADE;
DROP TABLE IF EXISTS Staff CASCADE;
DROP TABLE IF EXISTS Professors CASCADE;
DROP TABLE IF EXISTS Students CASCADE;
DROP TABLE IF EXISTS Users CASCADE;

-- Drop lookup tables
DROP TABLE IF EXISTS YearLevels CASCADE;
DROP TABLE IF EXISTS PayFrequencies CASCADE;
DROP TABLE IF EXISTS RSVPStatuses CASCADE;
DROP TABLE IF EXISTS ReminderTypes CASCADE;
DROP TABLE IF EXISTS PaymentMethods CASCADE;
DROP TABLE IF EXISTS CourseTypes CASCADE;
DROP TABLE IF EXISTS RoomTypes CASCADE;
DROP TABLE IF EXISTS Semesters CASCADE;
DROP TABLE IF EXISTS EventTypes CASCADE;
DROP TABLE IF EXISTS BenefitTypes CASCADE;
DROP TABLE IF EXISTS LeaveTypes CASCADE;
DROP TABLE IF EXISTS RelationshipTypes CASCADE;
DROP TABLE IF EXISTS UserTypes CASCADE;
DROP TABLE IF EXISTS StatusTypes CASCADE;
DROP TABLE IF EXISTS Departments CASCADE;

-- ============================================================================
-- STEP 2: CREATE LOOKUP TABLES
-- ============================================================================

-- Departments lookup table
CREATE TABLE Departments (
    DepartmentID SERIAL PRIMARY KEY,
    Code VARCHAR(20) NOT NULL UNIQUE,
    Name VARCHAR(100) NOT NULL,
    IsActive BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IX_Departments_Code ON Departments(Code);
CREATE INDEX IX_Departments_IsActive ON Departments(IsActive);

-- Status lookup table (for various entity statuses)
CREATE TABLE StatusTypes (
    StatusTypeID SERIAL PRIMARY KEY,
    EntityType VARCHAR(50) NOT NULL,
    StatusCode VARCHAR(50) NOT NULL,
    StatusName VARCHAR(100) NOT NULL,
    DisplayOrder INT NOT NULL DEFAULT 0,
    IsActive BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (EntityType, StatusCode)
);

CREATE INDEX IX_StatusTypes_EntityType ON StatusTypes(EntityType);
CREATE INDEX IX_StatusTypes_StatusCode ON StatusTypes(StatusCode);

-- UserTypes lookup (for RBAC - Rule 7)
CREATE TABLE UserTypes (
    UserTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(20) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL,
    IsActive BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IX_UserTypes_TypeCode ON UserTypes(TypeCode);

-- RelationshipTypes lookup
CREATE TABLE RelationshipTypes (
    RelationshipTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(50) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- LeaveTypes lookup
CREATE TABLE LeaveTypes (
    LeaveTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(50) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL,
    MaxDaysPerYear INT NULL,
    RequiresApproval BOOLEAN NOT NULL DEFAULT TRUE
);

-- BenefitTypes lookup
CREATE TABLE BenefitTypes (
    BenefitTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(50) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- EventTypes lookup
CREATE TABLE EventTypes (
    EventTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(50) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- Semester lookup
CREATE TABLE Semesters (
    SemesterID SERIAL PRIMARY KEY,
    Code VARCHAR(50) NOT NULL UNIQUE,
    Name VARCHAR(100) NOT NULL,
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    AcademicYear INT NOT NULL,
    IsActive BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IX_Semesters_Code ON Semesters(Code);
CREATE INDEX IX_Semesters_AcademicYear ON Semesters(AcademicYear);

-- YearLevels lookup
CREATE TABLE YearLevels (
    YearLevelID SERIAL PRIMARY KEY,
    LevelCode VARCHAR(20) NOT NULL UNIQUE,
    LevelName VARCHAR(100) NOT NULL,
    DisplayOrder INT NOT NULL DEFAULT 0
);

-- CourseTypes lookup
CREATE TABLE CourseTypes (
    CourseTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(20) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- RoomTypes lookup
CREATE TABLE RoomTypes (
    RoomTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(20) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- PaymentMethods lookup
CREATE TABLE PaymentMethods (
    PaymentMethodID SERIAL PRIMARY KEY,
    MethodCode VARCHAR(50) NOT NULL UNIQUE,
    MethodName VARCHAR(100) NOT NULL
);

-- ReminderTypes lookup
CREATE TABLE ReminderTypes (
    ReminderTypeID SERIAL PRIMARY KEY,
    TypeCode VARCHAR(20) NOT NULL UNIQUE,
    TypeName VARCHAR(100) NOT NULL
);

-- RSVPStatuses lookup
CREATE TABLE RSVPStatuses (
    RSVPStatusID SERIAL PRIMARY KEY,
    StatusCode VARCHAR(20) NOT NULL UNIQUE,
    StatusName VARCHAR(100) NOT NULL
);

-- PayFrequencies lookup
CREATE TABLE PayFrequencies (
    PayFrequencyID SERIAL PRIMARY KEY,
    FrequencyCode VARCHAR(20) NOT NULL UNIQUE,
    FrequencyName VARCHAR(100) NOT NULL
);

-- EquipmentType lookup table (needed before Equipment table)
CREATE TABLE EquipmentType (
    EquipmentTypeID SERIAL PRIMARY KEY,
    Name            VARCHAR(100) NOT NULL UNIQUE
);

-- ============================================================================
-- STEP 3: POPULATE LOOKUP TABLES WITH INITIAL DATA
-- ============================================================================

-- Populate StatusTypes
INSERT INTO StatusTypes (EntityType, StatusCode, StatusName) VALUES 
    ('STUDENT', 'ACTIVE', 'Active'),
    ('STUDENT', 'INACTIVE', 'Inactive'),
    ('STUDENT', 'GRADUATED', 'Graduated'),
    ('STUDENT', 'SUSPENDED', 'Suspended'),
    ('STUDENT', 'WITHDRAWN', 'Withdrawn'),
    ('ROOM', 'AVAILABLE', 'Available'),
    ('ROOM', 'OCCUPIED', 'Occupied'),
    ('ROOM', 'MAINTENANCE', 'Maintenance'),
    ('TICKET', 'NEW', 'New'),
    ('TICKET', 'IN_PROGRESS', 'In Progress'),
    ('TICKET', 'RESOLVED', 'Resolved'),
    ('BOOKING', 'CONFIRMED', 'Confirmed'),
    ('BOOKING', 'CANCELLED', 'Cancelled'),
    ('EQUIPMENT', 'AVAILABLE', 'Available'),
    ('EQUIPMENT', 'ALLOCATED', 'Allocated'),
    ('EQUIPMENT', 'MAINTENANCE', 'Maintenance'),
    ('EQUIPMENT', 'RETIRED', 'Retired'),
    ('LICENSE', 'ACTIVE', 'Active'),
    ('LICENSE', 'EXPIRED', 'Expired'),
    ('LICENSE', 'CANCELLED', 'Cancelled'),
    ('APPLICATION', 'SUBMITTED', 'Submitted'),
    ('APPLICATION', 'UNDER_REVIEW', 'Under Review'),
    ('APPLICATION', 'ACCEPTED', 'Accepted'),
    ('APPLICATION', 'REJECTED', 'Rejected'),
    ('TRANSCRIPT', 'PENDING', 'Pending'),
    ('TRANSCRIPT', 'IN_PROGRESS', 'In Progress'),
    ('TRANSCRIPT', 'READY_FOR_PICKUP', 'Ready for Pickup'),
    ('TRANSCRIPT', 'COMPLETED', 'Completed'),
    ('TRANSCRIPT', 'CANCELLED', 'Cancelled'),
    ('ENROLLMENT', 'ENROLLED', 'Enrolled'),
    ('ENROLLMENT', 'DROPPED', 'Dropped'),
    ('ENROLLMENT', 'COMPLETED', 'Completed'),
    ('ENROLLMENT', 'FAILED', 'Failed'),
    ('LEAVE', 'PENDING', 'Pending'),
    ('LEAVE', 'APPROVED', 'Approved'),
    ('LEAVE', 'REJECTED', 'Rejected'),
    ('LEAVE', 'CANCELLED', 'Cancelled'),
    ('BENEFIT', 'ACTIVE', 'Active'),
    ('BENEFIT', 'INACTIVE', 'Inactive'),
    ('BENEFIT', 'EXPIRED', 'Expired'),
    ('BENEFIT', 'CANCELLED', 'Cancelled'),
    ('SUBMISSION', 'SUBMITTED', 'Submitted'),
    ('SUBMISSION', 'GRADED', 'Graded'),
    ('QUIZ_ATTEMPT', 'IN_PROGRESS', 'In Progress'),
    ('QUIZ_ATTEMPT', 'COMPLETED', 'Completed'),
    ('QUIZ_ATTEMPT', 'TIMED_OUT', 'Timed Out'),
    ('ALLOCATION', 'ACTIVE', 'Active'),
    ('ALLOCATION', 'RETURNED', 'Returned'),
    ('MEETING', 'PENDING', 'Pending'),
    ('MEETING', 'APPROVED', 'Approved'),
    ('MEETING', 'REJECTED', 'Rejected'),
    ('MEETING', 'CANCELLED', 'Cancelled'),
    ('MEETING', 'COMPLETED', 'Completed');

-- Populate other lookup tables
INSERT INTO RoomTypes (TypeCode, TypeName) VALUES 
    ('CLASSROOM', 'Classroom'),
    ('LAB', 'Laboratory');

INSERT INTO UserTypes (TypeCode, TypeName) VALUES 
    ('STUDENT', 'Student'),
    ('PROFESSOR', 'Professor'),
    ('STAFF', 'Staff'),
    ('ADMIN', 'Administrator'),
    ('PARENT', 'Parent'),
    ('HR_ADMIN', 'HR Administrator'),
    ('GUEST', 'Guest');

INSERT INTO RelationshipTypes (TypeCode, TypeName) VALUES 
    ('PARENT', 'Parent'),
    ('GUARDIAN', 'Guardian'),
    ('EMERGENCY_CONTACT', 'Emergency Contact');

INSERT INTO LeaveTypes (TypeCode, TypeName) VALUES 
    ('SICK', 'Sick Leave'),
    ('VACATION', 'Vacation'),
    ('PERSONAL', 'Personal'),
    ('MATERNITY', 'Maternity'),
    ('PATERNITY', 'Paternity'),
    ('BEREAVEMENT', 'Bereavement'),
    ('OTHER', 'Other');

INSERT INTO BenefitTypes (TypeCode, TypeName) VALUES 
    ('HEALTH_INSURANCE', 'Health Insurance'),
    ('DENTAL_INSURANCE', 'Dental Insurance'),
    ('VISION_INSURANCE', 'Vision Insurance'),
    ('LIFE_INSURANCE', 'Life Insurance'),
    ('RETIREMENT', 'Retirement'),
    ('VACATION_DAYS', 'Vacation Days'),
    ('SICK_DAYS', 'Sick Days'),
    ('OTHER', 'Other');

INSERT INTO EventTypes (TypeCode, TypeName) VALUES 
    ('GENERAL', 'General'),
    ('ACADEMIC', 'Academic'),
    ('SOCIAL', 'Social'),
    ('SPORTS', 'Sports'),
    ('ADMINISTRATIVE', 'Administrative'),
    ('HOLIDAY', 'Holiday');

INSERT INTO CourseTypes (TypeCode, TypeName) VALUES 
    ('CORE', 'Core'),
    ('ELECTIVE', 'Elective');

INSERT INTO PaymentMethods (MethodCode, MethodName) VALUES 
    ('DIRECT_DEPOSIT', 'Direct Deposit'),
    ('CHECK', 'Check'),
    ('WIRE_TRANSFER', 'Wire Transfer');

INSERT INTO ReminderTypes (TypeCode, TypeName) VALUES 
    ('EMAIL', 'Email'),
    ('IN_APP', 'In-App'),
    ('BOTH', 'Both');

INSERT INTO RSVPStatuses (StatusCode, StatusName) VALUES 
    ('PENDING', 'Pending'),
    ('ATTENDING', 'Attending'),
    ('NOT_ATTENDING', 'Not Attending'),
    ('MAYBE', 'Maybe');

INSERT INTO PayFrequencies (FrequencyCode, FrequencyName) VALUES 
    ('WEEKLY', 'Weekly'),
    ('BIWEEKLY', 'Biweekly'),
    ('MONTHLY', 'Monthly'),
    ('QUARTERLY', 'Quarterly'),
    ('ANNUAL', 'Annual');

INSERT INTO YearLevels (LevelCode, LevelName) VALUES 
    ('FRESHMAN', 'Freshman'),
    ('SOPHOMORE', 'Sophomore'),
    ('JUNIOR', 'Junior'),
    ('SENIOR', 'Senior'),
    ('GRADUATE', 'Graduate');

-- Populate EquipmentType
INSERT INTO EquipmentType (Name) VALUES 
    ('Laptop'),
    ('Projector'),
    ('Microscope'),
    ('Printer'),
    ('Tablet'),
    ('Camera'),
    ('Whiteboard'),
    ('Desktop Computer'),
    ('Monitor'),
    ('Scanner'),
    ('3D Printer'),
    ('VR Headset'),
    ('Sound System'),
    ('Document Camera');

-- ============================================================================
-- STEP 4: CREATE MAIN TABLES
-- ============================================================================

-- Users table
CREATE TABLE Users (
    UserID   SERIAL PRIMARY KEY,
    USERNAME VARCHAR(50) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Email    VARCHAR(100) NOT NULL DEFAULT '',
    CreatedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LastLoginDate TIMESTAMP NULL,
    IsActive BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IX_Users_Username ON Users(USERNAME);
CREATE INDEX IX_Users_Email ON Users(Email);
CREATE INDEX IX_Users_IsActive ON Users(IsActive);

-- UserRoles junction table for RBAC (Rule 7: Multi-role support)
CREATE TABLE UserRoles (
    UserRoleID SERIAL PRIMARY KEY,
    UserID INT NOT NULL,
    UserTypeID INT NOT NULL,
    IsPrimary BOOLEAN NOT NULL DEFAULT FALSE,
    AssignedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (UserTypeID) REFERENCES UserTypes(UserTypeID),
    UNIQUE (UserID, UserTypeID)
);

CREATE INDEX IX_UserRoles_UserID ON UserRoles(UserID);
CREATE INDEX IX_UserRoles_UserTypeID ON UserRoles(UserTypeID);
CREATE INDEX IX_UserRoles_IsPrimary ON UserRoles(IsPrimary);

-- Students table
CREATE TABLE Students (
    UserID        INT PRIMARY KEY,
    StudentNumber VARCHAR(20) NOT NULL UNIQUE,
    DepartmentID  INT NULL,
    Major         VARCHAR(100) NOT NULL DEFAULT '',
    EnrollmentDate DATE NOT NULL DEFAULT CURRENT_DATE,
    GPA           DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    StatusTypeID  INT NOT NULL,
    AdmissionDate DATE NULL,
    YearLevelID   INT NULL,
    Notes         TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID),
    FOREIGN KEY (YearLevelID) REFERENCES YearLevels(YearLevelID)
);

CREATE INDEX IX_Students_StudentNumber ON Students(StudentNumber);
CREATE INDEX IX_Students_DepartmentID ON Students(DepartmentID);
CREATE INDEX IX_Students_StatusTypeID ON Students(StatusTypeID);
CREATE INDEX IX_Students_YearLevelID ON Students(YearLevelID);

-- Professors table
CREATE TABLE Professors (
    UserID    INT PRIMARY KEY,
    DepartmentID INT NULL,
    OfficeRoom VARCHAR(20) NOT NULL DEFAULT '',
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID)
);

CREATE INDEX IX_Professors_DepartmentID ON Professors(DepartmentID);

-- Staff table
CREATE TABLE Staff (
    UserID    INT PRIMARY KEY,
    DepartmentID INT NULL,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (DepartmentID) REFERENCES Departments(DepartmentID)
);

CREATE INDEX IX_Staff_DepartmentID ON Staff(DepartmentID);

-- Admins table
CREATE TABLE Admins (
    UserID    INT PRIMARY KEY,
    RoleTitle VARCHAR(100) NOT NULL DEFAULT '',
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE
);

-- Rooms table
CREATE TABLE Rooms (
    RoomID   SERIAL PRIMARY KEY,
    Code     VARCHAR(20) NOT NULL UNIQUE,
    Name     VARCHAR(100) NOT NULL,
    RoomTypeID INT NOT NULL,
    Capacity INT NOT NULL DEFAULT 0,
    Location VARCHAR(100) NOT NULL DEFAULT '',
    StatusTypeID INT NOT NULL,
    FOREIGN KEY (RoomTypeID) REFERENCES RoomTypes(RoomTypeID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID)
);

CREATE INDEX IX_Rooms_Code ON Rooms(Code);
CREATE INDEX IX_Rooms_RoomTypeID ON Rooms(RoomTypeID);
CREATE INDEX IX_Rooms_StatusTypeID ON Rooms(StatusTypeID);

-- RoomEquipment table
CREATE TABLE RoomEquipment (
    RoomID          INT NOT NULL,
    EquipmentTypeID INT NOT NULL,
    Quantity        INT NOT NULL DEFAULT 1,
    PRIMARY KEY (RoomID, EquipmentTypeID),
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID) ON DELETE CASCADE,
    FOREIGN KEY (EquipmentTypeID) REFERENCES EquipmentType(EquipmentTypeID) ON DELETE CASCADE
);

-- MaintenanceTickets table
CREATE TABLE MaintenanceTickets (
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

CREATE INDEX IX_MaintenanceTickets_AssignedToUserID ON MaintenanceTickets(AssignedToUserID) WHERE AssignedToUserID > 0;
CREATE INDEX IX_MaintenanceTickets_RoomID ON MaintenanceTickets(RoomID);
CREATE INDEX IX_MaintenanceTickets_ReporterUserID ON MaintenanceTickets(ReporterUserID);
CREATE INDEX IX_MaintenanceTickets_StatusTypeID ON MaintenanceTickets(StatusTypeID);

-- Bookings table
CREATE TABLE Bookings (
    BookingID     SERIAL PRIMARY KEY,
    RoomID         INT NOT NULL,
    UserID         INT NOT NULL,
    BookingDate    TIMESTAMP NOT NULL,
    EndDate        TIMESTAMP NOT NULL,
    Purpose        VARCHAR(200) NOT NULL DEFAULT '',
    StatusTypeID   INT NOT NULL,
    CreatedDate    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (UserID) REFERENCES Users(UserID),
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID),
    CHECK (EndDate > BookingDate)
);

CREATE INDEX IX_Bookings_RoomID ON Bookings(RoomID);
CREATE INDEX IX_Bookings_UserID ON Bookings(UserID);
CREATE INDEX IX_Bookings_BookingDate ON Bookings(BookingDate);
CREATE INDEX IX_Bookings_StatusTypeID ON Bookings(StatusTypeID);

-- Equipment table
CREATE TABLE Equipment (
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

CREATE INDEX IX_Equipment_EquipmentTypeID ON Equipment(EquipmentTypeID);
CREATE INDEX IX_Equipment_StatusTypeID ON Equipment(StatusTypeID);
CREATE INDEX IX_Equipment_SerialNumber ON Equipment(SerialNumber) WHERE SerialNumber != '';

-- EquipmentUserAllocations table
CREATE TABLE EquipmentUserAllocations (
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

CREATE INDEX IX_EquipmentUserAllocations_EquipmentID ON EquipmentUserAllocations(EquipmentID);
CREATE INDEX IX_EquipmentUserAllocations_AllocatedToUserID ON EquipmentUserAllocations(AllocatedToUserID);
CREATE INDEX IX_EquipmentUserAllocations_StatusTypeID ON EquipmentUserAllocations(StatusTypeID);

-- EquipmentDepartmentAllocations table
CREATE TABLE EquipmentDepartmentAllocations (
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

CREATE INDEX IX_EquipmentDepartmentAllocations_EquipmentID ON EquipmentDepartmentAllocations(EquipmentID);
CREATE INDEX IX_EquipmentDepartmentAllocations_DepartmentID ON EquipmentDepartmentAllocations(DepartmentID);

-- SoftwareLicenses table
CREATE TABLE SoftwareLicenses (
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

CREATE INDEX IX_SoftwareLicenses_ExpiryDate ON SoftwareLicenses(ExpiryDate);
CREATE INDEX IX_SoftwareLicenses_StatusTypeID ON SoftwareLicenses(StatusTypeID);

-- AdmissionApplications table
CREATE TABLE AdmissionApplications (
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

CREATE INDEX IX_AdmissionApplications_StatusTypeID ON AdmissionApplications(StatusTypeID);
CREATE INDEX IX_AdmissionApplications_SubmittedDate ON AdmissionApplications(SubmittedDate);
CREATE INDEX IX_AdmissionApplications_Email ON AdmissionApplications(Email);

-- TranscriptRequests table
CREATE TABLE TranscriptRequests (
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

CREATE INDEX IX_TranscriptRequests_StudentUserID ON TranscriptRequests(StudentUserID);
CREATE INDEX IX_TranscriptRequests_StatusTypeID ON TranscriptRequests(StatusTypeID);
CREATE INDEX IX_TranscriptRequests_RequestDate ON TranscriptRequests(RequestDate);

-- Courses table
CREATE TABLE Courses (
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

CREATE INDEX IX_Courses_Code ON Courses(Code);
CREATE INDEX IX_Courses_DepartmentID ON Courses(DepartmentID);
CREATE INDEX IX_Courses_SemesterID ON Courses(SemesterID);
CREATE INDEX IX_Courses_CourseTypeID ON Courses(CourseTypeID);
CREATE INDEX IX_Courses_IsActive ON Courses(IsActive);

-- CourseProfessors table
CREATE TABLE CourseProfessors (
    CourseProfessorID SERIAL PRIMARY KEY,
    CourseID          INT NOT NULL,
    ProfessorUserID   INT NOT NULL,
    CreatedDate       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (ProfessorUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    UNIQUE (CourseID, ProfessorUserID)
);

CREATE INDEX IX_CourseProfessors_CourseID ON CourseProfessors(CourseID);
CREATE INDEX IX_CourseProfessors_ProfessorUserID ON CourseProfessors(ProfessorUserID);

-- Prerequisites table
CREATE TABLE Prerequisites (
    PrerequisiteID SERIAL PRIMARY KEY,
    CourseID       INT NOT NULL,
    PrerequisiteCourseID INT NOT NULL,
    CreatedDate    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (PrerequisiteCourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    UNIQUE (CourseID, PrerequisiteCourseID),
    CHECK (CourseID != PrerequisiteCourseID)
);

CREATE INDEX IX_Prerequisites_CourseID ON Prerequisites(CourseID);
CREATE INDEX IX_Prerequisites_PrerequisiteCourseID ON Prerequisites(PrerequisiteCourseID);

-- CourseAttributes table (EAV pattern)
CREATE TABLE CourseAttributes (
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

CREATE INDEX IX_CourseAttributes_CourseID ON CourseAttributes(CourseID);
CREATE INDEX IX_CourseAttributes_AttributeName ON CourseAttributes(AttributeName);

-- Enrollments table
CREATE TABLE Enrollments (
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

-- Note: Unique constraint for active enrollments is handled at application level
-- PostgreSQL/Supabase doesn't support subqueries in partial index predicates
-- Application should check for existing ENROLLED status before creating new enrollment

CREATE INDEX IX_Enrollments_StudentUserID ON Enrollments(StudentUserID);
CREATE INDEX IX_Enrollments_CourseID ON Enrollments(CourseID);
CREATE INDEX IX_Enrollments_StatusTypeID ON Enrollments(StatusTypeID);
CREATE INDEX IX_Enrollments_Student_Course_Status ON Enrollments(StudentUserID, CourseID, StatusTypeID);

-- Assignments table
CREATE TABLE Assignments (
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

CREATE INDEX IX_Assignments_CourseID ON Assignments(CourseID);
CREATE INDEX IX_Assignments_DueDate ON Assignments(DueDate);

-- AssignmentAttributes table
CREATE TABLE AssignmentAttributes (
    AttributeID SERIAL PRIMARY KEY,
    AssignmentID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (AssignmentID) REFERENCES Assignments(AssignmentID)
);

CREATE INDEX IX_AssignmentAttributes_AssignmentID ON AssignmentAttributes(AssignmentID);

-- AssignmentSubmissions table
CREATE TABLE AssignmentSubmissions (
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

CREATE INDEX IX_AssignmentSubmissions_AssignmentID ON AssignmentSubmissions(AssignmentID);
CREATE INDEX IX_AssignmentSubmissions_StudentUserID ON AssignmentSubmissions(StudentUserID);
CREATE INDEX IX_AssignmentSubmissions_StatusTypeID ON AssignmentSubmissions(StatusTypeID);

-- CourseMaterials table
CREATE TABLE CourseMaterials (
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

CREATE INDEX IX_CourseMaterials_CourseID ON CourseMaterials(CourseID);
CREATE INDEX IX_CourseMaterials_UploadDate ON CourseMaterials(UploadDate DESC);
CREATE INDEX IX_CourseMaterials_UploadedBy ON CourseMaterials(UploadedByUserID);

-- Quizzes table
CREATE TABLE Quizzes (
    QuizID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    Instructions TEXT NOT NULL DEFAULT '',
    TotalPoints INT NOT NULL,
    DueDate TIMESTAMP NOT NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE
);

CREATE INDEX IX_Quizzes_CourseID ON Quizzes(CourseID);
CREATE INDEX IX_Quizzes_DueDate ON Quizzes(DueDate);

-- QuizAttributes table
CREATE TABLE QuizAttributes (
    AttributeID SERIAL PRIMARY KEY,
    QuizID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (QuizID) REFERENCES Quizzes(QuizID) ON DELETE CASCADE
);

CREATE INDEX IX_QuizAttributes_QuizID ON QuizAttributes(QuizID);

-- QuizQuestions table
CREATE TABLE QuizQuestions (
    QuestionID SERIAL PRIMARY KEY,
    QuizID INT NOT NULL,
    QuestionNumber INT NOT NULL,
    QuestionText TEXT NOT NULL,
    QuestionType VARCHAR(20) NOT NULL CHECK (QuestionType IN ('MCQ', 'WRITTEN')),
    Points INT NOT NULL DEFAULT 1,
    FOREIGN KEY (QuizID) REFERENCES Quizzes(QuizID) ON DELETE CASCADE,
    UNIQUE(QuizID, QuestionNumber)
);

CREATE INDEX IX_QuizQuestions_QuizID ON QuizQuestions(QuizID);

-- QuizQuestionOptions table
CREATE TABLE QuizQuestionOptions (
    OptionID SERIAL PRIMARY KEY,
    QuestionID INT NOT NULL,
    OptionText TEXT NOT NULL,
    IsCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    OptionOrder INT NOT NULL,
    FOREIGN KEY (QuestionID) REFERENCES QuizQuestions(QuestionID) ON DELETE CASCADE
);

CREATE INDEX IX_QuizQuestionOptions_QuestionID ON QuizQuestionOptions(QuestionID);

-- QuizAttempts table
CREATE TABLE QuizAttempts (
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

CREATE INDEX IX_QuizAttempts_QuizID ON QuizAttempts(QuizID);
CREATE INDEX IX_QuizAttempts_StudentUserID ON QuizAttempts(StudentUserID);
CREATE INDEX IX_QuizAttempts_StatusTypeID ON QuizAttempts(StatusTypeID);

-- Exams table
CREATE TABLE Exams (
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

CREATE INDEX IX_Exams_CourseID ON Exams(CourseID);
CREATE INDEX IX_Exams_ExamDate ON Exams(ExamDate);

-- ExamAttributes table
CREATE TABLE ExamAttributes (
    AttributeID SERIAL PRIMARY KEY,
    ExamID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (ExamID) REFERENCES Exams(ExamID) ON DELETE CASCADE
);

CREATE INDEX IX_ExamAttributes_ExamID ON ExamAttributes(ExamID);

-- ExamGrades table
CREATE TABLE ExamGrades (
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

CREATE INDEX IX_ExamGrades_ExamID ON ExamGrades(ExamID);
CREATE INDEX IX_ExamGrades_StudentUserID ON ExamGrades(StudentUserID);

-- CourseGradeWeights table
CREATE TABLE CourseGradeWeights (
    CourseID INT PRIMARY KEY,
    AssignmentsWeight DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    QuizzesWeight DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    ExamsWeight DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    CONSTRAINT CHK_WeightsSum CHECK (AssignmentsWeight + QuizzesWeight + ExamsWeight = 100.00)
);

CREATE INDEX IX_CourseGradeWeights_CourseID ON CourseGradeWeights(CourseID);

-- StaffProfiles table
CREATE TABLE StaffProfiles (
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

CREATE INDEX idx_staff_department ON StaffProfiles(DepartmentID);
CREATE INDEX idx_staff_name ON StaffProfiles(Name);
CREATE INDEX idx_staff_active ON StaffProfiles(IsActive);

-- MessageThreads table (must be created before Messages)
CREATE TABLE MessageThreads (
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

CREATE INDEX IX_MessageThreads_Parent ON MessageThreads(ParentUserID);
CREATE INDEX IX_MessageThreads_Teacher ON MessageThreads(TeacherUserID);
CREATE INDEX IX_MessageThreads_Student ON MessageThreads(StudentUserID);

-- Messages table
CREATE TABLE Messages (
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

CREATE INDEX IX_Messages_Receiver ON Messages(ReceiverUserID);
CREATE INDEX IX_Messages_Sender ON Messages(SenderUserID);
CREATE INDEX IX_Messages_ThreadID ON Messages(ThreadID);
CREATE INDEX IX_Messages_MessageType ON Messages(MessageType);

-- CourseStaff table (for TA and other staff assignments to courses)
CREATE TABLE CourseStaff (
    CourseStaffID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    StaffUserID INT NOT NULL,
    Role VARCHAR(50) NOT NULL DEFAULT 'TA' CHECK (Role IN ('TA', 'LAB_ASSISTANT', 'TUTOR', 'COORDINATOR', 'OTHER')),
    AssignmentDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (StaffUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    UNIQUE (CourseID, StaffUserID)
);

CREATE INDEX IX_CourseStaff_CourseID ON CourseStaff(CourseID);
CREATE INDEX IX_CourseStaff_StaffUserID ON CourseStaff(StaffUserID);

-- LeaveRequests table
CREATE TABLE LeaveRequests (
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

CREATE INDEX IX_LeaveRequests_StaffUserID ON LeaveRequests(StaffUserID);
CREATE INDEX IX_LeaveRequests_StatusTypeID ON LeaveRequests(StatusTypeID);
CREATE INDEX IX_LeaveRequests_StartDate ON LeaveRequests(StartDate);
CREATE INDEX IX_LeaveRequests_ReviewedByUserID ON LeaveRequests(ReviewedByUserID);
CREATE INDEX IX_LeaveRequests_LeaveTypeID ON LeaveRequests(LeaveTypeID);

-- PayrollInformation table
CREATE TABLE PayrollInformation (
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

CREATE INDEX IX_PayrollInformation_StaffUserID ON PayrollInformation(StaffUserID);
CREATE INDEX IX_PayrollInformation_PayDate ON PayrollInformation(PayDate DESC);
CREATE INDEX IX_PayrollInformation_PayPeriodStart ON PayrollInformation(PayPeriodStart);

-- BenefitsInformation table
CREATE TABLE BenefitsInformation (
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

CREATE INDEX IX_BenefitsInformation_StaffUserID ON BenefitsInformation(StaffUserID);
CREATE INDEX IX_BenefitsInformation_BenefitTypeID ON BenefitsInformation(BenefitTypeID);
CREATE INDEX IX_BenefitsInformation_StatusTypeID ON BenefitsInformation(StatusTypeID);

-- Parents table
CREATE TABLE Parents (
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

CREATE INDEX IX_Parents_UserID ON Parents(UserID);

-- StudentParentRelationship table
CREATE TABLE StudentParentRelationship (
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

CREATE INDEX IX_StudentParentRelationship_Student ON StudentParentRelationship(StudentUserID);
CREATE INDEX IX_StudentParentRelationship_Parent ON StudentParentRelationship(ParentUserID);
CREATE INDEX IX_StudentParentRelationship_RelationshipTypeID ON StudentParentRelationship(RelationshipTypeID);

-- ForumPosts table
CREATE TABLE ForumPosts (
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

CREATE INDEX IX_ForumPosts_Author ON ForumPosts(AuthorUserID);
CREATE INDEX IX_ForumPosts_Course ON ForumPosts(CourseID);
CREATE INDEX IX_ForumPosts_CreatedDate ON ForumPosts(CreatedDate DESC);
CREATE INDEX IX_ForumPosts_Topic ON ForumPosts(Topic);

-- ForumComments table
CREATE TABLE ForumComments (
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

CREATE INDEX IX_ForumComments_Post ON ForumComments(PostID);
CREATE INDEX IX_ForumComments_Author ON ForumComments(AuthorUserID);
CREATE INDEX IX_ForumComments_CreatedDate ON ForumComments(CreatedDate);

-- Events table
CREATE TABLE Events (
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

CREATE INDEX IX_Events_EventDate ON Events(EventDate);
CREATE INDEX IX_Events_CreatedBy ON Events(CreatedByUserID);
CREATE INDEX IX_Events_EventTypeID ON Events(EventTypeID);
CREATE INDEX IX_Events_IsPublic ON Events(IsPublic);

-- EventReminders table
CREATE TABLE EventReminders (
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

CREATE INDEX IX_EventReminders_Event ON EventReminders(EventID);
CREATE INDEX IX_EventReminders_User ON EventReminders(UserID);
CREATE INDEX IX_EventReminders_ReminderTime ON EventReminders(ReminderTime);
CREATE INDEX IX_EventReminders_IsSent ON EventReminders(IsSent);

-- EventAttendees table
CREATE TABLE EventAttendees (
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

CREATE INDEX IX_EventAttendees_Event ON EventAttendees(EventID);
CREATE INDEX IX_EventAttendees_User ON EventAttendees(UserID);
CREATE INDEX IX_EventAttendees_RSVPStatusID ON EventAttendees(RSVPStatusID);

-- Meetings table (for scheduling meetings between students and staff/professors)
CREATE TABLE Meetings (
    MeetingID SERIAL PRIMARY KEY,
    StudentUserID INT NOT NULL,
    StaffUserID INT NOT NULL,
    Subject VARCHAR(200) NOT NULL,
    Description TEXT NOT NULL DEFAULT '',
    MeetingDate DATE NOT NULL,
    StartTime TIME NOT NULL,
    EndTime TIME NOT NULL,
    Location VARCHAR(200) NOT NULL DEFAULT '',
    StatusTypeID INT NOT NULL,
    RequestedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ResponseDate TIMESTAMP NULL,
    ResponseNotes TEXT NOT NULL DEFAULT '',
    CreatedDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (StaffUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (StatusTypeID) REFERENCES StatusTypes(StatusTypeID),
    CHECK (EndTime > StartTime)
);

CREATE INDEX IX_Meetings_StudentUserID ON Meetings(StudentUserID);
CREATE INDEX IX_Meetings_StaffUserID ON Meetings(StaffUserID);
CREATE INDEX IX_Meetings_StatusTypeID ON Meetings(StatusTypeID);
CREATE INDEX IX_Meetings_MeetingDate ON Meetings(MeetingDate);
CREATE INDEX IX_Meetings_RequestedDate ON Meetings(RequestedDate DESC);

-- ============================================================================
-- END OF SCHEMA CREATION
-- ============================================================================
-- All tables created following database excellence rules:
-- ✓ Rule 1: NULL logic reduced (defaults, empty strings, 0 for unassigned)
-- ✓ Rule 2: All joins use INT PKs/FKs
-- ✓ Rule 3: UNIQUE constraints prevent duplicates
-- ✓ Rule 4: Normalized repeated strings (lookup tables)
-- ✓ Rule 5: Junction tables for many-to-many
-- ✓ Rule 6: EAV pattern properly implemented
-- ✓ Rule 7: RBAC with UserRoles junction table
-- ✓ Rule 8: No multi-lingual columns (not needed)
-- ✓ Rule 9: Proper indexes, sargable filters
-- ✓ Rule 10: Schema is reusable and parameterizable
-- ============================================================================

