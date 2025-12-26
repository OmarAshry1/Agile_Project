-- ============================================================================
-- Complete Database Schema for Agile Facilities Management System
-- PostgreSQL Version (Converted from SQL Server)
-- ============================================================================
-- This script creates all tables, constraints, and indexes needed for the system.
-- Run this script on a fresh PostgreSQL database to set up the complete schema.
-- 
-- PostgreSQL Compatibility: PostgreSQL 12 or later
-- Tested with: PostgreSQL 12, 13, 14, 15, 16
-- 
-- Features included:
--   - User management (Students, Professors, Staff, Admins)
--   - Room management with equipment tracking
--   - Maintenance ticket system with staff assignment
--   - Admission application management (US 2.5)
--   - Student records management (US 2.1)
--   - Transcript request management (US 2.2, 2.3, 2.4)
--   - All foreign key constraints and indexes for optimal performance
-- 
-- Note: Converted from SQL Server syntax to PostgreSQL
-- ============================================================================

-- Users table
CREATE TABLE Users (
    UserID   SERIAL PRIMARY KEY,
    USERNAME VARCHAR(50) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Email    VARCHAR(100) NULL,              -- Email address (optional)
    UserType VARCHAR(20) NOT NULL
        CHECK (UserType IN ('STUDENT', 'PROFESSOR', 'STAFF', 'ADMIN'))
);

-- Create index on Username for faster login lookups
CREATE INDEX IX_Users_Username 
ON Users(USERNAME);

-- Create index on UserType for filtering users by role
CREATE INDEX IX_Users_UserType 
ON Users(UserType);

