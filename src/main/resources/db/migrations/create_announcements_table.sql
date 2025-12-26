-- Announcements table
-- US 4.4 - View Announcements
CREATE TABLE Announcements (
    AnnouncementID INT PRIMARY KEY IDENTITY(1,1),
    Title VARCHAR(200) NOT NULL,
    Content VARCHAR(MAX) NOT NULL,
    TargetRole VARCHAR(20) NULL,                      -- NULL = all users, or 'STUDENT', 'PROFESSOR', 'STAFF', 'ADMIN'
    CreatedByUserID INT NOT NULL,                    -- User who created the announcement
    CreatedDate DATETIME2 NOT NULL DEFAULT GETDATE(),
    ExpiryDate DATETIME2 NULL,                       -- Optional expiry date
    IsActive BIT NOT NULL DEFAULT 1,                 -- Soft delete flag
    Priority VARCHAR(20) DEFAULT 'NORMAL'            -- LOW, NORMAL, HIGH, URGENT
        CHECK (Priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    FOREIGN KEY (CreatedByUserID) REFERENCES Users(UserID)
);
GO

-- Create index on TargetRole for filtering
CREATE INDEX IX_Announcements_TargetRole ON Announcements(TargetRole);
GO

-- Create index on CreatedDate for sorting
CREATE INDEX IX_Announcements_CreatedDate ON Announcements(CreatedDate DESC);
GO

-- Create index on IsActive for filtering active announcements
CREATE INDEX IX_Announcements_IsActive ON Announcements(IsActive);
GO

-- AnnouncementReadStatus table (tracks which users have read which announcements)
CREATE TABLE AnnouncementReadStatus (
    ReadStatusID INT PRIMARY KEY IDENTITY(1,1),
    AnnouncementID INT NOT NULL,
    UserID INT NOT NULL,
    ReadDate DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (AnnouncementID) REFERENCES Announcements(AnnouncementID) ON DELETE CASCADE,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    UNIQUE (AnnouncementID, UserID)                   -- Prevent duplicate read status
);
GO

-- Create index on UserID for faster lookups
CREATE INDEX IX_AnnouncementReadStatus_UserID ON AnnouncementReadStatus(UserID);
GO

-- Create index on AnnouncementID for faster lookups
CREATE INDEX IX_AnnouncementReadStatus_AnnouncementID ON AnnouncementReadStatus(AnnouncementID);
GO



