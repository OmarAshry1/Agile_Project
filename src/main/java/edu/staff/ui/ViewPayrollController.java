package edu.staff.ui;

import edu.staff.model.PayrollInformation;
import edu.staff.service.PayrollService;
import edu.facilities.model.User;
import edu.facilities.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 * Controller for viewing payroll information (Staff only)
 * US 3.14 - View Payroll Information
 */
public class ViewPayrollController implements Initializable {

    @FXML private TableView<PayrollInformation> payrollTable;
    @FXML private TableColumn<PayrollInformation, String> payPeriodColumn;
    @FXML private TableColumn<PayrollInformation, String> payDateColumn;
    @FXML private TableColumn<PayrollInformation, String> grossPayColumn;
    @FXML private TableColumn<PayrollInformation, String> deductionsColumn;
    @FXML private TableColumn<PayrollInformation, String> netPayColumn;
    @FXML private TableColumn<PayrollInformation, String> paymentMethodColumn;
    
    @FXML private Label baseSalaryLabel;
    @FXML private Label overtimePayLabel;
    @FXML private Label bonusesLabel;
    @FXML private Label grossPayLabel;
    @FXML private Label taxDeductionLabel;
    @FXML private Label insuranceDeductionLabel;
    @FXML private Label otherDeductionsLabel;
    @FXML private Label totalDeductionsLabel;
    @FXML private Label netPayLabel;
    @FXML private Label paymentMethodLabel;
    @FXML private TextArea notesTextArea;
    
    @FXML private Button refreshButton;

    private PayrollService payrollService;
    private AuthService authService;
    private ObservableList<PayrollInformation> payrollList;
    private NumberFormat currencyFormat;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        payrollService = new PayrollService();
        authService = AuthService.getInstance();
        payrollList = FXCollections.observableArrayList();
        currencyFormat = NumberFormat.getCurrencyInstance();

        // Check access
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to view payroll information.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"STAFF".equals(userType) && !"PROFESSOR".equals(userType)) {
            showError("Access Denied", "Only staff members can view payroll information.");
            return;
        }

        // Initialize table columns
        payPeriodColumn.setCellFactory(column -> new TableCell<PayrollInformation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    PayrollInformation payroll = getTableRow().getItem();
                    if (payroll.getPayPeriodStart() != null && payroll.getPayPeriodEnd() != null) {
                        setText(payroll.getPayPeriodStart() + " to " + payroll.getPayPeriodEnd());
                    } else {
                        setText("");
                    }
                }
            }
        });

        payDateColumn.setCellFactory(column -> new TableCell<PayrollInformation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    PayrollInformation payroll = getTableRow().getItem();
                    if (payroll.getPayDate() != null) {
                        setText(payroll.getPayDate().toString());
                    } else {
                        setText("");
                    }
                }
            }
        });

        grossPayColumn.setCellFactory(column -> new TableCell<PayrollInformation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    PayrollInformation payroll = getTableRow().getItem();
                    setText(currencyFormat.format(payroll.getGrossPay()));
                }
            }
        });

        deductionsColumn.setCellFactory(column -> new TableCell<PayrollInformation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    PayrollInformation payroll = getTableRow().getItem();
                    setText(currencyFormat.format(payroll.getTotalDeductions()));
                }
            }
        });

        netPayColumn.setCellFactory(column -> new TableCell<PayrollInformation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    PayrollInformation payroll = getTableRow().getItem();
                    setText(currencyFormat.format(payroll.getNetPay()));
                }
            }
        });

        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        payrollTable.setItems(payrollList);
        payrollTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    displayPayrollDetails(newSelection);
                }
            }
        );

        loadPayroll();
    }

    private void loadPayroll() {
        try {
            User currentUser = authService.getCurrentUser();
            int staffUserID = Integer.parseInt(currentUser.getId());
            
            payrollList.clear();
            payrollList.addAll(payrollService.getPayrollByStaff(staffUserID));
        } catch (SQLException e) {
            showError("Database Error", "Failed to load payroll information: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayPayrollDetails(PayrollInformation payroll) {
        if (payroll == null) {
            clearDetails();
            return;
        }

        baseSalaryLabel.setText("Base Salary: " + currencyFormat.format(payroll.getBaseSalary()));
        overtimePayLabel.setText("Overtime Pay: " + currencyFormat.format(payroll.getOvertimePay()));
        bonusesLabel.setText("Bonuses: " + currencyFormat.format(payroll.getBonuses()));
        grossPayLabel.setText("Gross Pay: " + currencyFormat.format(payroll.getGrossPay()));
        taxDeductionLabel.setText("Tax Deduction: " + currencyFormat.format(payroll.getTaxDeduction()));
        insuranceDeductionLabel.setText("Insurance Deduction: " + currencyFormat.format(payroll.getInsuranceDeduction()));
        otherDeductionsLabel.setText("Other Deductions: " + currencyFormat.format(payroll.getOtherDeductions()));
        totalDeductionsLabel.setText("Total Deductions: " + currencyFormat.format(payroll.getTotalDeductions()));
        netPayLabel.setText("Net Pay: " + currencyFormat.format(payroll.getNetPay()));
        paymentMethodLabel.setText("Payment Method: " + payroll.getPaymentMethod());
        notesTextArea.setText(payroll.getNotes() != null ? payroll.getNotes() : "");
    }

    private void clearDetails() {
        baseSalaryLabel.setText("Base Salary: -");
        overtimePayLabel.setText("Overtime Pay: -");
        bonusesLabel.setText("Bonuses: -");
        grossPayLabel.setText("Gross Pay: -");
        taxDeductionLabel.setText("Tax Deduction: -");
        insuranceDeductionLabel.setText("Insurance Deduction: -");
        otherDeductionsLabel.setText("Other Deductions: -");
        totalDeductionsLabel.setText("Total Deductions: -");
        netPayLabel.setText("Net Pay: -");
        paymentMethodLabel.setText("Payment Method: -");
        notesTextArea.clear();
    }

    @FXML
    private void handleRefresh() {
        loadPayroll();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