-- Students table
CREATE TABLE Students (
    UserID        INT PRIMARY KEY,
    StudentNumber VARCHAR(20),
    Major         VARCHAR(100),
    Department    VARCHAR(100),
    EnrollmentDate DATE NULL,                      -- Enrollment date
    GPA           DECIMAL(3,2) NULL,              -- Grade Point Average
    Status        VARCHAR(20) DEFAULT 'ACTIVE'     -- ACTIVE, INACTIVE, GRADUATED, SUSPENDED, WITHDRAWN
                CHECK (Status IN ('ACTIVE', 'INACTIVE', 'GRADUATED', 'SUSPENDED', 'WITHDRAWN')),
    AdmissionDate DATE NULL,                       -- Admission date
    YearLevel     VARCHAR(20) NULL                 -- FRESHMAN, SOPHOMORE, JUNIOR, SENIOR, GRADUATE
                CHECK (YearLevel IN ('FRESHMAN', 'SOPHOMORE', 'JUNIOR', 'SENIOR', 'GRADUATE')),
    Notes         TEXT NULL,                -- Admin notes
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

-- Create index on Status for filtering students by status
CREATE INDEX IX_Students_Status 
ON Students(Status);

-- Create index on StudentNumber for lookups
CREATE INDEX IX_Students_StudentNumber 
ON Students(StudentNumber);

-- Professors table
CREATE TABLE Professors (
    UserID    INT PRIMARY KEY,
    Department VARCHAR(100),
    OfficeRoom VARCHAR(20),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

-- Staff table
CREATE TABLE Staff (
    UserID    INT PRIMARY KEY,
    Department VARCHAR(100),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

-- Admins table
CREATE TABLE Admins (
    UserID    INT PRIMARY KEY,
    RoleTitle VARCHAR(100),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

-- Rooms table
CREATE TABLE Rooms (
    RoomID   SERIAL PRIMARY KEY,
    Code     VARCHAR(20) NOT NULL UNIQUE,      -- e.g. 'R101', 'LAB1' (unique room identifier)
    Name     VARCHAR(100) NOT NULL,
    Type     VARCHAR(20) NOT NULL
               CHECK (Type IN ('CLASSROOM', 'LAB')),
    Capacity INT,
    Location VARCHAR(100),
    Status   VARCHAR(20) DEFAULT 'AVAILABLE'
               CHECK (Status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE'))
);

-- Create index on Code for faster room lookups
CREATE INDEX IX_Rooms_Code 
ON Rooms(Code);

-- Create index on Status for filtering rooms by availability
CREATE INDEX IX_Rooms_Status 
ON Rooms(Status);

-- EquipmentType table
CREATE TABLE EquipmentType (
    EquipmentTypeID SERIAL PRIMARY KEY,
    Name            VARCHAR(100) NOT NULL
);

-- RoomEquipment table
CREATE TABLE RoomEquipment (
    RoomID          INT NOT NULL,
    EquipmentTypeID INT NOT NULL,
    Quantity        INT NOT NULL DEFAULT 1,
    PRIMARY KEY (RoomID, EquipmentTypeID),
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (EquipmentTypeID) REFERENCES EquipmentType(EquipmentTypeID)
);

-- MaintenanceTickets table
CREATE TABLE MaintenanceTickets (
    TicketID            SERIAL PRIMARY KEY,
    RoomID              INT NOT NULL,
    ReporterUserID      INT NOT NULL,
    AssignedToUserID    INT NULL,              -- Assigned staff member (NULL if unassigned)
    Description         TEXT NOT NULL, -- Changed from TEXT (deprecated) to TEXT
    Status              VARCHAR(20) DEFAULT 'NEW'
                        CHECK (Status IN ('NEW', 'IN_PROGRESS', 'RESOLVED')),
    CreatedDate         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- TIMESTAMP for better precision and range
    ResolvedDate        TIMESTAMP NULL,
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (ReporterUserID) REFERENCES Users(UserID),
    FOREIGN KEY (AssignedToUserID) REFERENCES Users(UserID)
);

-- Create index on AssignedToUserID for better query performance when filtering by assigned staff
CREATE INDEX IX_MaintenanceTickets_AssignedToUserID 
ON MaintenanceTickets(AssignedToUserID);

-- Create index on RoomID for better query performance
CREATE INDEX IX_MaintenanceTickets_RoomID 
ON MaintenanceTickets(RoomID);

-- Create index on ReporterUserID for better query performance
CREATE INDEX IX_MaintenanceTickets_ReporterUserID 
ON MaintenanceTickets(ReporterUserID);

-- Create index on Status for filtering tickets by status
CREATE INDEX IX_MaintenanceTickets_Status 
ON MaintenanceTickets(Status);

-- Bookings table
CREATE TABLE Bookings (
    BookingID     SERIAL PRIMARY KEY,
    RoomID         INT NOT NULL,
    UserID         INT NOT NULL,
    BookingDate    TIMESTAMP NOT NULL,              -- Start date/time of booking
    EndDate        TIMESTAMP NOT NULL,              -- End date/time of booking
    Purpose        VARCHAR(200) NULL,               -- Purpose/description of booking
    Status         VARCHAR(20) DEFAULT 'CONFIRMED'  -- CONFIRMED, CANCELLED
                CHECK (Status IN ('CONFIRMED', 'CANCELLED')),
    CreatedDate    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

-- Create index on RoomID for faster room booking lookups
CREATE INDEX IX_Bookings_RoomID 
ON Bookings(RoomID);

-- Create index on UserID for faster user booking lookups
CREATE INDEX IX_Bookings_UserID 
ON Bookings(UserID);

-- Create index on BookingDate for faster date range queries
CREATE INDEX IX_Bookings_BookingDate 
ON Bookings(BookingDate);

-- Create index on Status for filtering bookings by status
CREATE INDEX IX_Bookings_Status 
ON Bookings(Status);

-- Equipment table (standalone equipment items)
CREATE TABLE Equipment (
    EquipmentID      SERIAL PRIMARY KEY,
    EquipmentTypeID INT NOT NULL,
    SerialNumber    VARCHAR(100) NULL,              -- Optional serial number
    Status          VARCHAR(20) DEFAULT 'AVAILABLE' -- AVAILABLE, ALLOCATED, MAINTENANCE, RETIRED
                CHECK (Status IN ('AVAILABLE', 'ALLOCATED', 'MAINTENANCE', 'RETIRED')),
    Location        VARCHAR(200) NULL,              -- Current location
    Notes           TEXT NULL,              -- Additional notes
    CreatedDate     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (EquipmentTypeID) REFERENCES EquipmentType(EquipmentTypeID)
);

-- Create index on EquipmentTypeID for faster lookups
CREATE INDEX IX_Equipment_EquipmentTypeID 
ON Equipment(EquipmentTypeID);

-- Create index on Status for filtering equipment by availability
CREATE INDEX IX_Equipment_Status 
ON Equipment(Status);

-- EquipmentAllocation table (tracks equipment allocated to staff/departments)
CREATE TABLE EquipmentAllocation (
    AllocationID    SERIAL PRIMARY KEY,
    EquipmentID     INT NOT NULL,
    AllocatedToUserID INT NULL,                    -- NULL if allocated to department
    Department      VARCHAR(100) NULL,             -- NULL if allocated to specific user
    AllocatedByUserID INT NOT NULL,                -- Admin who allocated it
    AllocationDate  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ReturnDate      TIMESTAMP NULL,                -- NULL if still allocated
    Notes           TEXT NULL,
    Status          VARCHAR(20) DEFAULT 'ACTIVE'   -- ACTIVE, RETURNED
                CHECK (Status IN ('ACTIVE', 'RETURNED')),
    FOREIGN KEY (EquipmentID) REFERENCES Equipment(EquipmentID),
    FOREIGN KEY (AllocatedToUserID) REFERENCES Users(UserID),
    FOREIGN KEY (AllocatedByUserID) REFERENCES Users(UserID)
);

-- Create index on EquipmentID for faster lookups
CREATE INDEX IX_EquipmentAllocation_EquipmentID 
ON EquipmentAllocation(EquipmentID);

-- Create index on AllocatedToUserID for faster user lookups
CREATE INDEX IX_EquipmentAllocation_AllocatedToUserID 
ON EquipmentAllocation(AllocatedToUserID);

-- Create index on Department for faster department lookups
CREATE INDEX IX_EquipmentAllocation_Department 
ON EquipmentAllocation(Department);

-- Create index on Status for filtering active allocations
CREATE INDEX IX_EquipmentAllocation_Status 
ON EquipmentAllocation(Status);

-- SoftwareLicenses table
CREATE TABLE SoftwareLicenses (
    LicenseID       SERIAL PRIMARY KEY,
    SoftwareName   VARCHAR(200) NOT NULL,
    LicenseKey      VARCHAR(500) NULL,             -- Optional license key
    Vendor          VARCHAR(200) NULL,
    PurchaseDate    TIMESTAMP NULL,
    ExpiryDate      TIMESTAMP NULL,               -- NULL for perpetual licenses
    Cost            DECIMAL(10,2) NULL,            -- Purchase/renewal cost
    Quantity        INT NOT NULL DEFAULT 1,        -- Number of licenses
    UsedQuantity    INT NOT NULL DEFAULT 0,        -- Number of licenses in use
    Status          VARCHAR(20) DEFAULT 'ACTIVE'   -- ACTIVE, EXPIRED, CANCELLED
                CHECK (Status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    Notes           TEXT NULL,
    CreatedDate     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on ExpiryDate for finding near-expiring licenses
CREATE INDEX IX_SoftwareLicenses_ExpiryDate 
ON SoftwareLicenses(ExpiryDate);

-- Create index on Status for filtering active licenses
CREATE INDEX IX_SoftwareLicenses_Status 
ON SoftwareLicenses(Status);

-- ============================================================================
-- Admission Applications Table (US 2.5 - Admission Application Management)
-- ============================================================================
CREATE TABLE AdmissionApplications (
    ApplicationID    SERIAL PRIMARY KEY,
    FirstName        VARCHAR(100) NOT NULL,
    LastName         VARCHAR(100) NOT NULL,
    Email            VARCHAR(100) NOT NULL,
    PhoneNumber      VARCHAR(20) NULL,
    DateOfBirth      DATE NULL,
    Address          TEXT NULL,
    City             VARCHAR(100) NULL,
    State            VARCHAR(50) NULL,
    ZipCode          VARCHAR(20) NULL,
    Country          VARCHAR(100) NULL,
    Program          VARCHAR(100) NULL,              -- Program/Major applying for
    PreviousEducation TEXT NULL,             -- Previous education details
    Documents        TEXT NULL,              -- Document references/notes
    Status           VARCHAR(20) DEFAULT 'SUBMITTED'
                        CHECK (Status IN ('SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED')),
    SubmittedDate    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ReviewedDate      TIMESTAMP NULL,
    ReviewedByUserID INT NULL,                       -- Admin who reviewed
    Notes            TEXT NULL,               -- Admin notes
    FOREIGN KEY (ReviewedByUserID) REFERENCES Users(UserID)
);

-- Create index on Status for faster filtering
CREATE INDEX IX_AdmissionApplications_Status 
ON AdmissionApplications(Status);

-- Create index on SubmittedDate for sorting
CREATE INDEX IX_AdmissionApplications_SubmittedDate 
ON AdmissionApplications(SubmittedDate);

-- Create index on Email for lookups
CREATE INDEX IX_AdmissionApplications_Email 
ON AdmissionApplications(Email);

-- ============================================================================
-- Transcript Requests Table (US 2.2, 2.3, 2.4 - Transcript Management)
-- ============================================================================
CREATE TABLE TranscriptRequests (
    RequestID       SERIAL PRIMARY KEY,
    StudentUserID   INT NOT NULL,                    -- Student requesting transcript
    RequestDate     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Status          VARCHAR(20) DEFAULT 'PENDING'    -- PENDING, IN_PROGRESS, READY_FOR_PICKUP, COMPLETED, CANCELLED
                        CHECK (Status IN ('PENDING', 'IN_PROGRESS', 'READY_FOR_PICKUP', 'COMPLETED', 'CANCELLED')),
    RequestedByUserID INT NOT NULL,                  -- Usually same as StudentUserID, but tracks who made the request
    ProcessedByUserID INT NULL,                      -- Admin who processed the request
    ProcessedDate   TIMESTAMP NULL,                  -- When admin started processing
    CompletedDate   TIMESTAMP NULL,                  -- When transcript was generated/completed
    PickupDate      TIMESTAMP NULL,                  -- When student picked up transcript
    Purpose         VARCHAR(500) NULL,                -- Purpose of transcript request
    Notes           TEXT NULL,                -- Admin notes
    PDFPath         VARCHAR(500) NULL,                -- Path to generated PDF file
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    FOREIGN KEY (RequestedByUserID) REFERENCES Users(UserID),
    FOREIGN KEY (ProcessedByUserID) REFERENCES Users(UserID)
);

-- Create index on StudentUserID for faster student lookups
CREATE INDEX IX_TranscriptRequests_StudentUserID 
ON TranscriptRequests(StudentUserID);

-- Create index on Status for faster filtering
CREATE INDEX IX_TranscriptRequests_Status 
ON TranscriptRequests(Status);

-- Create index on RequestDate for sorting
CREATE INDEX IX_TranscriptRequests_RequestDate 
ON TranscriptRequests(RequestDate);

-- ============================================================================
-- Course Catalog & Enrollment Tables (US 2.1, 2.2, 2.3, 2.4)
-- SPRINT 2 - CURRICULUM MODULE (Assignments & Coursework)
-- User Stories: 2.7 (Create Assignment), 2.8 (Submit Assignment), 2.9 (Grade)
-- ============================================================================

-- Courses table
-- Note: ProfessorUserID is kept for backward compatibility, but CourseProfessors table should be used for new data
CREATE TABLE Courses (
    CourseID      SERIAL PRIMARY KEY,
    Code          VARCHAR(20) NOT NULL UNIQUE,      -- e.g. 'CS101', 'MATH201'
    Name          VARCHAR(200) NOT NULL,
    Description   TEXT NULL,
    Credits       INT NOT NULL DEFAULT 3
                    CHECK (Credits > 0 AND Credits <= 6),
    Department    VARCHAR(100) NOT NULL,
    Semester      VARCHAR(50) NOT NULL,              -- e.g. 'FALL2024', 'SPRING2024', 'SUMMER2024'
    Type          VARCHAR(20) NOT NULL               -- CORE, ELECTIVE
                    CHECK (Type IN ('CORE', 'ELECTIVE')),
    ProfessorUserID INT NULL,                        -- Kept for backward compatibility
    MaxSeats      INT NOT NULL DEFAULT 30
                    CHECK (MaxSeats > 0),
    CurrentSeats  INT NOT NULL DEFAULT 0
                    CHECK (CurrentSeats >= 0),
    IsActive      BOOLEAN NOT NULL DEFAULT TRUE,           -- Soft delete flag
    CreatedDate   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ProfessorUserID) REFERENCES Users(UserID)
);

-- Create index on Code for faster lookups
CREATE INDEX IX_Courses_Code ON Courses(Code);

-- Create index on Department for filtering
CREATE INDEX IX_Courses_Department ON Courses(Department);

-- Create index on Semester for filtering
CREATE INDEX IX_Courses_Semester ON Courses(Semester);

-- Create index on Type for filtering
CREATE INDEX IX_Courses_Type ON Courses(Type);

-- Create index on IsActive for filtering active courses
CREATE INDEX IX_Courses_IsActive ON Courses(IsActive);

-- Create index on ProfessorUserID for backward compatibility
CREATE INDEX IX_Courses_ProfessorUserID ON Courses(ProfessorUserID);

-- CourseProfessors table (Many-to-Many: Courses can have multiple professors)
-- This is the primary method for linking courses to professors
CREATE TABLE CourseProfessors (
    CourseProfessorID SERIAL PRIMARY KEY,
    CourseID          INT NOT NULL,
    ProfessorUserID   INT NOT NULL,
    CreatedDate       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (ProfessorUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    UNIQUE (CourseID, ProfessorUserID)              -- Prevent duplicate assignments
);

-- Create index on CourseID for faster lookups
CREATE INDEX IX_CourseProfessors_CourseID ON CourseProfessors(CourseID);

-- Create index on ProfessorUserID for faster lookups
CREATE INDEX IX_CourseProfessors_ProfessorUserID ON CourseProfessors(ProfessorUserID);

-- Prerequisites table (Many-to-Many: Courses can have multiple prerequisites)
-- Stores which courses are prerequisites for other courses (e.g., CS201 requires CS101)
CREATE TABLE Prerequisites (
    PrerequisiteID SERIAL PRIMARY KEY,
    CourseID       INT NOT NULL,                    -- Course that requires the prerequisite
    PrerequisiteCourseID INT NOT NULL,              -- Course that must be completed first
    CreatedDate    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (PrerequisiteCourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    UNIQUE (CourseID, PrerequisiteCourseID),       -- Prevent duplicate prerequisites
    CHECK (CourseID != PrerequisiteCourseID)        -- Prevent self-reference
);

-- Create index on CourseID for faster lookups
CREATE INDEX IX_Prerequisites_CourseID ON Prerequisites(CourseID);

-- Create index on PrerequisiteCourseID for faster lookups
CREATE INDEX IX_Prerequisites_PrerequisiteCourseID ON Prerequisites(PrerequisiteCourseID);

-- CourseAttributes table (EAV - Entity-Attribute-Value pattern for flexible attributes)
-- Allows storing additional course attributes without schema changes
CREATE TABLE CourseAttributes (
    AttributeID   SERIAL PRIMARY KEY,
    CourseID      INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,            -- e.g. 'LabHours', 'Online', 'PrerequisitesText'
    AttributeValue TEXT NULL,                -- Flexible value (can be text, number, JSON, etc.)
    AttributeType VARCHAR(20) DEFAULT 'TEXT'         -- TEXT, NUMBER, BOOLEAN, DATE, JSON
                    CHECK (AttributeType IN ('TEXT', 'NUMBER', 'BOOLEAN', 'DATE', 'JSON')),
    CreatedDate   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    UNIQUE (CourseID, AttributeName)                -- One attribute name per course
);

-- Create index on CourseID for faster lookups
CREATE INDEX IX_CourseAttributes_CourseID ON CourseAttributes(CourseID);

-- Create index on AttributeName for filtering
CREATE INDEX IX_CourseAttributes_AttributeName ON CourseAttributes(AttributeName);

-- Enrollments table
CREATE TABLE Enrollments (
    EnrollmentID  SERIAL PRIMARY KEY,
    StudentUserID INT NOT NULL,
    CourseID      INT NOT NULL,
    EnrollmentDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Status        VARCHAR(20) DEFAULT 'ENROLLED'      -- ENROLLED, DROPPED, COMPLETED, FAILED
                    CHECK (Status IN ('ENROLLED', 'DROPPED', 'COMPLETED', 'FAILED')),
    Grade         VARCHAR(5) NULL,                    -- e.g. 'A', 'B+', 'C', 'F', NULL if not graded yet
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE
);

-- Create partial unique index to prevent duplicate active enrollments
-- This allows re-enrollment after drop but prevents multiple ENROLLED status for same student/course
CREATE UNIQUE INDEX IX_Enrollments_UniqueActiveEnrollment 
ON Enrollments(StudentUserID, CourseID) 
WHERE Status = 'ENROLLED';

-- Create index on StudentUserID for faster student lookups
CREATE INDEX IX_Enrollments_StudentUserID ON Enrollments(StudentUserID);

-- Create index on CourseID for faster course lookups
CREATE INDEX IX_Enrollments_CourseID ON Enrollments(CourseID);

-- Create index on Status for filtering
CREATE INDEX IX_Enrollments_Status ON Enrollments(Status);

-- Create composite index for common queries
CREATE INDEX IX_Enrollments_Student_Course_Status ON Enrollments(StudentUserID, CourseID, Status);

-- Assignments table (US 2.7 - Create Assignment)
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

-- AssignmentAttributes table (EAV pattern for assignment metadata)
CREATE TABLE AssignmentAttributes (
    AttributeID SERIAL PRIMARY KEY,
    AssignmentID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue TEXT,
    FOREIGN KEY (AssignmentID) REFERENCES Assignments(AssignmentID)
);

CREATE INDEX IX_AssignmentAttributes_AssignmentID ON AssignmentAttributes(AssignmentID);

-- AssignmentSubmissions table (US 2.8 - Submit Assignment, US 2.9 - Grade)
CREATE TABLE AssignmentSubmissions (
    SubmissionID SERIAL PRIMARY KEY,
    AssignmentID INT NOT NULL,
    StudentUserID INT NOT NULL,
    SubmissionText TEXT,
    FileName VARCHAR(255),
    SubmittedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    Score INT,
    Feedback TEXT,
    Status VARCHAR(50) DEFAULT 'SUBMITTED' CHECK (Status IN ('SUBMITTED', 'GRADED')),
    GradedDate TIMESTAMP NULL,
    FOREIGN KEY (AssignmentID) REFERENCES Assignments(AssignmentID),
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID)
);

CREATE INDEX IX_AssignmentSubmissions_AssignmentID ON AssignmentSubmissions(AssignmentID);
CREATE INDEX IX_AssignmentSubmissions_StudentUserID ON AssignmentSubmissions(StudentUserID);
CREATE INDEX IX_AssignmentSubmissions_Status ON AssignmentSubmissions(Status);

-- CourseMaterials table (US 2.5 - Upload Course Materials, US 2.6 - View Course Materials)
-- Stores course materials uploaded by professors (files and links)
CREATE TABLE CourseMaterials (
    MaterialID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    Title VARCHAR(255) NOT NULL,
    Description TEXT NULL,
    MaterialType VARCHAR(20) NOT NULL
        CHECK (MaterialType IN ('LECTURE', 'READING', 'VIDEO', 'LINK')),
    FileName VARCHAR(255) NULL,              -- Original filename or link text
    FilePath VARCHAR(500) NOT NULL,           -- Path to file or URL for links
    FileSizeBytes BIGINT DEFAULT 0,            -- File size in bytes (0 for links)
    UploadDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UploadedByUserID INT NOT NULL,             -- Professor who uploaded
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (UploadedByUserID) REFERENCES Users(UserID)
);

-- Create index on CourseID for faster queries
CREATE INDEX IX_CourseMaterials_CourseID 
ON CourseMaterials(CourseID);

-- Create index on UploadDate for sorting (US 2.6 - organized by upload date)
CREATE INDEX IX_CourseMaterials_UploadDate 
ON CourseMaterials(UploadDate DESC);

-- Create index on UploadedByUserID
CREATE INDEX IX_CourseMaterials_UploadedBy 
ON CourseMaterials(UploadedByUserID);

-- ============================================================================
-- QUIZZES & EXAMS TABLES (US 2.10, 2.11, 2.12, 2.13)
-- ============================================================================

-- Quizzes table (US 2.10 - Create Quiz)
CREATE TABLE Quizzes (
    QuizID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    Instructions TEXT NULL,
    TotalPoints INT NOT NULL,
    DueDate TIMESTAMP NOT NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE
);

CREATE INDEX IX_Quizzes_CourseID ON Quizzes(CourseID);
CREATE INDEX IX_Quizzes_DueDate ON Quizzes(DueDate);

-- QuizAttributes table (EAV pattern for quiz metadata)
CREATE TABLE QuizAttributes (
    AttributeID SERIAL PRIMARY KEY,
    QuizID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue TEXT,
    FOREIGN KEY (QuizID) REFERENCES Quizzes(QuizID) ON DELETE CASCADE
);

CREATE INDEX IX_QuizAttributes_QuizID ON QuizAttributes(QuizID);

-- QuizQuestions table (Questions for quizzes)
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

-- QuizQuestionOptions table (Answer options for MCQ questions)
CREATE TABLE QuizQuestionOptions (
    OptionID SERIAL PRIMARY KEY,
    QuestionID INT NOT NULL,
    OptionText TEXT NOT NULL,
    IsCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    OptionOrder INT NOT NULL,
    FOREIGN KEY (QuestionID) REFERENCES QuizQuestions(QuestionID) ON DELETE CASCADE
);

CREATE INDEX IX_QuizQuestionOptions_QuestionID ON QuizQuestionOptions(QuestionID);

-- QuizAttempts table (US 2.11 - Take Quiz)
CREATE TABLE QuizAttempts (
    AttemptID SERIAL PRIMARY KEY,
    QuizID INT NOT NULL,
    StudentUserID INT NOT NULL,
    AttemptNumber INT NOT NULL DEFAULT 1,
    StartedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CompletedDate TIMESTAMP NULL,
    Score INT NULL,
    Status VARCHAR(50) DEFAULT 'IN_PROGRESS' CHECK (Status IN ('IN_PROGRESS', 'COMPLETED', 'TIMED_OUT')),
    FOREIGN KEY (QuizID) REFERENCES Quizzes(QuizID) ON DELETE CASCADE,
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    UNIQUE(QuizID, StudentUserID, AttemptNumber)
);

CREATE INDEX IX_QuizAttempts_QuizID ON QuizAttempts(QuizID);
CREATE INDEX IX_QuizAttempts_StudentUserID ON QuizAttempts(StudentUserID);
CREATE INDEX IX_QuizAttempts_Status ON QuizAttempts(Status);

-- Exams table (US 2.12 - Create Exam)
CREATE TABLE Exams (
    ExamID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    ExamDate TIMESTAMP NOT NULL,
    DurationMinutes INT NOT NULL,
    Location VARCHAR(200) NULL,
    TotalPoints INT NOT NULL,
    Instructions TEXT NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE
);

CREATE INDEX IX_Exams_CourseID ON Exams(CourseID);
CREATE INDEX IX_Exams_ExamDate ON Exams(ExamDate);

-- ExamAttributes table (EAV pattern for exam metadata)
CREATE TABLE ExamAttributes (
    AttributeID SERIAL PRIMARY KEY,
    ExamID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue TEXT,
    FOREIGN KEY (ExamID) REFERENCES Exams(ExamID) ON DELETE CASCADE
);

CREATE INDEX IX_ExamAttributes_ExamID ON ExamAttributes(ExamID);

-- ExamGrades table (US 2.13 - Record Exam Grades)
CREATE TABLE ExamGrades (
    ExamGradeID SERIAL PRIMARY KEY,
    ExamID INT NOT NULL,
    StudentUserID INT NOT NULL,
    PointsEarned INT NULL,
    Comments TEXT NULL,
    GradedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ExamID) REFERENCES Exams(ExamID) ON DELETE CASCADE,
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    UNIQUE(ExamID, StudentUserID)
);

CREATE INDEX IX_ExamGrades_ExamID ON ExamGrades(ExamID);
CREATE INDEX IX_ExamGrades_StudentUserID ON ExamGrades(StudentUserID);

-- ============================================================================
-- Table for storing grade weight distributions for courses
-- US: As a professor/system, I want to calculate final grades using weight distributions
-- ============================================================================

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

-- ============================================================================
-- StaffProfiles Table for Sprint 3
-- ============================================================================
CREATE TABLE IF NOT EXISTS StaffProfiles (
    StaffID SERIAL PRIMARY KEY,
    UserID INT NULL,
    Name VARCHAR(100) NOT NULL,
    Role VARCHAR(100) NOT NULL,
    Department VARCHAR(100) NOT NULL,
    Email VARCHAR(100) UNIQUE NOT NULL,
    OfficeHours VARCHAR(50),
    OfficeLocation VARCHAR(100),
    Phone VARCHAR(20),
    HireDate DATE,
    Bio TEXT,
    IsActive BOOLEAN DEFAULT TRUE,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_staff_department ON StaffProfiles(Department);
CREATE INDEX IF NOT EXISTS idx_staff_name ON StaffProfiles(Name);
CREATE INDEX IF NOT EXISTS idx_staff_active ON StaffProfiles(IsActive);

-- ============================================================================
-- Messages Table
-- ============================================================================
CREATE TABLE Messages (
    MessageID SERIAL PRIMARY KEY,
    SenderUserID INT NOT NULL,
    ReceiverUserID INT NOT NULL,
    Subject VARCHAR(200) NOT NULL,
    MessageBody TEXT NOT NULL,
    SentDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    IsRead BOOLEAN DEFAULT FALSE,
    ParentMessageID INT NULL,
    FOREIGN KEY (SenderUserID) REFERENCES Users(UserID),
    FOREIGN KEY (ReceiverUserID) REFERENCES Users(UserID),
    FOREIGN KEY (ParentMessageID) REFERENCES Messages(MessageID)
);

CREATE INDEX IX_Messages_Receiver ON Messages(ReceiverUserID);
CREATE INDEX IX_Messages_Sender ON Messages(SenderUserID);

-- ============================================================================
-- Course Staff Assignment Table (US 3.3.1 - Assign Staff to Course)
-- ============================================================================
CREATE TABLE IF NOT EXISTS CourseStaff (
    CourseStaffID SERIAL PRIMARY KEY,
    CourseID INT NOT NULL,
    StaffUserID INT NOT NULL,
    AssignmentDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (StaffUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    UNIQUE (CourseID, StaffUserID)  -- Prevent duplicate assignments
);

CREATE INDEX IF NOT EXISTS IX_CourseStaff_CourseID ON CourseStaff(CourseID);
CREATE INDEX IF NOT EXISTS IX_CourseStaff_StaffUserID ON CourseStaff(StaffUserID);

-- ============================================================================
-- END OF SCHEMA CREATION
-- ============================================================================
-- Note: Test data insertion scripts should be run separately
-- ============================================================================

