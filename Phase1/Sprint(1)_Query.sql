create Table Users(
	UserID   INT PRIMARY KEY AUTO_INCREMENT,
	USERNAME VARCHAR(50) NOT NULL UNIQUE,
	Password     VARCHAR(255) NOT NULL,
    UserType     ENUM('STUDENT', 'PROFESSOR', 'STAFF', 'ADMIN') NOT NULL
);

CREATE TABLE Students (
    UserID        INT PRIMARY KEY,
    StudentNumber VARCHAR(20),
    Major         VARCHAR(100),
	Department VARCHAR(100),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

CREATE TABLE Professors (
    UserID    INT PRIMARY KEY,
    Department VARCHAR(100),
    OfficeRoom VARCHAR(20),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

CREATE TABLE Staff (
    UserID    INT PRIMARY KEY,
    Department VARCHAR(100),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

CREATE TABLE Admins (
    UserID    INT PRIMARY KEY,
    RoleTitle VARCHAR(100),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

CREATE TABLE Rooms (
    RoomID   INT PRIMARY KEY AUTO_INCREMENT,
    Code     VARCHAR(20) NOT NULL,             -- e.g. 'R101', 'LAB1'
    Name     VARCHAR(100) NOT NULL,
    Type     ENUM('CLASSROOM', 'LAB') NOT NULL,
    Capacity INT,
    Location VARCHAR(100),
    Status   ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE')
             DEFAULT 'AVAILABLE'
) ;

CREATE TABLE EquipmentType (
    EquipmentTypeID INT PRIMARY KEY AUTO_INCREMENT,
    Name            VARCHAR(100) NOT NULL
);

CREATE TABLE RoomEquipment (
    RoomID          INT NOT NULL,
    EquipmentTypeID INT NOT NULL,
    Quantity        INT NOT NULL DEFAULT 1,
    PRIMARY KEY (RoomID, EquipmentTypeID),
    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID),
    FOREIGN KEY (EquipmentTypeID) REFERENCES EquipmentType(EquipmentTypeID)
);

CREATE TABLE MaintenanceTickets (
    TicketID            INT PRIMARY KEY AUTO_INCREMENT,
    RoomID              INT NOT NULL,
    ReporterUserID      INT NOT NULL,
    AssignedStaffUserID INT NULL,
    Description         TEXT NOT NULL,
    Status              ENUM('NEW', 'IN_PROGRESS', 'RESOLVED')
                        DEFAULT 'NEW',
    CreatedAt           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ResolvedAt          DATETIME NULL,

    FOREIGN KEY (RoomID) REFERENCES Rooms(RoomID)

    FOREIGN KEY (ReporterUserID) REFERENCES Users(UserID)

    FOREIGN KEY (AssignedStaffUserID) REFERENCES Users(UserID)
  
);
