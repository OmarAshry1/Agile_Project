-- ============================================================================
-- Performance & Research Tracking Tables
-- US 3.7, 3.8, 3.9, 3.10
-- ============================================================================
-- This script creates tables for:
--   - Performance Evaluations (US 3.7, 3.8)
--   - Research Activities (US 3.9, 3.10)
-- ============================================================================

USE agile;
GO

-- ============================================================================
-- Performance Evaluations Table (US 3.7, 3.8)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[PerformanceEvaluations]') AND type in (N'U'))
BEGIN
    CREATE TABLE PerformanceEvaluations (
        EvaluationID INT PRIMARY KEY IDENTITY(1,1),
        StaffUserID INT NOT NULL,                    -- Staff member being evaluated
        EvaluationPeriod VARCHAR(100) NOT NULL,       -- e.g., "Q1 2024", "Annual 2024"
        Score DECIMAL(5,2) NOT NULL,                  -- Performance score (0-100)
        EvaluatedByUserID INT NULL,                   -- Admin who recorded the evaluation
        EvaluationDate DATETIME2 NOT NULL DEFAULT GETDATE(),
        Notes VARCHAR(MAX) NULL,                      -- Additional notes/comments
        CreatedDate DATETIME2 NOT NULL DEFAULT GETDATE(),
        FOREIGN KEY (StaffUserID) REFERENCES Users(UserID),
        FOREIGN KEY (EvaluatedByUserID) REFERENCES Users(UserID)
    );
    
    -- Create indexes for performance
    CREATE INDEX IX_PerformanceEvaluations_StaffUserID ON PerformanceEvaluations(StaffUserID);
    CREATE INDEX IX_PerformanceEvaluations_EvaluationDate ON PerformanceEvaluations(EvaluationDate DESC);
    CREATE INDEX IX_PerformanceEvaluations_EvaluationPeriod ON PerformanceEvaluations(EvaluationPeriod);
    
    PRINT 'PerformanceEvaluations table created successfully.';
END
ELSE
BEGIN
    PRINT 'PerformanceEvaluations table already exists.';
END
GO

-- ============================================================================
-- Research Activities Table (US 3.9, 3.10)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ResearchActivities]') AND type in (N'U'))
BEGIN
    CREATE TABLE ResearchActivities (
        ResearchID INT PRIMARY KEY IDENTITY(1,1),
        StaffUserID INT NOT NULL,                     -- Staff member who conducted the research
        Title VARCHAR(500) NOT NULL,                  -- Research title (required)
        Type VARCHAR(100) NOT NULL,                   -- Research type (required): JOURNAL, CONFERENCE, BOOK, PATENT, etc.
        PublicationDate DATE NOT NULL,                -- Publication date (required)
        Description VARCHAR(MAX) NULL,                 -- Optional description
        JournalName VARCHAR(200) NULL,                -- Journal name (if applicable)
        ConferenceName VARCHAR(200) NULL,             -- Conference name (if applicable)
        Publisher VARCHAR(200) NULL,                  -- Publisher name
        DOI VARCHAR(100) NULL,                        -- Digital Object Identifier
        URL VARCHAR(500) NULL,                        -- Link to research
        CreatedDate DATETIME2 NOT NULL DEFAULT GETDATE(),
        UpdatedDate DATETIME2 NOT NULL DEFAULT GETDATE(),
        FOREIGN KEY (StaffUserID) REFERENCES Users(UserID)
    );
    
    -- Create indexes for performance
    CREATE INDEX IX_ResearchActivities_StaffUserID ON ResearchActivities(StaffUserID);
    CREATE INDEX IX_ResearchActivities_PublicationDate ON ResearchActivities(PublicationDate DESC);
    CREATE INDEX IX_ResearchActivities_Type ON ResearchActivities(Type);
    
    -- Create index for department filtering (via StaffProfiles join)
    -- This will be used when filtering by department in US 3.10
    
    PRINT 'ResearchActivities table created successfully.';
END
ELSE
BEGIN
    PRINT 'ResearchActivities table already exists.';
END
GO

PRINT '';
PRINT '============================================================================';
PRINT 'Performance & Research Tracking Tables Created';
PRINT '============================================================================';
PRINT 'Tables created:';
PRINT '  - PerformanceEvaluations (US 3.7, 3.8)';
PRINT '  - ResearchActivities (US 3.9, 3.10)';
PRINT '============================================================================';
GO

