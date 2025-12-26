package edu.facilities.ui;

import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import edu.community.service.MessageService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class dashboardcontroller {

    private AuthService authService;
    private MessageService messageService;

    @FXML
    public void initialize() {
        authService = AuthService.getInstance();
        messageService = new MessageService();
        updateUI();
    }

    private void updateUI() {
        if (authService.isLoggedIn()) {
            User currentUser = authService.getCurrentUser();
            userIdLabel.setText(currentUser.getUsername() + " / " + currentUser.getId());

            updateUnreadCount();

            // Show logout button, hide login/register
            if (logoutButton != null)
                logoutButton.setVisible(true);
            if (loginButton != null)
                loginButton.setVisible(false);
            if (registerButton != null)
                registerButton.setVisible(false);

            // Enable buttons
            if (roomsButton != null)
                roomsButton.setDisable(false);
            if (reportButton != null)
                reportButton.setDisable(false);

            // Update button text and behavior based on user role
            String userType = authService.getCurrentUserType();
            if ("ADMIN".equals(userType)) {
                // Admins can view all tickets and assign them
                if (reportButton != null)
                    reportButton.setText("View All Tickets");
                // Hide and disable "View My Tickets" button for admins
                if (viewMyTicketsButton != null) {
                    viewMyTicketsButton.setVisible(false);
                    viewMyTicketsButton.setManaged(false);
                    viewMyTicketsButton.setDisable(true);
                }
                // Hide booking buttons for admins
                if (bookRoomButton != null) {
                    bookRoomButton.setVisible(false);
                    bookRoomButton.setManaged(false);
                    bookRoomButton.setDisable(true);
                }
                if (viewMyBookingsButton != null) {
                    viewMyBookingsButton.setVisible(false);
                    viewMyBookingsButton.setManaged(false);
                    viewMyBookingsButton.setDisable(true);
                }
                // Hide equipment viewing for admins (they have admin tools instead)
                if (viewEquipmentButton != null) {
                    viewEquipmentButton.setVisible(false);
                    viewEquipmentButton.setManaged(false);
                    viewEquipmentButton.setDisable(true);
                }
                if (myAllocatedEquipmentButton != null) {
                    myAllocatedEquipmentButton.setVisible(false);
                    myAllocatedEquipmentButton.setManaged(false);
                    myAllocatedEquipmentButton.setDisable(true);
                }
                // Show admin equipment management buttons
                if (allocateEquipmentButton != null) {
                    allocateEquipmentButton.setVisible(true);
                    allocateEquipmentButton.setManaged(true);
                    allocateEquipmentButton.setDisable(false);
                }
                if (addEquipmentButton != null) {
                    addEquipmentButton.setVisible(true);
                    addEquipmentButton.setManaged(true);
                    addEquipmentButton.setDisable(false);
                }
                if (softwareLicensesButton != null) {
                    softwareLicensesButton.setVisible(true);
                    softwareLicensesButton.setManaged(true);
                    softwareLicensesButton.setDisable(false);
                }
                if (admissionApplicationsButton != null) {
                    admissionApplicationsButton.setVisible(true);
                    admissionApplicationsButton.setManaged(true);
                    admissionApplicationsButton.setDisable(false);
                }
                if (studentRecordsButton != null) {
                    studentRecordsButton.setVisible(true);
                    studentRecordsButton.setManaged(true);
                    studentRecordsButton.setDisable(false);
                }
                if (transcriptRequestsButton != null) {
                    transcriptRequestsButton.setVisible(true);
                    transcriptRequestsButton.setManaged(true);
                    transcriptRequestsButton.setDisable(false);
                }
                // Show course management button for admins
                if (manageCoursesButton != null) {
                    manageCoursesButton.setVisible(true);
                    manageCoursesButton.setManaged(true);
                    manageCoursesButton.setDisable(false);
                }
                // Show assign courses button for admins (US 3.3)
                if (assignCoursesButton != null) {
                    assignCoursesButton.setVisible(true);
                    assignCoursesButton.setManaged(true);
                    assignCoursesButton.setDisable(false);
                }
                // Show assign staff to courses button for admins (US 3.3.1)
                if (assignStaffToCourseButton != null) {
                    assignStaffToCourseButton.setVisible(true);
                    assignStaffToCourseButton.setManaged(true);
                    assignStaffToCourseButton.setDisable(false);
                }
                // Show announcements button for admins
                if (viewAnnouncementsButton != null) {
                    viewAnnouncementsButton.setVisible(true);
                    viewAnnouncementsButton.setManaged(true);
                    viewAnnouncementsButton.setDisable(false);
                }
                // Hide student transcript buttons for admins
                if (myTranscriptsButton != null) {
                    myTranscriptsButton.setVisible(false);
                    myTranscriptsButton.setManaged(false);
                    myTranscriptsButton.setDisable(true);
                }
                if (requestTranscriptButton != null) {
                    requestTranscriptButton.setVisible(false);
                    requestTranscriptButton.setManaged(false);
                    requestTranscriptButton.setDisable(true);
                }
                // Hide student course buttons for admins
                if (viewCourseCatalogButton != null) {
                    viewCourseCatalogButton.setVisible(false);
                    viewCourseCatalogButton.setManaged(false);
                    viewCourseCatalogButton.setDisable(true);
                }
                if (enrollInCoursesButton != null) {
                    enrollInCoursesButton.setVisible(false);
                    enrollInCoursesButton.setManaged(false);
                    enrollInCoursesButton.setDisable(true);
                }
                if (myEnrolledCoursesButton != null) {
                    myEnrolledCoursesButton.setVisible(false);
                    myEnrolledCoursesButton.setManaged(false);
                    myEnrolledCoursesButton.setDisable(true);
                }
                // Hide assignment buttons for admins
                if (myAssignmentsButton != null) {
                    myAssignmentsButton.setVisible(false);
                    myAssignmentsButton.setManaged(false);
                    myAssignmentsButton.setDisable(true);
                }
                if (manageAssignmentsButton != null) {
                    manageAssignmentsButton.setVisible(false);
                    manageAssignmentsButton.setManaged(false);
                    manageAssignmentsButton.setDisable(true);
                }
                if (viewGradebookButton != null) {
                    viewGradebookButton.setVisible(false);
                    viewGradebookButton.setManaged(false);
                    viewGradebookButton.setDisable(true);
                }
                // Hide quiz/exam buttons for admins
                if (myQuizzesButton != null) {
                    myQuizzesButton.setVisible(false);
                    myQuizzesButton.setManaged(false);
                    myQuizzesButton.setDisable(true);
                }
                if (myExamsButton != null) {
                    myExamsButton.setVisible(false);
                    myExamsButton.setManaged(false);
                    myExamsButton.setDisable(true);
                }
                if (manageQuizzesButton != null) {
                    manageQuizzesButton.setVisible(false);
                    manageQuizzesButton.setManaged(false);
                    manageQuizzesButton.setDisable(true);
                }
                if (manageExamsButton != null) {
                    manageExamsButton.setVisible(false);
                    manageExamsButton.setManaged(false);
                    manageExamsButton.setDisable(true);
                }

                // Show Staff Management for Admin
                if (manageStaffButton != null) {
                    manageStaffButton.setVisible(true);
                    manageStaffButton.setManaged(true);
                    manageStaffButton.setDisable(false);
                }
                // Show Staff Directory for Admin
                if (staffDirectoryButton != null) {
                    staffDirectoryButton.setVisible(true);
                    staffDirectoryButton.setManaged(true);
                    staffDirectoryButton.setDisable(false);
                }
                // Show performance and research management buttons for Admin
                if (recordPerformanceButton != null) {
                    recordPerformanceButton.setVisible(true);
                    recordPerformanceButton.setManaged(true);
                    recordPerformanceButton.setDisable(false);
                }
                if (reviewResearchButton != null) {
                    reviewResearchButton.setVisible(true);
                    reviewResearchButton.setManaged(true);
                    reviewResearchButton.setDisable(false);
                }
                // Show leave approval button for Admin
                if (approveLeaveButton != null) {
                    approveLeaveButton.setVisible(true);
                    approveLeaveButton.setManaged(true);
                    approveLeaveButton.setDisable(false);
                }
                // Show payroll and benefits management buttons for Admin
                if (managePayrollButton != null) {
                    managePayrollButton.setVisible(true);
                    managePayrollButton.setManaged(true);
                    managePayrollButton.setDisable(false);
                }
                if (manageBenefitsButton != null) {
                    manageBenefitsButton.setVisible(true);
                    manageBenefitsButton.setManaged(true);
                    manageBenefitsButton.setDisable(false);
                }
                // Hide staff-only view buttons for Admin
                if (viewPayrollButton != null) {
                    viewPayrollButton.setVisible(false);
                    viewPayrollButton.setManaged(false);
                    viewPayrollButton.setDisable(true);
                }
                if (viewBenefitsButton != null) {
                    viewBenefitsButton.setVisible(false);
                    viewBenefitsButton.setManaged(false);
                    viewBenefitsButton.setDisable(true);
                }
                // Show event management buttons for Admin
                if (createEventButton != null) {
                    createEventButton.setVisible(true);
                    createEventButton.setManaged(true);
                    createEventButton.setDisable(false);
                }
                if (eventsCalendarButton != null) {
                    eventsCalendarButton.setVisible(true);
                    eventsCalendarButton.setManaged(true);
                    eventsCalendarButton.setDisable(false);
                }
                // Show forum for Admin
                if (forumButton != null) {
                    forumButton.setVisible(true);
                    forumButton.setManaged(true);
                    forumButton.setDisable(false);
                }
                // Show message thread history for Admin (as teacher)
                if (messageThreadHistoryButton != null) {
                    messageThreadHistoryButton.setVisible(true);
                    messageThreadHistoryButton.setManaged(true);
                    messageThreadHistoryButton.setDisable(false);
                }
                // Show link child button for parents
                if (linkChildButton != null) {
                    linkChildButton.setVisible(true);
                    linkChildButton.setManaged(true);
                    linkChildButton.setDisable(false);
                }
                if (parentSendMessageButton != null) {
                    parentSendMessageButton.setVisible(true);
                    parentSendMessageButton.setManaged(true);
                    parentSendMessageButton.setDisable(false);
                }
                // Hide staff-only buttons for Admin
                if (viewPerformanceButton != null) {
                    viewPerformanceButton.setVisible(false);
                    viewPerformanceButton.setManaged(false);
                    viewPerformanceButton.setDisable(true);
                }
                if (addResearchButton != null) {
                    addResearchButton.setVisible(false);
                    addResearchButton.setManaged(false);
                    addResearchButton.setDisable(true);
                }
            } else {
                // Students, Staff, and Professors can create tickets and view their own
                // Staff can also view their assigned tickets from the tickets view
                if (reportButton != null)
                    reportButton.setText("Report Maintenance Issue");
                // Show and enable "View My Tickets" button for Students, Professors, and Staff
                // only
                if (viewMyTicketsButton != null) {
                    viewMyTicketsButton.setVisible(true);
                    viewMyTicketsButton.setManaged(true);
                    viewMyTicketsButton.setDisable(false);
                }

                // Show booking buttons only for PROFESSOR and STAFF
                if ("PROFESSOR".equals(userType) || "STAFF".equals(userType)) {
                    if (bookRoomButton != null) {
                        bookRoomButton.setVisible(true);
                        bookRoomButton.setManaged(true);
                        bookRoomButton.setDisable(false);
                    }
                    if (viewMyBookingsButton != null) {
                        viewMyBookingsButton.setVisible(true);
                        viewMyBookingsButton.setManaged(true);
                        viewMyBookingsButton.setDisable(false);
                    }
                    // Show equipment viewing for PROFESSOR and STAFF
                    if (viewEquipmentButton != null) {
                        viewEquipmentButton.setVisible(true);
                        viewEquipmentButton.setManaged(true);
                        viewEquipmentButton.setDisable(false);
                    }
                    if (myAllocatedEquipmentButton != null) {
                        myAllocatedEquipmentButton.setVisible(true);
                        myAllocatedEquipmentButton.setManaged(true);
                        myAllocatedEquipmentButton.setDisable(false);
                    }
                } else {
                    // Hide booking buttons for STUDENT
                    if (bookRoomButton != null) {
                        bookRoomButton.setVisible(false);
                        bookRoomButton.setManaged(false);
                        bookRoomButton.setDisable(true);
                    }
                    if (viewMyBookingsButton != null) {
                        viewMyBookingsButton.setVisible(false);
                        viewMyBookingsButton.setManaged(false);
                        viewMyBookingsButton.setDisable(true);
                    }
                    // Hide equipment buttons for STUDENT
                    if (viewEquipmentButton != null) {
                        viewEquipmentButton.setVisible(false);
                        viewEquipmentButton.setManaged(false);
                        viewEquipmentButton.setDisable(true);
                    }
                    if (myAllocatedEquipmentButton != null) {
                        myAllocatedEquipmentButton.setVisible(false);
                        myAllocatedEquipmentButton.setManaged(false);
                        myAllocatedEquipmentButton.setDisable(true);
                    }
                }
                // Hide admin equipment management buttons for non-admins
                if (allocateEquipmentButton != null) {
                    allocateEquipmentButton.setVisible(false);
                    allocateEquipmentButton.setManaged(false);
                    allocateEquipmentButton.setDisable(true);
                }
                if (addEquipmentButton != null) {
                    addEquipmentButton.setVisible(false);
                    addEquipmentButton.setManaged(false);
                    addEquipmentButton.setDisable(true);
                }
                if (softwareLicensesButton != null) {
                    softwareLicensesButton.setVisible(false);
                    softwareLicensesButton.setManaged(false);
                    softwareLicensesButton.setDisable(true);
                }
                if (admissionApplicationsButton != null) {
                    admissionApplicationsButton.setVisible(false);
                    admissionApplicationsButton.setManaged(false);
                    admissionApplicationsButton.setDisable(true);
                }
                if (studentRecordsButton != null) {
                    studentRecordsButton.setVisible(false);
                    studentRecordsButton.setManaged(false);
                    studentRecordsButton.setDisable(true);
                }
                if (transcriptRequestsButton != null) {
                    transcriptRequestsButton.setVisible(false);
                    transcriptRequestsButton.setManaged(false);
                    transcriptRequestsButton.setDisable(true);
                }
                // Show student transcript buttons for students only
                if ("STUDENT".equals(userType)) {
                    if (myTranscriptsButton != null) {
                        myTranscriptsButton.setVisible(true);
                        myTranscriptsButton.setManaged(true);
                        myTranscriptsButton.setDisable(false);
                    }
                    if (requestTranscriptButton != null) {
                        requestTranscriptButton.setVisible(true);
                        requestTranscriptButton.setManaged(true);
                        requestTranscriptButton.setDisable(false);
                    }
                    // Show course catalog buttons for students
                    if (viewCourseCatalogButton != null) {
                        viewCourseCatalogButton.setVisible(true);
                        viewCourseCatalogButton.setManaged(true);
                        viewCourseCatalogButton.setDisable(false);
                    }
                    if (enrollInCoursesButton != null) {
                        enrollInCoursesButton.setVisible(true);
                        enrollInCoursesButton.setManaged(true);
                        enrollInCoursesButton.setDisable(false);
                    }
                    if (myEnrolledCoursesButton != null) {
                        myEnrolledCoursesButton.setVisible(true);
                        myEnrolledCoursesButton.setManaged(true);
                        myEnrolledCoursesButton.setDisable(false);
                    }
                } else {
                    if (myTranscriptsButton != null) {
                        myTranscriptsButton.setVisible(false);
                        myTranscriptsButton.setManaged(false);
                        myTranscriptsButton.setDisable(true);
                    }
                    if (requestTranscriptButton != null) {
                        requestTranscriptButton.setVisible(false);
                        requestTranscriptButton.setManaged(false);
                        requestTranscriptButton.setDisable(true);
                    }
                    // Hide course catalog buttons for non-students
                    if (viewCourseCatalogButton != null) {
                        viewCourseCatalogButton.setVisible(false);
                        viewCourseCatalogButton.setManaged(false);
                        viewCourseCatalogButton.setDisable(true);
                    }
                    if (enrollInCoursesButton != null) {
                        enrollInCoursesButton.setVisible(false);
                        enrollInCoursesButton.setManaged(false);
                        enrollInCoursesButton.setDisable(true);
                    }
                    if (myEnrolledCoursesButton != null) {
                        myEnrolledCoursesButton.setVisible(false);
                        myEnrolledCoursesButton.setManaged(false);
                        myEnrolledCoursesButton.setDisable(true);
                    }
                }
                // Hide course management button for non-admins
                if (manageCoursesButton != null) {
                    manageCoursesButton.setVisible(false);
                    manageCoursesButton.setManaged(false);
                    manageCoursesButton.setDisable(true);
                }
                // Show assignment buttons based on role
                if ("STUDENT".equals(userType)) {
                    if (myAssignmentsButton != null) {
                        myAssignmentsButton.setVisible(true);
                        myAssignmentsButton.setManaged(true);
                        myAssignmentsButton.setDisable(false);
                    }
                } else {
                    if (myAssignmentsButton != null) {
                        myAssignmentsButton.setVisible(false);
                        myAssignmentsButton.setManaged(false);
                        myAssignmentsButton.setDisable(true);
                    }
                }
                if ("PROFESSOR".equals(userType)) {
                    if (manageAssignmentsButton != null) {
                        manageAssignmentsButton.setVisible(true);
                        manageAssignmentsButton.setManaged(true);
                        manageAssignmentsButton.setDisable(false);
                    }
                    if (viewGradebookButton != null) {
                        viewGradebookButton.setVisible(true);
                        viewGradebookButton.setManaged(true);
                        viewGradebookButton.setDisable(false);
                    }
                    if (calculateFinalGradesButton != null) {
                        calculateFinalGradesButton.setVisible(true);
                        calculateFinalGradesButton.setManaged(true);
                        calculateFinalGradesButton.setDisable(false);
                    }
                } else {
                    if (manageAssignmentsButton != null) {
                        manageAssignmentsButton.setVisible(false);
                        manageAssignmentsButton.setManaged(false);
                        manageAssignmentsButton.setDisable(true);
                    }
                    if (viewGradebookButton != null) {
                        viewGradebookButton.setVisible(false);
                        viewGradebookButton.setManaged(false);
                        viewGradebookButton.setDisable(true);
                    }
                    if (calculateFinalGradesButton != null) {
                        calculateFinalGradesButton.setVisible(false);
                        calculateFinalGradesButton.setManaged(false);
                        calculateFinalGradesButton.setDisable(true);
                    }
                }
                // Show quiz/exam buttons based on role
                if ("STUDENT".equals(userType)) {
                    if (myQuizzesButton != null) {
                        myQuizzesButton.setVisible(true);
                        myQuizzesButton.setManaged(true);
                        myQuizzesButton.setDisable(false);
                    }
                    if (myExamsButton != null) {
                        myExamsButton.setVisible(true);
                        myExamsButton.setManaged(true);
                        myExamsButton.setDisable(false);
                    }
                    if (courseGradesButton != null) {
                        courseGradesButton.setVisible(true);
                        courseGradesButton.setManaged(true);
                        courseGradesButton.setDisable(false);
                    }
                    // Show student messaging and forum buttons
                    if (studentSendMessageButton != null) {
                        studentSendMessageButton.setVisible(true);
                        studentSendMessageButton.setManaged(true);
                        studentSendMessageButton.setDisable(false);
                    }
                    if (forumButton != null) {
                        forumButton.setVisible(true);
                        forumButton.setManaged(true);
                        forumButton.setDisable(false);
                    }
                    if (eventsCalendarButton != null) {
                        eventsCalendarButton.setVisible(true);
                        eventsCalendarButton.setManaged(true);
                        eventsCalendarButton.setDisable(false);
                    }
                } else {
                    if (myQuizzesButton != null) {
                        myQuizzesButton.setVisible(false);
                        myQuizzesButton.setManaged(false);
                        myQuizzesButton.setDisable(true);
                    }
                    if (myExamsButton != null) {
                        myExamsButton.setVisible(false);
                        myExamsButton.setManaged(false);
                        myExamsButton.setDisable(true);
                    }
                    if (courseGradesButton != null) {
                        courseGradesButton.setVisible(false);
                        courseGradesButton.setManaged(false);
                        courseGradesButton.setDisable(true);
                    }
                }
                if ("PROFESSOR".equals(userType)) {
                    if (manageQuizzesButton != null) {
                        manageQuizzesButton.setVisible(true);
                        manageQuizzesButton.setManaged(true);
                        manageQuizzesButton.setDisable(false);
                    }
                    if (manageExamsButton != null) {
                        manageExamsButton.setVisible(true);
                        manageExamsButton.setManaged(true);
                        manageExamsButton.setDisable(false);
                    }
                } else {
                    if (manageQuizzesButton != null) {
                        manageQuizzesButton.setVisible(false);
                        manageQuizzesButton.setManaged(false);
                        manageQuizzesButton.setDisable(true);
                    }
                    if (manageExamsButton != null) {
                        manageExamsButton.setVisible(false);
                        manageExamsButton.setManaged(false);
                        manageExamsButton.setDisable(true);
                    }
                }

                // Show Staff Directory for all logged-in users
                if (staffDirectoryButton != null) {
                    staffDirectoryButton.setVisible(true);
                    staffDirectoryButton.setManaged(true);
                    staffDirectoryButton.setDisable(false);
                }
                // Show view assigned courses button for staff (US 3.3.2)
                if ("STAFF".equals(userType)) {
                    if (viewAssignedCoursesButton != null) {
                        viewAssignedCoursesButton.setVisible(true);
                        viewAssignedCoursesButton.setManaged(true);
                        viewAssignedCoursesButton.setDisable(false);
                    }
                } else {
                    if (viewAssignedCoursesButton != null) {
                        viewAssignedCoursesButton.setVisible(false);
                        viewAssignedCoursesButton.setManaged(false);
                        viewAssignedCoursesButton.setDisable(true);
                    }
                }
                // Hide Staff Management for non-admins
                if (manageStaffButton != null) {
                    manageStaffButton.setVisible(false);
                    manageStaffButton.setManaged(false);
                    manageStaffButton.setDisable(true);
                }
                // Show performance and research buttons for STAFF
                if ("STAFF".equals(userType)) {
                    if (viewPerformanceButton != null) {
                        viewPerformanceButton.setVisible(true);
                        viewPerformanceButton.setManaged(true);
                        viewPerformanceButton.setDisable(false);
                    }
                    if (addResearchButton != null) {
                        addResearchButton.setVisible(true);
                        addResearchButton.setManaged(true);
                        addResearchButton.setDisable(false);
                    }
                    // Show leave management buttons for STAFF
                    if (submitLeaveRequestButton != null) {
                        submitLeaveRequestButton.setVisible(true);
                        submitLeaveRequestButton.setManaged(true);
                        submitLeaveRequestButton.setDisable(false);
                    }
                    if (viewLeaveHistoryButton != null) {
                        viewLeaveHistoryButton.setVisible(true);
                        viewLeaveHistoryButton.setManaged(true);
                        viewLeaveHistoryButton.setDisable(false);
                    }
                    // Show payroll and benefits buttons for STAFF
                    if (viewPayrollButton != null) {
                        viewPayrollButton.setVisible(true);
                        viewPayrollButton.setManaged(true);
                        viewPayrollButton.setDisable(false);
                    }
                    if (viewBenefitsButton != null) {
                        viewBenefitsButton.setVisible(true);
                        viewBenefitsButton.setManaged(true);
                        viewBenefitsButton.setDisable(false);
                    }
                } else {
                    if (viewPerformanceButton != null) {
                        viewPerformanceButton.setVisible(false);
                        viewPerformanceButton.setManaged(false);
                        viewPerformanceButton.setDisable(true);
                    }
                    if (addResearchButton != null) {
                        addResearchButton.setVisible(false);
                        addResearchButton.setManaged(false);
                        addResearchButton.setDisable(true);
                    }
                    // Hide leave management buttons for non-staff
                    if (submitLeaveRequestButton != null) {
                        submitLeaveRequestButton.setVisible(false);
                        submitLeaveRequestButton.setManaged(false);
                        submitLeaveRequestButton.setDisable(true);
                    }
                    if (viewLeaveHistoryButton != null) {
                        viewLeaveHistoryButton.setVisible(false);
                        viewLeaveHistoryButton.setManaged(false);
                        viewLeaveHistoryButton.setDisable(true);
                    }
                    // Hide payroll and benefits buttons for non-staff
                    if (viewPayrollButton != null) {
                        viewPayrollButton.setVisible(false);
                        viewPayrollButton.setManaged(false);
                        viewPayrollButton.setDisable(true);
                    }
                    if (viewBenefitsButton != null) {
                        viewBenefitsButton.setVisible(false);
                        viewBenefitsButton.setManaged(false);
                        viewBenefitsButton.setDisable(true);
                    }
                }
                // Hide admin-only buttons for non-admins
                if (recordPerformanceButton != null) {
                    recordPerformanceButton.setVisible(false);
                    recordPerformanceButton.setManaged(false);
                    recordPerformanceButton.setDisable(true);
                }
                if (reviewResearchButton != null) {
                    reviewResearchButton.setVisible(false);
                    reviewResearchButton.setManaged(false);
                    reviewResearchButton.setDisable(true);
                }
                if (approveLeaveButton != null) {
                    approveLeaveButton.setVisible(false);
                    approveLeaveButton.setManaged(false);
                    approveLeaveButton.setDisable(true);
                }
                // Hide assign staff button for non-admins
                if (assignStaffToCourseButton != null) {
                    assignStaffToCourseButton.setVisible(false);
                    assignStaffToCourseButton.setManaged(false);
                    assignStaffToCourseButton.setDisable(true);
                }
            }

            // Messages button is visible for all logged-in users
            if (messagesButton != null) {
                messagesButton.setVisible(true);
                messagesButton.setManaged(true);
                messagesButton.setDisable(false);
            }
            // Announcements button is visible for all logged-in users (US 4.4)
            if (viewAnnouncementsButton != null) {
                viewAnnouncementsButton.setVisible(true);
                viewAnnouncementsButton.setManaged(true);
                viewAnnouncementsButton.setDisable(false);
            }
            // Create Announcement button is visible for Admin/Staff (US 3.1)
            if (createAnnouncementButton != null) {
                boolean isAdminOrStaff = "ADMIN".equals(userType) || "STAFF".equals(userType);
                createAnnouncementButton.setVisible(isAdminOrStaff);
                createAnnouncementButton.setManaged(isAdminOrStaff);
                createAnnouncementButton.setDisable(!isAdminOrStaff);
            }
            // Show parent-specific buttons for PARENT user type
            if ("PARENT".equals(userType)) {
                if (parentSendMessageButton != null) {
                    parentSendMessageButton.setVisible(true);
                    parentSendMessageButton.setManaged(true);
                    parentSendMessageButton.setDisable(false);
                }
                if (linkChildButton != null) {
                    linkChildButton.setVisible(true);
                    linkChildButton.setManaged(true);
                    linkChildButton.setDisable(false);
                }
                if (messageThreadHistoryButton != null) {
                    messageThreadHistoryButton.setVisible(true);
                    messageThreadHistoryButton.setManaged(true);
                    messageThreadHistoryButton.setDisable(false);
                }
                if (forumButton != null) {
                    forumButton.setVisible(true);
                    forumButton.setManaged(true);
                    forumButton.setDisable(false);
                }
                if (eventsCalendarButton != null) {
                    eventsCalendarButton.setVisible(true);
                    eventsCalendarButton.setManaged(true);
                    eventsCalendarButton.setDisable(false);
                }
            } else if (!"ADMIN".equals(userType)) {
                // Hide parent buttons for non-parents (Admin already handled above)
                if (parentSendMessageButton != null && !parentSendMessageButton.isVisible()) {
                    parentSendMessageButton.setVisible(false);
                    parentSendMessageButton.setManaged(false);
                    parentSendMessageButton.setDisable(true);
                }
                if (linkChildButton != null && !linkChildButton.isVisible()) {
                    linkChildButton.setVisible(false);
                    linkChildButton.setManaged(false);
                    linkChildButton.setDisable(true);
                }
            }
        } else {
            userIdLabel.setText("Guest");

            // Show login/register buttons, hide logout
            if (logoutButton != null)
                logoutButton.setVisible(false);
            if (loginButton != null)
                loginButton.setVisible(true);
            if (registerButton != null)
                registerButton.setVisible(true);

            // Disable feature buttons
            if (roomsButton != null)
                roomsButton.setDisable(true);
            if (reportButton != null)
                reportButton.setDisable(true);
            if (viewMyTicketsButton != null) {
                viewMyTicketsButton.setVisible(false);
                viewMyTicketsButton.setManaged(false);
            }
            if (bookRoomButton != null) {
                bookRoomButton.setVisible(false);
                bookRoomButton.setManaged(false);
                bookRoomButton.setDisable(true);
            }
            if (viewMyBookingsButton != null) {
                viewMyBookingsButton.setVisible(false);
                viewMyBookingsButton.setManaged(false);
                viewMyBookingsButton.setDisable(true);
            }
            if (viewEquipmentButton != null) {
                viewEquipmentButton.setVisible(false);
                viewEquipmentButton.setManaged(false);
                viewEquipmentButton.setDisable(true);
            }
            if (myAllocatedEquipmentButton != null) {
                myAllocatedEquipmentButton.setVisible(false);
                myAllocatedEquipmentButton.setManaged(false);
                myAllocatedEquipmentButton.setDisable(true);
            }
            if (allocateEquipmentButton != null) {
                allocateEquipmentButton.setVisible(false);
                allocateEquipmentButton.setManaged(false);
                allocateEquipmentButton.setDisable(true);
            }
            if (addEquipmentButton != null) {
                addEquipmentButton.setVisible(false);
                addEquipmentButton.setManaged(false);
                addEquipmentButton.setDisable(true);
            }
            if (softwareLicensesButton != null) {
                softwareLicensesButton.setVisible(false);
                softwareLicensesButton.setManaged(false);
                softwareLicensesButton.setDisable(true);
            }
            if (admissionApplicationsButton != null) {
                admissionApplicationsButton.setVisible(false);
                admissionApplicationsButton.setManaged(false);
                admissionApplicationsButton.setDisable(true);
            }
            if (studentRecordsButton != null) {
                studentRecordsButton.setVisible(false);
                studentRecordsButton.setManaged(false);
                studentRecordsButton.setDisable(true);
            }
            if (myTranscriptsButton != null) {
                myTranscriptsButton.setVisible(false);
                myTranscriptsButton.setManaged(false);
                myTranscriptsButton.setDisable(true);
            }
            if (requestTranscriptButton != null) {
                requestTranscriptButton.setVisible(false);
                requestTranscriptButton.setManaged(false);
                requestTranscriptButton.setDisable(true);
            }
            if (transcriptRequestsButton != null) {
                transcriptRequestsButton.setVisible(false);
                transcriptRequestsButton.setManaged(false);
                transcriptRequestsButton.setDisable(true);
            }
            if (manageCoursesButton != null) {
                manageCoursesButton.setVisible(false);
                manageCoursesButton.setManaged(false);
                manageCoursesButton.setDisable(true);
            }
            if (assignCoursesButton != null) {
                assignCoursesButton.setVisible(false);
                assignCoursesButton.setManaged(false);
                assignCoursesButton.setDisable(true);
            }
            if (viewAnnouncementsButton != null) {
                viewAnnouncementsButton.setVisible(false);
                viewAnnouncementsButton.setManaged(false);
                viewAnnouncementsButton.setDisable(true);
            }
            if (createAnnouncementButton != null) {
                createAnnouncementButton.setVisible(false);
                createAnnouncementButton.setManaged(false);
                createAnnouncementButton.setDisable(true);
            }
            if (assignStaffToCourseButton != null) {
                assignStaffToCourseButton.setVisible(false);
                assignStaffToCourseButton.setManaged(false);
                assignStaffToCourseButton.setDisable(true);
            }
            if (viewAssignedCoursesButton != null) {
                viewAssignedCoursesButton.setVisible(false);
                viewAssignedCoursesButton.setManaged(false);
                viewAssignedCoursesButton.setDisable(true);
            }
            if (viewCourseCatalogButton != null) {
                viewCourseCatalogButton.setVisible(false);
                viewCourseCatalogButton.setManaged(false);
                viewCourseCatalogButton.setDisable(true);
            }
            if (enrollInCoursesButton != null) {
                enrollInCoursesButton.setVisible(false);
                enrollInCoursesButton.setManaged(false);
                enrollInCoursesButton.setDisable(true);
            }
            if (myEnrolledCoursesButton != null) {
                myEnrolledCoursesButton.setVisible(false);
                myEnrolledCoursesButton.setManaged(false);
                myEnrolledCoursesButton.setDisable(true);
            }
            if (myAssignmentsButton != null) {
                myAssignmentsButton.setVisible(false);
                myAssignmentsButton.setManaged(false);
                myAssignmentsButton.setDisable(true);
            }
            if (manageAssignmentsButton != null) {
                manageAssignmentsButton.setVisible(false);
                manageAssignmentsButton.setManaged(false);
                manageAssignmentsButton.setDisable(true);
            }
            if (viewGradebookButton != null) {
                viewGradebookButton.setVisible(false);
                viewGradebookButton.setManaged(false);
                viewGradebookButton.setDisable(true);
            }
            if (calculateFinalGradesButton != null) {
                calculateFinalGradesButton.setVisible(false);
                calculateFinalGradesButton.setManaged(false);
                calculateFinalGradesButton.setDisable(true);
            }
            if (myQuizzesButton != null) {
                myQuizzesButton.setVisible(false);
                myQuizzesButton.setManaged(false);
                myQuizzesButton.setDisable(true);
            }
            if (myExamsButton != null) {
                myExamsButton.setVisible(false);
                myExamsButton.setManaged(false);
                myExamsButton.setDisable(true);
            }
            if (courseGradesButton != null) {
                courseGradesButton.setVisible(false);
                courseGradesButton.setManaged(false);
                courseGradesButton.setDisable(true);
            }
            if (manageQuizzesButton != null) {
                manageQuizzesButton.setVisible(false);
                manageQuizzesButton.setManaged(false);
                manageQuizzesButton.setDisable(true);
            }
            if (manageExamsButton != null) {
                manageExamsButton.setVisible(false);
                manageExamsButton.setManaged(false);
                manageExamsButton.setDisable(true);
            }
            // Hide Staff buttons for Guests
            if (staffDirectoryButton != null) {
                staffDirectoryButton.setVisible(false);
                staffDirectoryButton.setManaged(false);
                staffDirectoryButton.setDisable(true);
            }
            if (manageStaffButton != null) {
                manageStaffButton.setVisible(false);
                manageStaffButton.setManaged(false);
                manageStaffButton.setDisable(true);
            }
            // Hide performance and research buttons for Guests
            if (recordPerformanceButton != null) {
                recordPerformanceButton.setVisible(false);
                recordPerformanceButton.setManaged(false);
                recordPerformanceButton.setDisable(true);
            }
            if (viewPerformanceButton != null) {
                viewPerformanceButton.setVisible(false);
                viewPerformanceButton.setManaged(false);
                viewPerformanceButton.setDisable(true);
            }
            if (addResearchButton != null) {
                addResearchButton.setVisible(false);
                addResearchButton.setManaged(false);
                addResearchButton.setDisable(true);
            }
            if (reviewResearchButton != null) {
                reviewResearchButton.setVisible(false);
                reviewResearchButton.setManaged(false);
                reviewResearchButton.setDisable(true);
            }
            // Hide leave management buttons for Guests
            if (submitLeaveRequestButton != null) {
                submitLeaveRequestButton.setVisible(false);
                submitLeaveRequestButton.setManaged(false);
                submitLeaveRequestButton.setDisable(true);
            }
            if (approveLeaveButton != null) {
                approveLeaveButton.setVisible(false);
                approveLeaveButton.setManaged(false);
                approveLeaveButton.setDisable(true);
            }
            if (viewLeaveHistoryButton != null) {
                viewLeaveHistoryButton.setVisible(false);
                viewLeaveHistoryButton.setManaged(false);
                viewLeaveHistoryButton.setDisable(true);
            }
            // Hide payroll and benefits buttons for Guests
            if (viewPayrollButton != null) {
                viewPayrollButton.setVisible(false);
                viewPayrollButton.setManaged(false);
                viewPayrollButton.setDisable(true);
            }
            if (viewBenefitsButton != null) {
                viewBenefitsButton.setVisible(false);
                viewBenefitsButton.setManaged(false);
                viewBenefitsButton.setDisable(true);
            }
            // Hide communication buttons for Guests
            if (linkChildButton != null) {
                linkChildButton.setVisible(false);
                linkChildButton.setManaged(false);
                linkChildButton.setDisable(true);
            }
            if (parentSendMessageButton != null) {
                parentSendMessageButton.setVisible(false);
                parentSendMessageButton.setManaged(false);
                parentSendMessageButton.setDisable(true);
            }
            if (messageThreadHistoryButton != null) {
                messageThreadHistoryButton.setVisible(false);
                messageThreadHistoryButton.setManaged(false);
                messageThreadHistoryButton.setDisable(true);
            }
            if (studentSendMessageButton != null) {
                studentSendMessageButton.setVisible(false);
                studentSendMessageButton.setManaged(false);
                studentSendMessageButton.setDisable(true);
            }
            if (forumButton != null) {
                forumButton.setVisible(false);
                forumButton.setManaged(false);
                forumButton.setDisable(true);
            }
            if (createEventButton != null) {
                createEventButton.setVisible(false);
                createEventButton.setManaged(false);
                createEventButton.setDisable(true);
            }
            if (eventsCalendarButton != null) {
                eventsCalendarButton.setVisible(false);
                eventsCalendarButton.setManaged(false);
                eventsCalendarButton.setDisable(true);
            }
            if (managePayrollButton != null) {
                managePayrollButton.setVisible(false);
                managePayrollButton.setManaged(false);
                managePayrollButton.setDisable(true);
            }
            if (manageBenefitsButton != null) {
                manageBenefitsButton.setVisible(false);
                manageBenefitsButton.setManaged(false);
                manageBenefitsButton.setDisable(true);
            }

            // Hide messages button for guest
            if (messagesButton != null) {
                messagesButton.setVisible(false);
                messagesButton.setManaged(false);
                messagesButton.setDisable(true);
            }
        }
    }

    private void updateUnreadCount() {
        if (authService.isLoggedIn() && messagesButton != null) {
            try {
                int count = messageService.getUnreadCount(Integer.parseInt(authService.getCurrentUser().getId()));
                if (count > 0) {
                    messagesButton.setText("Messages (" + count + ")");
                } else {
                    messagesButton.setText("Messages");
                }
            } catch (SQLException | NumberFormatException e) {
                messagesButton.setText("Messages");
            }
        }
    }

    @FXML
    void handleViewMyTickets(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your tickets.");
            alert.showAndWait();
            return;
        }

        // Only allow Students, Professors, and Staff to view their tickets
        // Admins should use "View All Tickets" instead
        String userType = authService.getCurrentUserType();
        if ("ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText(
                    "Administrators cannot view individual tickets. Please use 'View All Tickets' to see all tickets.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/tickets_view.fxml", event, "My Tickets");
    }

    @FXML
    private VBox extraButtonsContainer;

    @FXML
    private Button reportButton;

    @FXML
    private Button roomsButton;

    @FXML
    private Button assignCoursesButton;

    @FXML
    private Button viewAnnouncementsButton;

    @FXML
    private Button createAnnouncementButton;

    @FXML
    private Button assignStaffToCourseButton;

    @FXML
    private Button viewAssignedCoursesButton;

    @FXML
    private Button viewMyTicketsButton;

    @FXML
    private Button bookRoomButton;

    @FXML
    private Button viewMyBookingsButton;

    @FXML
    private Button viewEquipmentButton;

    @FXML
    private Button myAllocatedEquipmentButton;

    @FXML
    private Button allocateEquipmentButton;

    @FXML
    private Button addEquipmentButton;

    @FXML
    private Button softwareLicensesButton;

    @FXML
    private Button admissionApplicationsButton;

    @FXML
    private Button studentRecordsButton;

    @FXML
    private Button myTranscriptsButton;

    @FXML
    private Button transcriptRequestsButton;

    @FXML
    private Button manageCoursesButton; // Admin - Manage Course Catalog (US 2.1)
    @FXML
    private Button viewCourseCatalogButton; // Student - View Course Catalog (US 2.2)
    @FXML
    private Button enrollInCoursesButton; // Student - Enroll in Courses (US 2.3)
    @FXML
    private Button myEnrolledCoursesButton; // Student - View My Enrolled Courses (US 2.4)

    @FXML
    private Button myAssignmentsButton; // Student - My Assignments (US 2.8)
    @FXML
    private Button manageAssignmentsButton; // Professor - Manage Assignments (US 2.7)
    @FXML
    private Button viewGradebookButton; // Professor - View Gradebook (US 2.9)
    @FXML
    private Button calculateFinalGradesButton; // Professor - Calculate Final Grades
    @FXML
    private Button requestTranscriptButton; // Student - Request Transcript
    @FXML
    private Button myQuizzesButton; // Student - My Quizzes (US 2.11)
    @FXML
    private Button myExamsButton; // Student - My Exams
    @FXML
    private Button courseGradesButton; // Student - View Course Grades
    @FXML
    private Button manageQuizzesButton; // Professor - Manage Quizzes (US 2.10)
    @FXML
    private Button manageExamsButton; // Professor - Manage Exams (US 2.12, 2.13)

    @FXML
    private Button staffDirectoryButton;
    @FXML
    private Button manageStaffButton;
    @FXML
    private Button messagesButton;
    @FXML
    private Button recordPerformanceButton; // Admin - Record Performance Evaluation (US 3.7)
    @FXML
    private Button viewPerformanceButton; // Staff - View Performance Evaluation (US 3.8)
    @FXML
    private Button addResearchButton; // Staff - Add Research Activity (US 3.9)
    @FXML
    private Button reviewResearchButton; // Admin - Review Research Records (US 3.10)
    @FXML
    private Button submitLeaveRequestButton; // Staff - Submit Leave Request (US 3.11)
    @FXML
    private Button approveLeaveButton; // Admin - Approve/Reject Leave (US 3.12)
    @FXML
    private Button viewLeaveHistoryButton; // Staff - View Leave History (US 3.13)
    @FXML
    private Button viewPayrollButton; // Staff - View Payroll Information (US 3.16)
    @FXML
    private Button viewBenefitsButton; // Staff - View Benefits Information (US 3.17)
    @FXML
    private Button managePayrollButton; // Admin - Manage Payroll Information (US 3.14)
    @FXML
    private Button manageBenefitsButton; // Admin - Manage Benefits Information (US 3.15)
    @FXML
    private Button parentSendMessageButton; // Parent - Send Message to Teacher (US 4.1)
    @FXML
    private Button linkChildButton; // Parent - Link My Child
    @FXML
    private Button messageThreadHistoryButton; // Parent/Teacher - View Thread History (US 4.3)
    @FXML
    private Button studentSendMessageButton; // Student - Send Message to Staff (US 4.4)
    @FXML
    private Button forumButton; // Student/Staff - Forum (US 4.6, 4.7)
    @FXML
    private Button createEventButton; // Admin - Create Event (US 4.11)
    @FXML
    private Button eventsCalendarButton; // All - View Events Calendar (US 4.12)

    @FXML
    private Button logoutButton;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label userIdLabel;

    @FXML
    void handleReport(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to access this feature.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if ("ADMIN".equals(userType)) {
            // Admins can only view and assign tickets
            navigateTo("/fxml/tickets_view.fxml", event, "Maintenance Tickets");
        } else {
            // Students, Staff, and Professors can create tickets
            navigateTo("/fxml/maintenanceTicket.fxml", event, "Report Maintenance Issue");
        }
    }

    @FXML
    void handleRooms(ActionEvent event) {
        navigateTo("/fxml/rooms.fxml", event, "Room Management");
    }

    @FXML
    void handleBookRoom(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to book a room.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors and staff can book rooms.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/booking.fxml", event, "Book a Room");
    }

    @FXML
    void handleMessages(ActionEvent event) {
        navigateTo("/fxml/messages-inbox.fxml", event, "Messages Inbox");
    }

    @FXML
    void handleViewMyBookings(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your bookings.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors and staff can view bookings.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/my_bookings.fxml", event, "My Bookings");
    }

    @FXML
    void handleViewEquipment(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view equipment.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors and staff can view equipment.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/view_equipment.fxml", event, "View Equipment");
    }

    @FXML
    void handleStaffDirectory(ActionEvent event) {
        navigateTo("/fxml/staff_directory.fxml", event, "Staff Directory");
    }

    @FXML
    void handleManageStaff(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to access this feature.");
            alert.showAndWait();
            return;
        }
        if (!"ADMIN".equals(authService.getCurrentUserType())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can manage staff profiles.");
            alert.showAndWait();
            return;
        }
        navigateTo("/edu/staff/ui/manage_staff_profiles.fxml", event, "Manage Staff Profiles");
    }

    @FXML
    void handleMyAllocatedEquipment(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your allocated equipment.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType) && !"STAFF".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors and staff can view allocated equipment.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/my_allocated_equipment.fxml", event, "My Allocated Equipment");
    }

    @FXML
    void handleAllocateEquipment(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to allocate equipment.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can allocate equipment.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/allocate_equipment.fxml", event, "Allocate Equipment");
    }

    @FXML
    void handleAddEquipment(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to add equipment.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can add equipment.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/add_equipment.fxml", event, "Add Equipment");
    }

    @FXML
    void handleSoftwareLicenses(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view software licenses.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can view software licenses.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/software_licenses.fxml", event, "Software Licenses");
    }

    @FXML
    void handleAdmissionApplications(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to manage admission applications.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can manage admission applications.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/admission_applications.fxml", event, "Admission Applications");
    }

    @FXML
    void handleStudentRecords(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to manage student records.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can manage student records.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/student_records.fxml", event, "Student Records");
    }

    @FXML
    void handleMyTranscripts(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your transcript.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only students can view their transcript.");
            alert.showAndWait();
            return;
        }

        // Navigate to transcript view (academic record)
        navigateTo("/fxml/view_transcript.fxml", event, "View Transcript");
    }

    @FXML
    void handleRequestTranscript(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to request a transcript.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only students can request transcripts.");
            alert.showAndWait();
            return;
        }

        // Navigate to transcript request page
        navigateTo("/fxml/student_transcript.fxml", event, "Request Transcript");
    }

    @FXML
    void handleTranscriptRequests(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to manage transcript requests.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can manage transcript requests.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/admin_transcript.fxml", event, "Transcript Request Management");
    }

    @FXML
    void handleMyAssignments(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your assignments.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only students can view their assignments.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/student_assignments.fxml", event, "My Assignments");
    }

    @FXML
    void handleManageAssignments(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to manage assignments.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors can manage assignments.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/professor_assignments.fxml", event, "Assignment Management");
    }

    @FXML
    void handleViewGradebook(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view gradebook.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors can view gradebook.");
            alert.showAndWait();
            return;
        }

        // Navigate to dedicated gradebook page
        System.out.println("Navigating to Gradebook page");
        navigateTo("/fxml/gradebook.fxml", event, "Gradebook");
    }

    @FXML
    void handleCalculateFinalGrades(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to calculate final grades.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors can calculate final grades.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/calculate_final_grades.fxml", event, "Calculate Final Grades");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        authService.logout();
        // Navigate to login page after logout
        navigateTo("/fxml/login.fxml", event, "Login");
    }

    @FXML
    void handleLogin(ActionEvent event) {
        navigateTo("/fxml/login.fxml", event, "Login");
    }

    @FXML
    void handleRegister(ActionEvent event) {
        navigateTo("/fxml/register.fxml", event, "Register");
    }

    @FXML
    void handleManageCourses(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to manage courses.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can manage courses.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/admin_courses.fxml", event, "Manage Course Catalog");
    }

    @FXML
    void handleViewCourseCatalog(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view the course catalog.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only students can view the course catalog.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/student_course_catalog.fxml", event, "Course Catalog");
    }

    @FXML
    void handleEnrollInCourses(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to enroll in courses.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only students can enroll in courses.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/student_enrollment.fxml", event, "Enroll in Courses");
    }

    @FXML
    void handleMyEnrolledCourses(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your enrolled courses.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only students can view enrolled courses.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/student_my_courses.fxml", event, "My Enrolled Courses");
    }

    @FXML
    void handleAssignCourses(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to assign courses.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can assign courses to professors.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/assign_courses.fxml", event, "Assign Courses to Professors");
    }

    @FXML
    void handleViewAnnouncements(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view announcements.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/view_announcements.fxml", event, "Announcements");
    }

    @FXML
    void handleCreateAnnouncement(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to create announcements.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType) && !"STAFF".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators and staff can create announcements.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/create_announcement.fxml", event, "Create Announcement");
    }

    @FXML
    void handleAssignStaffToCourse(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to assign staff to courses.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only academic administrators can assign staff to courses.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/assign_staff_to_course.fxml", event, "Assign Staff to Courses");
    }

    @FXML
    void handleViewAssignedCourses(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your assigned courses.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only staff members can view assigned courses.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/view_assigned_courses.fxml", event, "My Assigned Courses");
    }

    @FXML
    void handleMyQuizzes(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your quizzes.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only students can view their quizzes.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/student_quizzes.fxml", event, "My Quizzes");
    }

    @FXML
    void handleManageQuizzes(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to manage quizzes.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors can manage quizzes.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/professor_quizzes.fxml", event, "Quiz Management");
    }

    @FXML
    void handleManageExams(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to manage exams.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"PROFESSOR".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only professors can manage exams.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/professor_exams.fxml", event, "Exam Management");
    }

    @FXML
    void handleMyExams(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your exams.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only students can view their exams.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/student_exams.fxml", event, "My Exams");
    }

    @FXML
    void handleCourseGrades(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view course grades.");
            alert.showAndWait();
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only students can view course grades.");
            alert.showAndWait();
            return;
        }

        navigateTo("/fxml/student_course_grades.fxml", event, "Course Grades");
    }

    private void navigateTo(String fxmlPath, ActionEvent event, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            // Preserve maximized state
            boolean wasMaximized = stage.isMaximized();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            // Always keep window maximized
            stage.setMaximized(true);
            stage.show();
            // Double-check maximized state after showing to prevent resize
            javafx.application.Platform.runLater(() -> {
                if (wasMaximized) {
                    stage.setMaximized(true);
                }
            });
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to open view");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleMessagesButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/messages-inbox.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Messages");
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open Messages window.");
        }
    }

    @FXML
    void handleRecordPerformanceEvaluation(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to access this feature.");
            alert.showAndWait();
            return;
        }
        if (!"ADMIN".equals(authService.getCurrentUserType())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can record performance evaluations.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/record_performance_evaluation.fxml", event, "Record Performance Evaluation");
    }

    @FXML
    void handleViewPerformanceEvaluation(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view your performance evaluations.");
            alert.showAndWait();
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only staff members can view their performance evaluations.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/view_performance_evaluation.fxml", event, "My Performance Evaluations");
    }

    @FXML
    void handleAddResearchActivity(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to add research activities.");
            alert.showAndWait();
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only staff members can add research activities.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/add_research_activity.fxml", event, "Add Research Activity");
    }

    @FXML
    void handleReviewResearchRecords(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to review research records.");
            alert.showAndWait();
            return;
        }
        if (!"ADMIN".equals(authService.getCurrentUserType())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only administrators can review research records.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/review_research_records.fxml", event, "Review Research Records");
    }

    @FXML
    void handleSubmitLeaveRequest(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to submit leave requests.");
            alert.showAndWait();
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType) && !"PROFESSOR".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only staff members can submit leave requests.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/submit_leave_request.fxml", event, "Submit Leave Request");
    }

    @FXML
    void handleApproveLeave(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to manage leave requests.");
            alert.showAndWait();
            return;
        }
        if (!"ADMIN".equals(authService.getCurrentUserType())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only HR administrators can approve/reject leave requests.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/approve_leave.fxml", event, "Approve/Reject Leave Requests");
    }

    @FXML
    void handleViewLeaveHistory(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view leave history.");
            alert.showAndWait();
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType) && !"PROFESSOR".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only staff members can view their leave history.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/view_leave_history.fxml", event, "My Leave History");
    }

    @FXML
    void handleViewPayroll(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view payroll information.");
            alert.showAndWait();
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType) && !"PROFESSOR".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only staff members can view payroll information.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/view_payroll.fxml", event, "My Payroll Information");
    }

    @FXML
    void handleViewBenefits(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to view benefits information.");
            alert.showAndWait();
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType) && !"PROFESSOR".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only staff members can view benefits information.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/view_benefits.fxml", event, "My Benefits Information");
    }

    @FXML
    void handleManagePayroll(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to manage payroll information.");
            alert.showAndWait();
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only HR administrators can manage payroll information.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/manage_payroll.fxml", event, "Manage Payroll Information");
    }

    @FXML
    void handleManageBenefits(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Not Logged In");
            alert.setHeaderText(null);
            alert.setContentText("Please login to manage benefits information.");
            alert.showAndWait();
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.setContentText("Only HR administrators can manage benefits information.");
            alert.showAndWait();
            return;
        }
        navigateTo("/fxml/manage_benefits.fxml", event, "Manage Benefits Information");
    }

    @FXML
    void handleLinkChild(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            showAlert("Not Logged In", "Please login to link your child.");
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"PARENT".equals(userType)) {
            showAlert("Access Denied", "Only parents can link students.");
            return;
        }
        navigateTo("/fxml/link_child.fxml", event, "Link My Child");
    }

    @FXML
    void handleParentSendMessage(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            showAlert("Not Logged In", "Please login to send messages to teachers.");
            return;
        }
        navigateTo("/fxml/parent_send_message.fxml", event, "Send Message to Teacher");
    }

    @FXML
    void handleMessageThreadHistory(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            showAlert("Not Logged In", "Please login to view message history.");
            return;
        }
        navigateTo("/fxml/message_thread_history.fxml", event, "Message Thread History");
    }

    @FXML
    void handleStudentSendMessage(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            showAlert("Not Logged In", "Please login to send messages.");
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"STUDENT".equals(userType)) {
            showAlert("Access Denied", "Only students can use this feature.");
            return;
        }
        navigateTo("/fxml/student_send_message.fxml", event, "Send Message to Staff");
    }

    @FXML
    void handleForum(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            showAlert("Not Logged In", "Please login to access the forum.");
            return;
        }
        navigateTo("/fxml/forum.fxml", event, "Forum");
    }

    @FXML
    void handleCreateEvent(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            showAlert("Not Logged In", "Please login to create events.");
            return;
        }
        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showAlert("Access Denied", "Only administrators can create events.");
            return;
        }
        navigateTo("/fxml/create_event.fxml", event, "Create Event");
    }

    @FXML
    void handleEventsCalendar(ActionEvent event) {
        if (!authService.isLoggedIn()) {
            showAlert("Not Logged In", "Please login to view events.");
            return;
        }
        navigateTo("/fxml/events_calendar.fxml", event, "Events Calendar");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
