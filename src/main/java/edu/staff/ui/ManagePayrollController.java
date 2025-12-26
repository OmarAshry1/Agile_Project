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

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 * Controller for managing payroll information (HR Admin only)
 * US 3.14 - Add or Update Payroll Information
 */
public class ManagePayrollController implements Initializable {

    @FXML private ComboBox<User> staffComboBox;
    @FXML private TableView<PayrollInformation> payrollTable;
    @FXML private TableColumn<PayrollInformation, String> payPeriodColumn;
    @FXML private TableColumn<PayrollInformation, String> payDateColumn;
    @FXML private TableColumn<PayrollInformation, String> grossPayColumn;
    @FXML private TableColumn<PayrollInformation, String> netPayColumn;
    
    @FXML private DatePicker payPeriodStartPicker;
    @FXML private DatePicker payPeriodEndPicker;
    @FXML private DatePicker payDatePicker;
    @FXML private DatePicker effectiveDatePicker;
    @FXML private ComboBox<String> payFrequencyComboBox;
    @FXML private TextField baseSalaryField;
    @FXML private TextField overtimePayField;
    @FXML private TextField bonusesField;
    @FXML private TextField taxDeductionField;
    @FXML private TextField insuranceDeductionField;
    @FXML private TextField otherDeductionsField;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private TextArea notesTextArea;
    
    @FXML private Label grossPayLabel;
    @FXML private Label totalDeductionsLabel;
    @FXML private Label netPayLabel;
    
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;
    @FXML private Button clearButton;

    private PayrollService payrollService;
    private AuthService authService;
    private ObservableList<PayrollInformation> payrollList;
    private ObservableList<User> staffList;
    private PayrollInformation currentPayroll;
    private NumberFormat currencyFormat;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        payrollService = new PayrollService();
        authService = AuthService.getInstance();
        payrollList = FXCollections.observableArrayList();
        staffList = FXCollections.observableArrayList();
        currencyFormat = NumberFormat.getCurrencyInstance();

        // Check access
        if (!authService.isLoggedIn()) {
            showError("Access Denied", "Please login to manage payroll information.");
            return;
        }

        String userType = authService.getCurrentUserType();
        if (!"ADMIN".equals(userType)) {
            showError("Access Denied", "Only HR administrators can manage payroll information.");
            return;
        }

        // Initialize pay frequency combo box
        ObservableList<String> frequencies = FXCollections.observableArrayList(
            "WEEKLY", "BIWEEKLY", "MONTHLY", "QUARTERLY", "ANNUAL"
        );
        payFrequencyComboBox.setItems(frequencies);
        payFrequencyComboBox.getSelectionModel().select("MONTHLY");

        // Initialize payment method combo box
        ObservableList<String> methods = FXCollections.observableArrayList(
            "DIRECT_DEPOSIT", "CHECK", "WIRE_TRANSFER"
        );
        paymentMethodComboBox.setItems(methods);
        paymentMethodComboBox.getSelectionModel().select("DIRECT_DEPOSIT");

