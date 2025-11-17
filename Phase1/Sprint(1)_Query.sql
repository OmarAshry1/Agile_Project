USE agile;
GO

-- Users table
CREATE TABLE Users (
    UserID   INT PRIMARY KEY IDENTITY(1,1),
    USERNAME VARCHAR(50) NOT NULL UNIQUE,
    [Password] VARCHAR(255) NOT NULL,
    UserType VARCHAR(20) NOT NULL
        CHECK (UserType IN ('STUDENT', 'PROFESSOR', 'STAFF', 'ADMIN'))
);
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
    Code     VARCHAR(20) NOT NULL,             -- e.g. 'R101', 'LAB1'
    Name     VARCHAR(100) NOT NULL,
    Type     VARCHAR(20) NOT NULL
               CHECK (Type IN ('CLASSROOM', 'LAB')),
    Capacity INT,
    Location VARCHAR(100),
    Status   VARCHAR(20) DEFAULT 'AVAILABLE'
               CHECK (Status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE'))
);
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
    AssignedStaffUserID INT NULL,
    Description         TEXT NOT NULL,
    Status              VARCHAR(20) DEFAULT 'NEW'
                        CHECK (Status IN ('NEW', 'IN_PROGRESS', 'RESOLVED')),
    CreatedAt           DATETIME NOT NULL DEFAULT GETDATE(),
    ResolvedAt          DATETIME NULL,
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (ReporterUserID) REFERENCES Users(UserID),
    FOREIGN KEY (AssignedStaffUserID) REFERENCES Users(UserID)
);
GO