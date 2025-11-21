# Ticket Assignment Feature - Database Setup

## Overview
This feature allows:
- **Admins**: View all maintenance tickets and assign them to staff members
- **Staff**: View tickets assigned to them

## Database Schema Requirements

The `MaintenanceTickets` table needs an `AssignedToUserID` column to support ticket assignment.

### SQL Script to Add Column

If the column doesn't exist, run this SQL script:

```sql
-- Add AssignedToUserID column to MaintenanceTickets table
ALTER TABLE MaintenanceTickets 
ADD AssignedToUserID INT NULL;

-- Add foreign key constraint (optional but recommended)
ALTER TABLE MaintenanceTickets
ADD CONSTRAINT FK_MaintenanceTickets_AssignedToUser 
FOREIGN KEY (AssignedToUserID) REFERENCES Users(UserID);

-- Add index for better query performance
CREATE INDEX IX_MaintenanceTickets_AssignedToUserID 
ON MaintenanceTickets(AssignedToUserID);
```

### Verify Column Exists

```sql
-- Check if column exists
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'MaintenanceTickets' 
  AND COLUMN_NAME = 'AssignedToUserID';
```

## Features Implemented

### 1. MaintenanceService Methods
- `getAllTickets()` - Get all tickets (for admins)
- `getTicketsByAssignee(String staffUserId)` - Get tickets assigned to a staff member
- `assignTicketToStaff(String ticketId, String staffUserId)` - Assign ticket to staff
- `getStaffUsers()` - Get list of all staff users

### 2. TicketsViewController
- **For Admins**: 
  - View all tickets in a table
  - Select a ticket and assign it to a staff member via dropdown
  - See assignee information for each ticket
  
- **For Staff**:
  - View only tickets assigned to them
  - See ticket details (room, reporter, description, status, dates)

### 3. Dashboard Updates
- **Admins**: "View All Tickets" button → Tickets view with assignment capability
- **Staff**: "My Assigned Tickets" button → Tickets view (filtered to their tickets)
- **Students/Professors**: "Report Maintenance Issue" button → Ticket creation form

## Usage

1. **Admin assigns a ticket**:
   - Navigate to "View All Tickets" from dashboard
   - Select a ticket from the table
   - Select a staff member from the dropdown
   - Click "Assign" button
   - Confirm assignment

2. **Staff views assigned tickets**:
   - Navigate to "My Assigned Tickets" from dashboard
   - View all tickets assigned to them
   - Tickets show room, reporter, description, status, and dates

## File Changes

### New Files
- `src/main/java/edu/facilities/ui/TicketsViewController.java` - Main controller for ticket viewing/assignment

### Modified Files
- `src/main/java/edu/facilities/service/MaintenanceService.java` - Added ticket query and assignment methods
- `src/main/java/edu/facilities/ui/dashboardcontroller.java` - Updated to show role-appropriate buttons

### FXML Files Needed
You'll need to create:
- `/fxml/tickets_view.fxml` - UI for the TicketsViewController

## FXML Structure Example

The `tickets_view.fxml` should include:
- TableView for tickets (ticketsTable)
- TableColumns: ticketIdColumn, roomColumn, reporterColumn, assigneeColumn, descriptionColumn, statusColumn, createdDateColumn
- Button for assignment (assignButton) - visible only for admins
- ComboBox for staff selection (staffComboBox) - visible only for admins
- Button for refresh (refreshButton)
- Button for back (backButton)
- Labels: titleLabel, assigneeLabel, staffComboBoxLabel