        // Initialize staff combo box
        staffComboBox.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + " (ID: " + item.getId() + ")");
                }
            }
        });
        staffComboBox.setButtonCell(new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + " (ID: " + item.getId() + ")");
                }
            }
        });
        staffComboBox.setOnAction(e -> loadPayrollForStaff());

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

        payrollTable.setItems(payrollList);
        payrollTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    populateForm(newSelection);
                    currentPayroll = newSelection;
                    updateButton.setDisable(false);
                    deleteButton.setDisable(false);
                }
            }
        );

        // Add listeners to calculate totals
        baseSalaryField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotals());
        overtimePayField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotals());
        bonusesField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotals());
        taxDeductionField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotals());
        insuranceDeductionField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotals());
        otherDeductionsField.textProperty().addListener((obs, oldVal, newVal) -> calculateTotals());

        loadStaffList();
        clearForm();
    }

    private void loadStaffList() {
        try {
            staffList.clear();
            staffList.addAll(payrollService.getAllStaff());
            staffComboBox.setItems(staffList);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load staff list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadPayrollForStaff() {
        User selectedStaff = staffComboBox.getValue();
        if (selectedStaff == null) {
            payrollList.clear();
            return;
        }

        try {
            int staffUserID = Integer.parseInt(selectedStaff.getId());
            payrollList.clear();
            payrollList.addAll(payrollService.getPayrollByStaff(staffUserID));
        } catch (SQLException e) {
            showError("Database Error", "Failed to load payroll: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void calculateTotals() {
        try {
            BigDecimal baseSalary = parseDecimal(baseSalaryField.getText());
            BigDecimal overtimePay = parseDecimal(overtimePayField.getText());
            BigDecimal bonuses = parseDecimal(bonusesField.getText());
            BigDecimal taxDeduction = parseDecimal(taxDeductionField.getText());
            BigDecimal insuranceDeduction = parseDecimal(insuranceDeductionField.getText());
            BigDecimal otherDeductions = parseDecimal(otherDeductionsField.getText());

            BigDecimal grossPay = baseSalary.add(overtimePay).add(bonuses);
            BigDecimal totalDeductions = taxDeduction.add(insuranceDeduction).add(otherDeductions);
            BigDecimal netPay = grossPay.subtract(totalDeductions);

            grossPayLabel.setText("Gross Pay: " + currencyFormat.format(grossPay));
            totalDeductionsLabel.setText("Total Deductions: " + currencyFormat.format(totalDeductions));
            netPayLabel.setText("Net Pay: " + currencyFormat.format(netPay));
        } catch (Exception e) {
            grossPayLabel.setText("Gross Pay: -");
            totalDeductionsLabel.setText("Total Deductions: -");
            netPayLabel.setText("Net Pay: -");
        }
    }

    private BigDecimal parseDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void populateForm(PayrollInformation payroll) {
        if (payroll == null) {
            clearForm();
            return;
        }

        payPeriodStartPicker.setValue(payroll.getPayPeriodStart());
        payPeriodEndPicker.setValue(payroll.getPayPeriodEnd());
        payDatePicker.setValue(payroll.getPayDate());
        effectiveDatePicker.setValue(payroll.getEffectiveDate());
        payFrequencyComboBox.setValue(payroll.getPayFrequency());
        baseSalaryField.setText(payroll.getBaseSalary().toString());
        overtimePayField.setText(payroll.getOvertimePay().toString());
        bonusesField.setText(payroll.getBonuses().toString());
        taxDeductionField.setText(payroll.getTaxDeduction().toString());
        insuranceDeductionField.setText(payroll.getInsuranceDeduction().toString());
        otherDeductionsField.setText(payroll.getOtherDeductions().toString());
        paymentMethodComboBox.setValue(payroll.getPaymentMethod());
        notesTextArea.setText(payroll.getNotes() != null ? payroll.getNotes() : "");

        calculateTotals();
    }

    @FXML
    private void handleAdd() {
        try {
            // Validate inputs
            if (staffComboBox.getValue() == null) {
                showError("Validation Error", "Please select a staff member.");
                return;
            }

            if (payPeriodStartPicker.getValue() == null || payPeriodEndPicker.getValue() == null ||
                payDatePicker.getValue() == null || effectiveDatePicker.getValue() == null) {
                showError("Validation Error", "Please fill in all required date fields.");
                return;
            }

            if (baseSalaryField.getText() == null || baseSalaryField.getText().trim().isEmpty()) {
                showError("Validation Error", "Base Salary is required.");
                return;
            }

            // Create payroll object
            PayrollInformation payroll = new PayrollInformation();
            payroll.setStaffUserID(Integer.parseInt(staffComboBox.getValue().getId()));
            payroll.setPayPeriodStart(payPeriodStartPicker.getValue());
            payroll.setPayPeriodEnd(payPeriodEndPicker.getValue());
            payroll.setPayDate(payDatePicker.getValue());
            payroll.setEffectiveDate(effectiveDatePicker.getValue());
            payroll.setPayFrequency(payFrequencyComboBox.getValue());
            payroll.setBaseSalary(parseDecimal(baseSalaryField.getText()));
            payroll.setOvertimePay(parseDecimal(overtimePayField.getText()));
            payroll.setBonuses(parseDecimal(bonusesField.getText()));
            payroll.setTaxDeduction(parseDecimal(taxDeductionField.getText()));
            payroll.setInsuranceDeduction(parseDecimal(insuranceDeductionField.getText()));
            payroll.setOtherDeductions(parseDecimal(otherDeductionsField.getText()));
            payroll.setPaymentMethod(paymentMethodComboBox.getValue());
            payroll.setNotes(notesTextArea.getText());

            // Add payroll
            User currentUser = authService.getCurrentUser();
            payrollService.addPayroll(payroll, Integer.parseInt(currentUser.getId()));

            showInfo("Success", "Payroll record added successfully.");
            loadPayrollForStaff();
            clearForm();

        } catch (Exception e) {
            showError("Error", "Failed to add payroll record: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdate() {
        if (currentPayroll == null) {
            showError("Selection Error", "Please select a payroll record to update.");
            return;
        }

        try {
            // Validate inputs
            if (payPeriodStartPicker.getValue() == null || payPeriodEndPicker.getValue() == null ||
                payDatePicker.getValue() == null || effectiveDatePicker.getValue() == null) {
                showError("Validation Error", "Please fill in all required date fields.");
                return;
            }

            if (baseSalaryField.getText() == null || baseSalaryField.getText().trim().isEmpty()) {
                showError("Validation Error", "Base Salary is required.");
                return;
            }

            // Update payroll object
            currentPayroll.setPayPeriodStart(payPeriodStartPicker.getValue());
            currentPayroll.setPayPeriodEnd(payPeriodEndPicker.getValue());
            currentPayroll.setPayDate(payDatePicker.getValue());
            currentPayroll.setEffectiveDate(effectiveDatePicker.getValue());
            currentPayroll.setPayFrequency(payFrequencyComboBox.getValue());
            currentPayroll.setBaseSalary(parseDecimal(baseSalaryField.getText()));
            currentPayroll.setOvertimePay(parseDecimal(overtimePayField.getText()));
            currentPayroll.setBonuses(parseDecimal(bonusesField.getText()));
            currentPayroll.setTaxDeduction(parseDecimal(taxDeductionField.getText()));
            currentPayroll.setInsuranceDeduction(parseDecimal(insuranceDeductionField.getText()));
            currentPayroll.setOtherDeductions(parseDecimal(otherDeductionsField.getText()));
            currentPayroll.setPaymentMethod(paymentMethodComboBox.getValue());
            currentPayroll.setNotes(notesTextArea.getText());

            // Update payroll
            User currentUser = authService.getCurrentUser();
            payrollService.updatePayroll(currentPayroll, Integer.parseInt(currentUser.getId()));

            showInfo("Success", "Payroll record updated successfully.");
            loadPayrollForStaff();
            clearForm();

        } catch (Exception e) {
            showError("Error", "Failed to update payroll record: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        if (currentPayroll == null) {
            showError("Selection Error", "Please select a payroll record to delete.");
            return;
        }

        Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Payroll Record");
        confirmDialog.setHeaderText("Delete Payroll Record?");
        confirmDialog.setContentText("Are you sure you want to delete this payroll record? This action cannot be undone.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Note: We don't have a delete method in PayrollService, so we'll skip this for now
                    // or you can add it if needed
                    showError("Not Implemented", "Delete functionality not implemented. Please contact system administrator.");
                } catch (Exception e) {
                    showError("Error", "Failed to delete payroll record: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    @FXML
    private void handleRefresh() {
        loadPayrollForStaff();
    }

    private void clearForm() {
        payPeriodStartPicker.setValue(null);
        payPeriodEndPicker.setValue(null);
        payDatePicker.setValue(null);
        effectiveDatePicker.setValue(null);
        payFrequencyComboBox.getSelectionModel().select("MONTHLY");
        baseSalaryField.clear();
        overtimePayField.clear();
        bonusesField.clear();
        taxDeductionField.clear();
        insuranceDeductionField.clear();
        otherDeductionsField.clear();
        paymentMethodComboBox.getSelectionModel().select("DIRECT_DEPOSIT");
        notesTextArea.clear();
        grossPayLabel.setText("Gross Pay: -");
        totalDeductionsLabel.setText("Total Deductions: -");
        netPayLabel.setText("Net Pay: -");
        currentPayroll = null;
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        payrollTable.getSelectionModel().clearSelection();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

