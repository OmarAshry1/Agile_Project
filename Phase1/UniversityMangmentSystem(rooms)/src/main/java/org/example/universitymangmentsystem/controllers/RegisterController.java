package org.example.universitymangmentsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML
    private Label confirmPasswordError;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label emailError;

    @FXML
    private TextField emailField;

    @FXML
    private Hyperlink loginLink;

    @FXML
    private Label passwordError;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button registerButton;

    @FXML
    private ComboBox<?> roleComboBox;

    @FXML
    private Label roleError;

    @FXML
    private Label usernameError;

    @FXML
    private TextField usernameField;

    @FXML
    void handleRegister(ActionEvent event) {

    }

    @FXML
    void login(ActionEvent event) {

    }

    @FXML
    void selectrole(ActionEvent event) {

    }

}
