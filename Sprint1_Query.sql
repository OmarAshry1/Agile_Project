-- ============================================================================
-- Complete Database Schema for Agile Facilities Management System
-- ============================================================================
-- This script creates all tables, constraints, and indexes needed for the system.
-- Run this script on a fresh database to set up the complete schema.
-- 
-- SQL Server Compatibility: SQL Server 2008 or later
-- Tested with: SQL Server 2012, 2014, 2016, 2017, 2019, 2022
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
-- Note: Uses DATETIME2 and VARCHAR(MAX) for modern SQL Server compatibility
-- ============================================================================

USE agile;
GO

-- Users table
CREATE TABLE Users (
    UserID   INT PRIMARY KEY IDENTITY(1,1),
    USERNAME VARCHAR(50) NOT NULL UNIQUE,
    [Password] VARCHAR(255) NOT NULL,
    Email    VARCHAR(100) NULL,              -- Email address (optional)
    UserType VARCHAR(20) NOT NULL
        CHECK (UserType IN ('STUDENT', 'PROFESSOR', 'STAFF', 'ADMIN'))
);
GO

-- Create index on Username for faster login lookups
CREATE INDEX IX_Users_Username 
ON Users(USERNAME);
GO

-- Create index on UserType for filtering users by role
CREATE INDEX IX_Users_UserType 
ON Users(UserType);
GO

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
    Notes         VARCHAR(MAX) NULL,                -- Admin notes
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

-- Create index on Status for filtering students by status
CREATE INDEX IX_Students_Status 
ON Students(Status);
GO

-- Create index on StudentNumber for lookups
CREATE INDEX IX_Students_StudentNumber 
ON Students(StudentNumber);
GO

