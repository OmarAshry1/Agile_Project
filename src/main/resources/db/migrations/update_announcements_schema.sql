-- Update Announcements table for US 3.1-3.3
-- Add Status, PublishDate, IsArchived fields

-- Add new columns to Announcements table
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Announcements') AND name = 'Status')
BEGIN
    ALTER TABLE Announcements ADD Status VARCHAR(20) DEFAULT 'DRAFT'
        CHECK (Status IN ('DRAFT', 'PUBLISHED'));
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Announcements') AND name = 'PublishDate')
BEGIN
    ALTER TABLE Announcements ADD PublishDate DATETIME2 NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Announcements') AND name = 'IsArchived')
BEGIN
    ALTER TABLE Announcements ADD IsArchived BIT NOT NULL DEFAULT 0;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Announcements') AND name = 'LastModifiedDate')
BEGIN
    ALTER TABLE Announcements ADD LastModifiedDate DATETIME2 NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Announcements') AND name = 'LastModifiedByUserID')
BEGIN
    ALTER TABLE Announcements ADD LastModifiedByUserID INT NULL;
    ALTER TABLE Announcements ADD CONSTRAINT FK_Announcements_LastModifiedByUser 
        FOREIGN KEY (LastModifiedByUserID) REFERENCES Users(UserID);
END
GO

-- Update existing announcements to PUBLISHED if they are active
UPDATE Announcements SET Status = 'PUBLISHED', PublishDate = CreatedDate 
WHERE IsActive = 1 AND Status IS NULL;
GO

-- Create index on Status
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Announcements_Status' AND object_id = OBJECT_ID('Announcements'))
BEGIN
    CREATE INDEX IX_Announcements_Status ON Announcements(Status);
END
GO

-- Create index on PublishDate
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Announcements_PublishDate' AND object_id = OBJECT_ID('Announcements'))
BEGIN
    CREATE INDEX IX_Announcements_PublishDate ON Announcements(PublishDate DESC);
END
GO

-- Create index on IsArchived
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Announcements_IsArchived' AND object_id = OBJECT_ID('Announcements'))
BEGIN
    CREATE INDEX IX_Announcements_IsArchived ON Announcements(IsArchived);
END
GO

-- AnnouncementAttachments table (for file attachments)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'AnnouncementAttachments')
BEGIN
    CREATE TABLE AnnouncementAttachments (
        AttachmentID INT PRIMARY KEY IDENTITY(1,1),
        AnnouncementID INT NOT NULL,
        FileName VARCHAR(255) NOT NULL,
        FilePath VARCHAR(500) NOT NULL,
        FileSize BIGINT NULL,
        MimeType VARCHAR(100) NULL,
        UploadedDate DATETIME2 NOT NULL DEFAULT GETDATE(),
        FOREIGN KEY (AnnouncementID) REFERENCES Announcements(AnnouncementID) ON DELETE CASCADE
    );
    
    CREATE INDEX IX_AnnouncementAttachments_AnnouncementID ON AnnouncementAttachments(AnnouncementID);
END
GO

-- AnnouncementLinks table (for external links)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'AnnouncementLinks')
BEGIN
    CREATE TABLE AnnouncementLinks (
        LinkID INT PRIMARY KEY IDENTITY(1,1),
        AnnouncementID INT NOT NULL,
        LinkText VARCHAR(200) NOT NULL,
        LinkURL VARCHAR(500) NOT NULL,
        CreatedDate DATETIME2 NOT NULL DEFAULT GETDATE(),
        FOREIGN KEY (AnnouncementID) REFERENCES Announcements(AnnouncementID) ON DELETE CASCADE
    );
    
    CREATE INDEX IX_AnnouncementLinks_AnnouncementID ON AnnouncementLinks(AnnouncementID);
END
GO

-- AnnouncementEditHistory table (stores edit history)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'AnnouncementEditHistory')
BEGIN
    CREATE TABLE AnnouncementEditHistory (
        EditHistoryID INT PRIMARY KEY IDENTITY(1,1),
        AnnouncementID INT NOT NULL,
        EditedByUserID INT NOT NULL,
        EditDate DATETIME2 NOT NULL DEFAULT GETDATE(),
        PreviousTitle VARCHAR(200) NULL,
        PreviousContent VARCHAR(MAX) NULL,
        PreviousTargetRole VARCHAR(20) NULL,
        FOREIGN KEY (AnnouncementID) REFERENCES Announcements(AnnouncementID) ON DELETE CASCADE,
        FOREIGN KEY (EditedByUserID) REFERENCES Users(UserID)
    );
    
    CREATE INDEX IX_AnnouncementEditHistory_AnnouncementID ON AnnouncementEditHistory(AnnouncementID);
    CREATE INDEX IX_AnnouncementEditHistory_EditDate ON AnnouncementEditHistory(EditDate DESC);
END
GO



