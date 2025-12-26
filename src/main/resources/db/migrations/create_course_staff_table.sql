-- CourseStaff table (Many-to-Many: Courses can have multiple staff members)
-- US 3.3.1 - Assign Staff to Course
-- US 3.3.2 - View Assigned Courses
CREATE TABLE CourseStaff (
    CourseStaffID INT PRIMARY KEY IDENTITY(1,1),
    CourseID      INT NOT NULL,
    StaffUserID   INT NOT NULL,
    Role          VARCHAR(50) NULL,                    -- e.g., 'TA', 'LAB_ASSISTANT', 'TUTOR', 'COORDINATOR'
    CreatedDate   DATETIME2 DEFAULT GETDATE(),
    CreatedByUserID INT NULL,                          -- Admin who made the assignment
    FOREIGN KEY (CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE,
    FOREIGN KEY (StaffUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (CreatedByUserID) REFERENCES Users(UserID),
    UNIQUE (CourseID, StaffUserID)                     -- Prevent duplicate assignments
);
GO

-- Create index on CourseID for faster lookups
CREATE INDEX IX_CourseStaff_CourseID ON CourseStaff(CourseID);
GO

-- Create index on StaffUserID for faster lookups
CREATE INDEX IX_CourseStaff_StaffUserID ON CourseStaff(StaffUserID);
GO

-- Create index on Role for filtering
CREATE INDEX IX_CourseStaff_Role ON CourseStaff(Role);
GO



