package edu.facilities.ui;

import edu.facilities.model.Admin;
import edu.facilities.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private Label idError;

    @FXML
    private TextField idField;

    @FXML
    private Button loginbutton;

    @FXML
    private Label passwordError;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Hyperlink registerLink;

    @FXML
    void handleLogin(ActionEvent event) {
        clearErrors();

        boolean hasErrors = false;

        if (idField == null || idField.getText().isBlank()) {
            idError.setText("ID is required");
            idError.setVisible(true);
            hasErrors = true;
        }

        if (passwordField == null || passwordField.getText().isBlank()) {
            passwordError.setText("Password is required");
            passwordError.setVisible(true);
            hasErrors = true;
        }

        if (hasErrors) {
            return;
        }

        // Stub "authentication": store the user information so other screens can read it
        AuthService authService = AuthService.getInstance();
        authService.setCurrentUser(new Admin(
                idField.getText().trim(),
                idField.getText().trim(),
                passwordField.getText()
        ));

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Unable to load dashboard");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    void register(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private void clearErrors() {
        if (idError != null) {
            idError.setText("");
            idError.setVisible(false);
        }
        if (passwordError != null) {
            passwordError.setText("");
            passwordError.setVisible(false);
        }
    }

}