-- Professors table
CREATE TABLE Professors (
    UserID    INT PRIMARY KEY,
    Department VARCHAR(100),
    OfficeRoom VARCHAR(20),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

-- Staff table
CREATE TABLE Staff (
    UserID    INT PRIMARY KEY,
    Department VARCHAR(100),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

-- Admins table
CREATE TABLE Admins (
    UserID    INT PRIMARY KEY,
    RoleTitle VARCHAR(100),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

-- Rooms table
CREATE TABLE Rooms (
    RoomID   INT PRIMARY KEY IDENTITY(1,1),
    Code     VARCHAR(20) NOT NULL UNIQUE,      -- e.g. 'R101', 'LAB1' (unique room identifier)
    Name     VARCHAR(100) NOT NULL,
    Type     VARCHAR(20) NOT NULL
               CHECK (Type IN ('CLASSROOM', 'LAB')),
    Capacity INT,
    Location VARCHAR(100),
    Status   VARCHAR(20) DEFAULT 'AVAILABLE'
               CHECK (Status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE'))
);
GO

-- Create index on Code for faster room lookups
CREATE INDEX IX_Rooms_Code 
ON Rooms(Code);
GO

-- Create index on Status for filtering rooms by availability
CREATE INDEX IX_Rooms_Status 
ON Rooms(Status);
GO

-- EquipmentType table
CREATE TABLE EquipmentType (
    EquipmentTypeID INT PRIMARY KEY IDENTITY(1,1),
    Name            VARCHAR(100) NOT NULL
);
GO

-- RoomEquipment table
CREATE TABLE RoomEquipment (
    RoomID          INT NOT NULL,
    EquipmentTypeID INT NOT NULL,
    Quantity        INT NOT NULL DEFAULT 1,
    PRIMARY KEY (RoomID, EquipmentTypeID),
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (EquipmentTypeID) REFERENCES EquipmentType(EquipmentTypeID)
);
GO

-- MaintenanceTickets table
CREATE TABLE MaintenanceTickets (
    TicketID            INT PRIMARY KEY IDENTITY(1,1),
    RoomID              INT NOT NULL,
    ReporterUserID      INT NOT NULL,
    AssignedToUserID    INT NULL,              -- Assigned staff member (NULL if unassigned)
    Description         VARCHAR(MAX) NOT NULL, -- Changed from TEXT (deprecated) to VARCHAR(MAX)
    Status              VARCHAR(20) DEFAULT 'NEW'
                        CHECK (Status IN ('NEW', 'IN_PROGRESS', 'RESOLVED')),
    CreatedDate         DATETIME2 NOT NULL DEFAULT GETDATE(), -- DATETIME2 for better precision and range
    ResolvedDate        DATETIME2 NULL,
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (ReporterUserID) REFERENCES Users(UserID),
    FOREIGN KEY (AssignedToUserID) REFERENCES Users(UserID)
);
GO

-- Create index on AssignedToUserID for better query performance when filtering by assigned staff
CREATE INDEX IX_MaintenanceTickets_AssignedToUserID 
ON MaintenanceTickets(AssignedToUserID);
GO

-- Create index on RoomID for better query performance
CREATE INDEX IX_MaintenanceTickets_RoomID 
ON MaintenanceTickets(RoomID);
GO

-- Create index on ReporterUserID for better query performance
CREATE INDEX IX_MaintenanceTickets_ReporterUserID 
ON MaintenanceTickets(ReporterUserID);
GO

-- Create index on Status for filtering tickets by status
CREATE INDEX IX_MaintenanceTickets_Status 
ON MaintenanceTickets(Status);
GO

-- Bookings table
CREATE TABLE Bookings (
    BookingID     INT PRIMARY KEY IDENTITY(1,1),
    RoomID         INT NOT NULL,
    UserID         INT NOT NULL,
    BookingDate    DATETIME2 NOT NULL,              -- Start date/time of booking
    EndDate        DATETIME2 NOT NULL,              -- End date/time of booking
    Purpose        VARCHAR(200) NULL,               -- Purpose/description of booking
    Status         VARCHAR(20) DEFAULT 'CONFIRMED'  -- CONFIRMED, CANCELLED
                CHECK (Status IN ('CONFIRMED', 'CANCELLED')),
    CreatedDate    DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

-- Create index on RoomID for faster room booking lookups
CREATE INDEX IX_Bookings_RoomID 
ON Bookings(RoomID);
GO

-- Create index on UserID for faster user booking lookups
CREATE INDEX IX_Bookings_UserID 
ON Bookings(UserID);
GO

-- Create index on BookingDate for faster date range queries
CREATE INDEX IX_Bookings_BookingDate 
ON Bookings(BookingDate);
GO

-- Create index on Status for filtering bookings by status
CREATE INDEX IX_Bookings_Status 
ON Bookings(Status);
GO

-- Equipment table (standalone equipment items)
CREATE TABLE Equipment (
    EquipmentID      INT PRIMARY KEY IDENTITY(1,1),
    EquipmentTypeID INT NOT NULL,
    SerialNumber    VARCHAR(100) NULL,              -- Optional serial number
    Status          VARCHAR(20) DEFAULT 'AVAILABLE' -- AVAILABLE, ALLOCATED, MAINTENANCE, RETIRED
                CHECK (Status IN ('AVAILABLE', 'ALLOCATED', 'MAINTENANCE', 'RETIRED')),
    Location        VARCHAR(200) NULL,              -- Current location
    Notes           VARCHAR(MAX) NULL,              -- Additional notes
    CreatedDate     DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (EquipmentTypeID) REFERENCES EquipmentType(EquipmentTypeID)
);
GO

-- Create index on EquipmentTypeID for faster lookups
CREATE INDEX IX_Equipment_EquipmentTypeID 
ON Equipment(EquipmentTypeID);
GO

-- Create index on Status for filtering equipment by availability
CREATE INDEX IX_Equipment_Status 
ON Equipment(Status);
GO

-- EquipmentAllocation table (tracks equipment allocated to staff/departments)
CREATE TABLE EquipmentAllocation (
    AllocationID    INT PRIMARY KEY IDENTITY(1,1),
    EquipmentID     INT NOT NULL,
    AllocatedToUserID INT NULL,                    -- NULL if allocated to department
    Department      VARCHAR(100) NULL,             -- NULL if allocated to specific user
    AllocatedByUserID INT NOT NULL,                -- Admin who allocated it
    AllocationDate  DATETIME2 NOT NULL DEFAULT GETDATE(),
    ReturnDate      DATETIME2 NULL,                -- NULL if still allocated
    Notes           VARCHAR(MAX) NULL,
    Status          VARCHAR(20) DEFAULT 'ACTIVE'   -- ACTIVE, RETURNED
                CHECK (Status IN ('ACTIVE', 'RETURNED')),
    FOREIGN KEY (EquipmentID) REFERENCES Equipment(EquipmentID),
    FOREIGN KEY (AllocatedToUserID) REFERENCES Users(UserID),
    FOREIGN KEY (AllocatedByUserID) REFERENCES Users(UserID)
);
GO

-- Create index on EquipmentID for faster lookups
CREATE INDEX IX_EquipmentAllocation_EquipmentID 
ON EquipmentAllocation(EquipmentID);
GO

-- Create index on AllocatedToUserID for faster user lookups
CREATE INDEX IX_EquipmentAllocation_AllocatedToUserID 
ON EquipmentAllocation(AllocatedToUserID);
GO

-- Create index on Department for faster department lookups
CREATE INDEX IX_EquipmentAllocation_Department 
ON EquipmentAllocation(Department);
GO

-- Create index on Status for filtering active allocations
CREATE INDEX IX_EquipmentAllocation_Status 
ON EquipmentAllocation(Status);
GO

-- SoftwareLicenses table
CREATE TABLE SoftwareLicenses (
    LicenseID       INT PRIMARY KEY IDENTITY(1,1),
    SoftwareName   VARCHAR(200) NOT NULL,
    LicenseKey      VARCHAR(500) NULL,             -- Optional license key
    Vendor          VARCHAR(200) NULL,
    PurchaseDate    DATETIME2 NULL,
    ExpiryDate      DATETIME2 NULL,               -- NULL for perpetual licenses
    Cost            DECIMAL(10,2) NULL,            -- Purchase/renewal cost
    Quantity        INT NOT NULL DEFAULT 1,        -- Number of licenses
    UsedQuantity    INT NOT NULL DEFAULT 0,        -- Number of licenses in use
    Status          VARCHAR(20) DEFAULT 'ACTIVE'   -- ACTIVE, EXPIRED, CANCELLED
                CHECK (Status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    Notes           VARCHAR(MAX) NULL,
    CreatedDate     DATETIME2 NOT NULL DEFAULT GETDATE(),
    UpdatedDate     DATETIME2 NOT NULL DEFAULT GETDATE()
);
GO

-- Create index on ExpiryDate for finding near-expiring licenses
CREATE INDEX IX_SoftwareLicenses_ExpiryDate 
ON SoftwareLicenses(ExpiryDate);
GO

-- Create index on Status for filtering active licenses
CREATE INDEX IX_SoftwareLicenses_Status 
ON SoftwareLicenses(Status);
GO

-- ============================================================================
-- Admission Applications Table (US 2.5 - Admission Application Management)
-- ============================================================================
CREATE TABLE AdmissionApplications (
    ApplicationID    INT PRIMARY KEY IDENTITY(1,1),
    FirstName        VARCHAR(100) NOT NULL,
    LastName         VARCHAR(100) NOT NULL,
    Email            VARCHAR(100) NOT NULL,
    PhoneNumber      VARCHAR(20) NULL,
    DateOfBirth      DATE NULL,
    Address          VARCHAR(MAX) NULL,
    City             VARCHAR(100) NULL,
    State            VARCHAR(50) NULL,
    ZipCode          VARCHAR(20) NULL,
    Country          VARCHAR(100) NULL,
    Program          VARCHAR(100) NULL,              -- Program/Major applying for
    PreviousEducation VARCHAR(MAX) NULL,             -- Previous education details
    Documents        VARCHAR(MAX) NULL,              -- Document references/notes
    Status           VARCHAR(20) DEFAULT 'SUBMITTED'
                        CHECK (Status IN ('SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED')),
    SubmittedDate    DATETIME2 NOT NULL DEFAULT GETDATE(),
    ReviewedDate      DATETIME2 NULL,
    ReviewedByUserID INT NULL,                       -- Admin who reviewed
    Notes            VARCHAR(MAX) NULL,               -- Admin notes
    FOREIGN KEY (ReviewedByUserID) REFERENCES Users(UserID)
);
GO

-- Create index on Status for faster filtering
CREATE INDEX IX_AdmissionApplications_Status 
ON AdmissionApplications(Status);
GO

-- Create index on SubmittedDate for sorting
CREATE INDEX IX_AdmissionApplications_SubmittedDate 
ON AdmissionApplications(SubmittedDate);
GO

-- Create index on Email for lookups
CREATE INDEX IX_AdmissionApplications_Email 
ON AdmissionApplications(Email);
GO

-- ============================================================================
-- Transcript Requests Table (US 2.2, 2.3, 2.4 - Transcript Management)
-- ============================================================================
CREATE TABLE TranscriptRequests (
    RequestID       INT PRIMARY KEY IDENTITY(1,1),
    StudentUserID   INT NOT NULL,                    -- Student requesting transcript
    RequestDate     DATETIME2 NOT NULL DEFAULT GETDATE(),
    Status          VARCHAR(20) DEFAULT 'PENDING'    -- PENDING, IN_PROGRESS, READY_FOR_PICKUP, COMPLETED, CANCELLED
                        CHECK (Status IN ('PENDING', 'IN_PROGRESS', 'READY_FOR_PICKUP', 'COMPLETED', 'CANCELLED')),
    RequestedByUserID INT NOT NULL,                  -- Usually same as StudentUserID, but tracks who made the request
    ProcessedByUserID INT NULL,                      -- Admin who processed the request
    ProcessedDate   DATETIME2 NULL,                  -- When admin started processing
    CompletedDate   DATETIME2 NULL,                  -- When transcript was generated/completed
    PickupDate      DATETIME2 NULL,                  -- When student picked up transcript
    Purpose         VARCHAR(500) NULL,                -- Purpose of transcript request
    Notes           VARCHAR(MAX) NULL,                -- Admin notes
    PDFPath         VARCHAR(500) NULL,                -- Path to generated PDF file
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    FOREIGN KEY (RequestedByUserID) REFERENCES Users(UserID),
    FOREIGN KEY (ProcessedByUserID) REFERENCES Users(UserID)
);
GO

-- Create index on StudentUserID for faster student lookups
CREATE INDEX IX_TranscriptRequests_StudentUserID 
ON TranscriptRequests(StudentUserID);
GO

-- Create index on Status for faster filtering
CREATE INDEX IX_TranscriptRequests_Status 
ON TranscriptRequests(Status);
GO

-- Create index on RequestDate for sorting
CREATE INDEX IX_TranscriptRequests_RequestDate 
ON TranscriptRequests(RequestDate);
GO

-- ============================================================================
-- Course Catalog & Enrollment Tables (US 2.1, 2.2, 2.3, 2.4)
-- SPRINT 2 - CURRICULUM MODULE (Assignments & Coursework)
-- User Stories: 2.7 (Create Assignment), 2.8 (Submit Assignment), 2.9 (Grade)
-- ============================================================================

-- Courses table
-- Note: ProfessorUserID is kept for backward compatibility, but CourseProfessors table should be used for new data
CREATE TABLE Courses (
    CourseID      INT PRIMARY KEY IDENTITY(1,1),
    Code          VARCHAR(20) NOT NULL UNIQUE,      -- e.g. 'CS101', 'MATH201'
    Name          VARCHAR(200) NOT NULL,
    Description   VARCHAR(MAX) NULL,
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
    IsActive      BIT NOT NULL DEFAULT 1,           -- Soft delete flag
    CreatedDate   DATETIME2 NOT NULL DEFAULT GETDATE(),
    UpdatedDate   DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (ProfessorUserID) REFERENCES Users(UserID)
);
GO

-- Create index on Code for faster lookups
CREATE INDEX IX_Courses_Code ON Courses(Code);
GO

-- Create index on Department for filtering
CREATE INDEX IX_Courses_Department ON Courses(Department);
GO

-- Create index on Semester for filtering
CREATE INDEX IX_Courses_Semester ON Courses(Semester);
GO

-- Create index on Type for filtering
CREATE INDEX IX_Courses_Type ON Courses(Type);
GO

-- Create index on IsActive for filtering active courses
CREATE INDEX IX_Courses_IsActive ON Courses(IsActive);
GO

-- Create index on ProfessorUserID for backward compatibility
CREATE INDEX IX_Courses_ProfessorUserID ON Courses(ProfessorUserID);
GO

-- CourseProfessors table (Many-to-Many: Courses can have multiple professors)
-- This is the primary method for linking courses to professors
CREATE TABLE CourseProfessors (
    CourseProfessorID INT PRIMARY KEY IDENTITY(1,1),
    CourseID          INT NOT NULL,
    ProfessorUserID   INT NOT NULL,
    CreatedDate       DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (ProfessorUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    UNIQUE (CourseID, ProfessorUserID)              -- Prevent duplicate assignments
);
GO

-- Create index on CourseID for faster lookups
CREATE INDEX IX_CourseProfessors_CourseID ON CourseProfessors(CourseID);
GO

-- Create index on ProfessorUserID for faster lookups
CREATE INDEX IX_CourseProfessors_ProfessorUserID ON CourseProfessors(ProfessorUserID);
GO

-- Prerequisites table (Many-to-Many: Courses can have multiple prerequisites)
-- Stores which courses are prerequisites for other courses (e.g., CS201 requires CS101)
CREATE TABLE Prerequisites (
    PrerequisiteID INT PRIMARY KEY IDENTITY(1,1),
    CourseID       INT NOT NULL,                    -- Course that requires the prerequisite
    PrerequisiteCourseID INT NOT NULL,              -- Course that must be completed first
    CreatedDate    DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (PrerequisiteCourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    UNIQUE (CourseID, PrerequisiteCourseID),       -- Prevent duplicate prerequisites
    CHECK (CourseID != PrerequisiteCourseID)        -- Prevent self-reference
);
GO

-- Create index on CourseID for faster lookups
CREATE INDEX IX_Prerequisites_CourseID ON Prerequisites(CourseID);
GO

-- Create index on PrerequisiteCourseID for faster lookups
CREATE INDEX IX_Prerequisites_PrerequisiteCourseID ON Prerequisites(PrerequisiteCourseID);
GO

-- CourseAttributes table (EAV - Entity-Attribute-Value pattern for flexible attributes)
-- Allows storing additional course attributes without schema changes
CREATE TABLE CourseAttributes (
    AttributeID   INT PRIMARY KEY IDENTITY(1,1),
    CourseID      INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,            -- e.g. 'LabHours', 'Online', 'PrerequisitesText'
    AttributeValue VARCHAR(MAX) NULL,                -- Flexible value (can be text, number, JSON, etc.)
    AttributeType VARCHAR(20) DEFAULT 'TEXT'         -- TEXT, NUMBER, BOOLEAN, DATE, JSON
                    CHECK (AttributeType IN ('TEXT', 'NUMBER', 'BOOLEAN', 'DATE', 'JSON')),
    CreatedDate   DATETIME2 NOT NULL DEFAULT GETDATE(),
    UpdatedDate   DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    UNIQUE (CourseID, AttributeName)                -- One attribute name per course
);
GO

-- Create index on CourseID for faster lookups
CREATE INDEX IX_CourseAttributes_CourseID ON CourseAttributes(CourseID);
GO

-- Create index on AttributeName for filtering
CREATE INDEX IX_CourseAttributes_AttributeName ON CourseAttributes(AttributeName);
GO

-- Enrollments table
CREATE TABLE Enrollments (
    EnrollmentID  INT PRIMARY KEY IDENTITY(1,1),
    StudentUserID INT NOT NULL,
    CourseID      INT NOT NULL,
    EnrollmentDate DATETIME2 NOT NULL DEFAULT GETDATE(),
    Status        VARCHAR(20) DEFAULT 'ENROLLED'      -- ENROLLED, DROPPED, COMPLETED, FAILED
                    CHECK (Status IN ('ENROLLED', 'DROPPED', 'COMPLETED', 'FAILED')),
    Grade         VARCHAR(5) NULL,                    -- e.g. 'A', 'B+', 'C', 'F', NULL if not graded yet
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE
);
GO

-- Create filtered unique index to prevent duplicate active enrollments
-- This allows re-enrollment after drop but prevents multiple ENROLLED status for same student/course
CREATE UNIQUE NONCLUSTERED INDEX IX_Enrollments_UniqueActiveEnrollment 
ON Enrollments(StudentUserID, CourseID) 
WHERE Status = 'ENROLLED';
GO

-- Create index on StudentUserID for faster student lookups
CREATE INDEX IX_Enrollments_StudentUserID ON Enrollments(StudentUserID);
GO

-- Create index on CourseID for faster course lookups
CREATE INDEX IX_Enrollments_CourseID ON Enrollments(CourseID);
GO

-- Create index on Status for filtering
CREATE INDEX IX_Enrollments_Status ON Enrollments(Status);
GO

-- Create composite index for common queries
CREATE INDEX IX_Enrollments_Student_Course_Status ON Enrollments(StudentUserID, CourseID, Status);
GO

-- Assignments table (US 2.7 - Create Assignment)
CREATE TABLE Assignments (
    AssignmentID INT PRIMARY KEY IDENTITY(1,1),
    CourseID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    Instructions VARCHAR(MAX) NOT NULL,
    DueDate DATETIME2 NOT NULL,
    TotalPoints INT NOT NULL,
    SubmissionType VARCHAR(50) DEFAULT 'TEXT' CHECK (SubmissionType IN ('FILE', 'TEXT', 'BOTH')),
    CreatedDate DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID)
);
GO

CREATE INDEX IX_Assignments_CourseID ON Assignments(CourseID);
CREATE INDEX IX_Assignments_DueDate ON Assignments(DueDate);
GO

-- AssignmentAttributes table (EAV pattern for assignment metadata)
CREATE TABLE AssignmentAttributes (
    AttributeID INT PRIMARY KEY IDENTITY(1,1),
    AssignmentID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue VARCHAR(MAX),
    FOREIGN KEY (AssignmentID) REFERENCES Assignments(AssignmentID)
);
GO

CREATE INDEX IX_AssignmentAttributes_AssignmentID ON AssignmentAttributes(AssignmentID);
GO

-- AssignmentSubmissions table (US 2.8 - Submit Assignment, US 2.9 - Grade)
CREATE TABLE AssignmentSubmissions (
    SubmissionID INT PRIMARY KEY IDENTITY(1,1),
    AssignmentID INT NOT NULL,
    StudentUserID INT NOT NULL,
    SubmissionText VARCHAR(MAX),
    FileName VARCHAR(255),
    SubmittedDate DATETIME2 DEFAULT GETDATE(),
    Score INT,
    Feedback VARCHAR(MAX),
    Status VARCHAR(50) DEFAULT 'SUBMITTED' CHECK (Status IN ('SUBMITTED', 'GRADED')),
    GradedDate DATETIME2 NULL,
    FOREIGN KEY (AssignmentID) REFERENCES Assignments(AssignmentID),
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID)
);
GO

CREATE INDEX IX_AssignmentSubmissions_AssignmentID ON AssignmentSubmissions(AssignmentID);
CREATE INDEX IX_AssignmentSubmissions_StudentUserID ON AssignmentSubmissions(StudentUserID);
CREATE INDEX IX_AssignmentSubmissions_Status ON AssignmentSubmissions(Status);
GO

-- CourseMaterials table (US 2.5 - Upload Course Materials, US 2.6 - View Course Materials)
-- Stores course materials uploaded by professors (files and links)
CREATE TABLE CourseMaterials (
    MaterialID INT PRIMARY KEY IDENTITY(1,1),
    CourseID INT NOT NULL,
    Title NVARCHAR(255) NOT NULL,
    Description NVARCHAR(MAX) NULL,
    MaterialType VARCHAR(20) NOT NULL
        CHECK (MaterialType IN ('LECTURE', 'READING', 'VIDEO', 'LINK')),
    FileName NVARCHAR(255) NULL,              -- Original filename or link text
    FilePath NVARCHAR(500) NOT NULL,           -- Path to file or URL for links
    FileSizeBytes BIGINT DEFAULT 0,            -- File size in bytes (0 for links)
    UploadDate DATETIME2 DEFAULT GETDATE(),
    UploadedByUserID INT NOT NULL,             -- Professor who uploaded
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (UploadedByUserID) REFERENCES Users(UserID)
);
GO

-- Create index on CourseID for faster queries
CREATE INDEX IX_CourseMaterials_CourseID 
ON CourseMaterials(CourseID);
GO

-- Create index on UploadDate for sorting (US 2.6 - organized by upload date)
CREATE INDEX IX_CourseMaterials_UploadDate 
ON CourseMaterials(UploadDate DESC);
GO

-- Create index on UploadedByUserID
CREATE INDEX IX_CourseMaterials_UploadedBy 
ON CourseMaterials(UploadedByUserID);
GO

<<<<<<< Updated upstream
-- ============================================================================
-- QUIZZES & EXAMS TABLES (US 2.10, 2.11, 2.12, 2.13)
-- ============================================================================

-- Quizzes table (US 2.10 - Create Quiz)
CREATE TABLE Quizzes (
    QuizID INT PRIMARY KEY IDENTITY(1,1),
    CourseID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    Instructions VARCHAR(MAX) NULL,
    TotalPoints INT NOT NULL,
    DueDate DATETIME2 NOT NULL,
    CreatedDate DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE
);
GO

CREATE INDEX IX_Quizzes_CourseID ON Quizzes(CourseID);
CREATE INDEX IX_Quizzes_DueDate ON Quizzes(DueDate);
GO

-- QuizAttributes table (EAV pattern for quiz metadata)
CREATE TABLE QuizAttributes (
    AttributeID INT PRIMARY KEY IDENTITY(1,1),
    QuizID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue VARCHAR(MAX),
    FOREIGN KEY (QuizID) REFERENCES Quizzes(QuizID) ON DELETE CASCADE
);
GO

CREATE INDEX IX_QuizAttributes_QuizID ON QuizAttributes(QuizID);
GO

-- QuizQuestions table (Questions for quizzes)
CREATE TABLE QuizQuestions (
    QuestionID INT PRIMARY KEY IDENTITY(1,1),
    QuizID INT NOT NULL,
    QuestionNumber INT NOT NULL,
    QuestionText VARCHAR(MAX) NOT NULL,
    QuestionType VARCHAR(20) NOT NULL CHECK (QuestionType IN ('MCQ', 'WRITTEN')),
    Points INT NOT NULL DEFAULT 1,
    FOREIGN KEY (QuizID) REFERENCES Quizzes(QuizID) ON DELETE CASCADE,
    UNIQUE(QuizID, QuestionNumber)
);
GO

CREATE INDEX IX_QuizQuestions_QuizID ON QuizQuestions(QuizID);
GO

-- QuizQuestionOptions table (Answer options for MCQ questions)
CREATE TABLE QuizQuestionOptions (
    OptionID INT PRIMARY KEY IDENTITY(1,1),
    QuestionID INT NOT NULL,
    OptionText VARCHAR(MAX) NOT NULL,
    IsCorrect BIT NOT NULL DEFAULT 0,
    OptionOrder INT NOT NULL,
    FOREIGN KEY (QuestionID) REFERENCES QuizQuestions(QuestionID) ON DELETE CASCADE
);
GO

CREATE INDEX IX_QuizQuestionOptions_QuestionID ON QuizQuestionOptions(QuestionID);
GO

-- QuizAttempts table (US 2.11 - Take Quiz)
CREATE TABLE QuizAttempts (
    AttemptID INT PRIMARY KEY IDENTITY(1,1),
    QuizID INT NOT NULL,
    StudentUserID INT NOT NULL,
    AttemptNumber INT NOT NULL DEFAULT 1,
    StartedDate DATETIME2 DEFAULT GETDATE(),
    CompletedDate DATETIME2 NULL,
    Score INT NULL,
    Status VARCHAR(50) DEFAULT 'IN_PROGRESS' CHECK (Status IN ('IN_PROGRESS', 'COMPLETED', 'TIMED_OUT')),
    FOREIGN KEY (QuizID) REFERENCES Quizzes(QuizID) ON DELETE CASCADE,
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    UNIQUE(QuizID, StudentUserID, AttemptNumber)
);
GO

CREATE INDEX IX_QuizAttempts_QuizID ON QuizAttempts(QuizID);
CREATE INDEX IX_QuizAttempts_StudentUserID ON QuizAttempts(StudentUserID);
CREATE INDEX IX_QuizAttempts_Status ON QuizAttempts(Status);
GO

-- Exams table (US 2.12 - Create Exam)
CREATE TABLE Exams (
    ExamID INT PRIMARY KEY IDENTITY(1,1),
    CourseID INT NOT NULL,
    Title VARCHAR(200) NOT NULL,
    ExamDate DATETIME2 NOT NULL,
    DurationMinutes INT NOT NULL,
    Location VARCHAR(200) NULL,
    TotalPoints INT NOT NULL,
    Instructions VARCHAR(MAX) NULL,
    CreatedDate DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE
);
GO

CREATE INDEX IX_Exams_CourseID ON Exams(CourseID);
CREATE INDEX IX_Exams_ExamDate ON Exams(ExamDate);
GO

-- ExamAttributes table (EAV pattern for exam metadata)
CREATE TABLE ExamAttributes (
    AttributeID INT PRIMARY KEY IDENTITY(1,1),
    ExamID INT NOT NULL,
    AttributeName VARCHAR(100) NOT NULL,
    AttributeValue VARCHAR(MAX),
    FOREIGN KEY (ExamID) REFERENCES Exams(ExamID) ON DELETE CASCADE
);
GO

CREATE INDEX IX_ExamAttributes_ExamID ON ExamAttributes(ExamID);
GO

-- ExamGrades table (US 2.13 - Record Exam Grades)
CREATE TABLE ExamGrades (
    ExamGradeID INT PRIMARY KEY IDENTITY(1,1),
    ExamID INT NOT NULL,
    StudentUserID INT NOT NULL,
    PointsEarned INT NULL,
    Comments VARCHAR(MAX) NULL,
    GradedDate DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (ExamID) REFERENCES Exams(ExamID) ON DELETE CASCADE,
    FOREIGN KEY (StudentUserID) REFERENCES Users(UserID),
    UNIQUE(ExamID, StudentUserID)
);
GO

CREATE INDEX IX_ExamGrades_ExamID ON ExamGrades(ExamID);
CREATE INDEX IX_ExamGrades_StudentUserID ON ExamGrades(StudentUserID);
GO


-- ============================================================================
-- Table for storing grade weight distributions for courses
-- US: As a professor/system, I want to calculate final grades using weight distributions
-- ============================================================================

CREATE TABLE CourseGradeWeights (
                                    CourseID INT PRIMARY KEY,
                                    AssignmentsWeight DECIMAL(5,2) NOT NULL DEFAULT 0.00,
                                    QuizzesWeight DECIMAL(5,2) NOT NULL DEFAULT 0.00,
                                    ExamsWeight DECIMAL(5,2) NOT NULL DEFAULT 0.00,
                                    CreatedDate DATETIME2 DEFAULT GETDATE(),
                                    UpdatedDate DATETIME2 DEFAULT GETDATE(),
                                    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
                                    CONSTRAINT CHK_WeightsSum CHECK (AssignmentsWeight + QuizzesWeight + ExamsWeight = 100.00)
);
GO

CREATE INDEX IX_CourseGradeWeights_CourseID ON CourseGradeWeights(CourseID);
GO

-- ============================================================================
-- TEST DATA SECTION
-- ============================================================================
-- Sample data for testing the Curriculum Module
-- This section inserts sample courses and links them to professors
-- ============================================================================

-- Verify professor UserID 1003 exists
IF NOT EXISTS (SELECT 1 FROM Users WHERE UserID = 1003 AND UserType = 'PROFESSOR')
BEGIN
    PRINT 'WARNING: UserID 1003 does not exist or is not a PROFESSOR';
    PRINT 'Please ensure a professor user with UserID 1003 exists before running this script';
END
ELSE
BEGIN
    PRINT 'Professor UserID 1003 verified';
END
GO

-- Insert Sample Courses for Professor 1003
PRINT '';
PRINT 'Inserting sample courses...';

-- Course 1: Introduction to Programming
IF NOT EXISTS (SELECT 1 FROM Courses WHERE Code = 'CS101')
BEGIN
    INSERT INTO Courses (Code, Name, Description, Credits, Department, Semester, Type, ProfessorUserID, CreatedDate, MaxSeats, CurrentSeats, IsActive, UpdatedDate)
    VALUES ('CS101', 'Introduction to Programming', 
            'Basic programming concepts including variables, control structures, functions, and object-oriented programming fundamentals.', 
            3, 'Computer Science', 'Fall 2024', 'CORE', 1003, GETDATE(), 30, 0, 1, GETDATE());
    PRINT 'Inserted course: CS101 - Introduction to Programming';
END
ELSE
BEGIN
    PRINT 'Course CS101 already exists, skipping...';
END
GO

-- Course 2: Data Structures
IF NOT EXISTS (SELECT 1 FROM Courses WHERE Code = 'CS201')
BEGIN
    INSERT INTO Courses (Code, Name, Description, Credits, Department, Semester, Type, ProfessorUserID, CreatedDate, MaxSeats, CurrentSeats, IsActive, UpdatedDate)
    VALUES ('CS201', 'Data Structures', 
            'Advanced data structures including arrays, linked lists, stacks, queues, trees, and graphs. Analysis of algorithms and complexity.', 
            3, 'Computer Science', 'Fall 2024', 'CORE', 1003, GETDATE(), 25, 0, 1, GETDATE());
    PRINT 'Inserted course: CS201 - Data Structures';
END
ELSE
BEGIN
    PRINT 'Course CS201 already exists, skipping...';
END
GO

-- Course 3: Database Systems (Elective)
IF NOT EXISTS (SELECT 1 FROM Courses WHERE Code = 'CS301')
BEGIN
    INSERT INTO Courses (Code, Name, Description, Credits, Department, Semester, Type, ProfessorUserID, CreatedDate, MaxSeats, CurrentSeats, IsActive, UpdatedDate)
    VALUES ('CS301', 'Database Systems', 
            'Introduction to database design, SQL, normalization, transaction management, and database administration.', 
            3, 'Computer Science', 'Fall 2024', 'ELECTIVE', 1003, GETDATE(), 20, 0, 1, GETDATE());
    PRINT 'Inserted course: CS301 - Database Systems';
END
ELSE
BEGIN
    PRINT 'Course CS301 already exists, skipping...';
END
GO

-- Course 4: Software Engineering
IF NOT EXISTS (SELECT 1 FROM Courses WHERE Code = 'CS401')
BEGIN
    INSERT INTO Courses (Code, Name, Description, Credits, Department, Semester, Type, ProfessorUserID, CreatedDate, MaxSeats, CurrentSeats, IsActive, UpdatedDate)
    VALUES ('CS401', 'Software Engineering', 
            'Software development lifecycle, requirements analysis, design patterns, testing, and project management.', 
            4, 'Computer Science', 'Fall 2024', 'CORE', 1003, GETDATE(), 20, 0, 1, GETDATE());
    PRINT 'Inserted course: CS401 - Software Engineering';
END
ELSE
BEGIN
    PRINT 'Course CS401 already exists, skipping...';
END
GO

-- Link courses to professor in CourseProfessors table
PRINT '';
PRINT 'Linking courses to professor in CourseProfessors table...';

-- Link CS101
IF EXISTS (SELECT 1 FROM Courses WHERE Code = 'CS101')
BEGIN
    INSERT INTO CourseProfessors (CourseID, ProfessorUserID, CreatedDate)
    SELECT c.CourseID, 1003, GETDATE()
    FROM Courses c
    WHERE c.Code = 'CS101'
      AND NOT EXISTS (
          SELECT 1 FROM CourseProfessors cp 
          WHERE cp.CourseID = c.CourseID AND cp.ProfessorUserID = 1003
      );
    PRINT 'Linked CS101 to professor 1003';
END
GO

-- Link CS201
IF EXISTS (SELECT 1 FROM Courses WHERE Code = 'CS201')
BEGIN
    INSERT INTO CourseProfessors (CourseID, ProfessorUserID, CreatedDate)
    SELECT c.CourseID, 1003, GETDATE()
    FROM Courses c
    WHERE c.Code = 'CS201'
      AND NOT EXISTS (
          SELECT 1 FROM CourseProfessors cp 
          WHERE cp.CourseID = c.CourseID AND cp.ProfessorUserID = 1003
      );
    PRINT 'Linked CS201 to professor 1003';
END
GO

-- Link CS301
IF EXISTS (SELECT 1 FROM Courses WHERE Code = 'CS301')
BEGIN
    INSERT INTO CourseProfessors (CourseID, ProfessorUserID, CreatedDate)
    SELECT c.CourseID, 1003, GETDATE()
    FROM Courses c
    WHERE c.Code = 'CS301'
      AND NOT EXISTS (
          SELECT 1 FROM CourseProfessors cp 
          WHERE cp.CourseID = c.CourseID AND cp.ProfessorUserID = 1003
      );
    PRINT 'Linked CS301 to professor 1003';
END
GO

-- Link CS401
IF EXISTS (SELECT 1 FROM Courses WHERE Code = 'CS401')
BEGIN
    INSERT INTO CourseProfessors (CourseID, ProfessorUserID, CreatedDate)
    SELECT c.CourseID, 1003, GETDATE()
    FROM Courses c
    WHERE c.Code = 'CS401'
      AND NOT EXISTS (
          SELECT 1 FROM CourseProfessors cp 
          WHERE cp.CourseID = c.CourseID AND cp.ProfessorUserID = 1003
      );
    PRINT 'Linked CS401 to professor 1003';
END
GO

-- Add sample prerequisite (CS201 requires CS101)
PRINT '';
PRINT 'Adding sample prerequisites...';

IF EXISTS (SELECT 1 FROM Courses WHERE Code = 'CS101') AND EXISTS (SELECT 1 FROM Courses WHERE Code = 'CS201')
BEGIN
    INSERT INTO Prerequisites (CourseID, PrerequisiteCourseID, CreatedDate)
    SELECT c2.CourseID, c1.CourseID, GETDATE()
    FROM Courses c1, Courses c2
    WHERE c1.Code = 'CS101' AND c2.Code = 'CS201'
      AND NOT EXISTS (
          SELECT 1 FROM Prerequisites p 
          WHERE p.CourseID = c2.CourseID AND p.PrerequisiteCourseID = c1.CourseID
      );
    PRINT 'Added prerequisite: CS201 requires CS101';
END
GO

-- Verification
PRINT '';
PRINT '============================================================================';
PRINT 'Test Data Verification';
PRINT '============================================================================';

PRINT '';
PRINT 'Courses linked to Professor 1003 via CourseProfessors:';
SELECT 
    c.CourseID,
    c.Code,
    c.Name,
    c.Department,
    c.Semester,
    c.Type,
    c.MaxSeats,
    c.CurrentSeats,
    c.IsActive,
    cp.CreatedDate AS LinkDate
FROM Courses c
INNER JOIN CourseProfessors cp ON c.CourseID = cp.CourseID
WHERE cp.ProfessorUserID = 1003
ORDER BY c.Code;

PRINT '';
DECLARE @CourseCount INT = (SELECT COUNT(*) FROM CourseProfessors WHERE ProfessorUserID = 1003);
PRINT 'Total courses linked to Professor 1003: ' + CAST(@CourseCount AS VARCHAR);
PRINT '';
PRINT '============================================================================';
PRINT 'Test Data Insertion Complete';
PRINT '============================================================================';
GO
-- ============================================================================
-- StaffProfiles Table for Sprint 3
-- ============================================================================
-- Check if table exists before creating to prevent errors on re-run
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[StaffProfiles]') AND type in (N'U'))
BEGIN
    CREATE TABLE StaffProfiles (
        StaffID INT PRIMARY KEY IDENTITY(1,1),
        UserID INT NULL,
        Name VARCHAR(100) NOT NULL,
        Role VARCHAR(100) NOT NULL,
        Department VARCHAR(100) NOT NULL,
        Email VARCHAR(100) UNIQUE NOT NULL,
        OfficeHours VARCHAR(50),
        OfficeLocation VARCHAR(100),
        Phone VARCHAR(20),
        HireDate DATE,
        Bio VARCHAR(MAX),
        IsActive BIT DEFAULT 1,
        CreatedDate DATETIME2 DEFAULT GETDATE(),
        UpdatedDate DATETIME2 DEFAULT GETDATE(),
        FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE SET NULL
    );
    PRINT 'StaffProfiles table created successfully.';
END
ELSE
BEGIN
    PRINT 'StaffProfiles table already exists.';
END
GO

-- Indexes for performance
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_staff_department' AND object_id = OBJECT_ID('StaffProfiles'))
    CREATE INDEX idx_staff_department ON StaffProfiles(Department);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_staff_name' AND object_id = OBJECT_ID('StaffProfiles'))
    CREATE INDEX idx_staff_name ON StaffProfiles(Name);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_staff_active' AND object_id = OBJECT_ID('StaffProfiles'))
    CREATE INDEX idx_staff_active ON StaffProfiles(IsActive);
GO

-- Sample Data for Testing (10 records)
-- Using IF NOT EXISTS to prevent duplicates if script is run multiple times
IF NOT EXISTS (SELECT 1 FROM StaffProfiles WHERE Email = 'john.smith@edu.com')
BEGIN
    INSERT INTO StaffProfiles (Name, Role, Department, Email, OfficeHours, OfficeLocation, Phone, HireDate, Bio) VALUES 
    ('Dr. John Smith', 'Professor', 'Computer Science', 'john.smith@edu.com', 'MWF 10-12', 'CS Building Room 301', '555-0101', '2018-08-15', 'Expert in Algorithms and Data Structures. PhD from MIT.'),
    ('Dr. Sarah Johnson', 'Associate Professor', 'Mathematics', 'sarah.johnson@edu.com', 'TTh 2-4', 'Math Building Room 205', '555-0102', '2015-03-20', 'Specializes in Calculus and Linear Algebra.'),
    ('Prof. Michael Chen', 'Assistant Professor', 'Physics', 'michael.chen@edu.com', 'MW 1-3', 'Physics Building Room 101', '555-0103', '2020-01-10', 'Quantum Mechanics researcher.'),
    ('Dr. Emily Williams', 'Department Chair', 'Computer Science', 'emily.williams@edu.com', 'By Appointment', 'CS Building Room 401', '555-0104', '2010-09-01', 'Head of Computer Science Department.'),
    ('Mr. Robert Brown', 'Lab Technician', 'Engineering', 'robert.brown@edu.com', 'M-F 9-5', 'Engineering Lab B', '555-0105', '2019-11-15', 'Manages engineering laboratory equipment.'),
    ('Dr. Maria Garcia', 'Professor', 'Biology', 'maria.garcia@edu.com', 'TTh 11-1', 'Biology Building Room 150', '555-0106', '2012-07-22', 'Molecular biology expert.'),
    ('Ms. Lisa Wong', 'Administrator', 'Administration', 'lisa.wong@edu.com', 'M-F 8-4', 'Admin Building Room 100', '555-0107', '2018-04-30', 'Student services coordinator.'),
    ('Dr. James Wilson', 'Professor', 'Chemistry', 'james.wilson@edu.com', 'MWF 9-11', 'Chemistry Building Room 210', '555-0108', '2009-12-05', 'Organic chemistry specialist.'),
    ('Mr. David Miller', 'IT Support', 'Information Technology', 'david.miller@edu.com', 'M-F 10-6', 'IT Center Room 15', '555-0109', '2021-03-18', 'Technical support for faculty and students.'),
    ('Dr. Jennifer Lee', 'Associate Professor', 'Psychology', 'jennifer.lee@edu.com', 'TTh 3-5', 'Psychology Building Room 320', '555-0110', '2017-06-12', 'Clinical psychology and counseling.');
    PRINT 'StaffProfiles sample data inserted.';
END
GO

USE agile;
GO

-- Create Messages table
CREATE TABLE Messages (
    MessageID INT PRIMARY KEY IDENTITY(1,1),
    SenderUserID INT NOT NULL,
    ReceiverUserID INT NOT NULL,
    Subject VARCHAR(200) NOT NULL,
    MessageBody VARCHAR(MAX) NOT NULL,
    SentDate DATETIME2 DEFAULT GETDATE(),
    IsRead BIT DEFAULT 0,
    ParentMessageID INT NULL,
    FOREIGN KEY (SenderUserID) REFERENCES Users(UserID),
    FOREIGN KEY (ReceiverUserID) REFERENCES Users(UserID),
    FOREIGN KEY (ParentMessageID) REFERENCES Messages(MessageID)
);
GO

CREATE INDEX IX_Messages_Receiver ON Messages(ReceiverUserID);
CREATE INDEX IX_Messages_Sender ON Messages(SenderUserID);
GO

PRINT 'Messages table created!';
GO
```

### To Run This:
1. Open SQL Server Management Studio (SSMS)
2. Connect to your database
3. Make sure you're using the `agile` database
4. Paste the script above
5. Click **Execute** (or press F5)

You should see:
```
Messages table created!

