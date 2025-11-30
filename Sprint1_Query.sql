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
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
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