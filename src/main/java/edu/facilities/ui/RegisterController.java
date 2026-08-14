package edu.facilities.ui;
import edu.facilities.service.AuthService; import javafx.fxml.*; import javafx.event.*; import javafx.scene.*; import javafx.scene.control.*; import javafx.stage.*; import java.io.*; import java.sql.*;
public class RegisterController { @FXML private TextField usernameField,emailField; @FXML private PasswordField passwordField,confirmField; @FXML private Label errorLabel;
 @FXML private void register(ActionEvent e) throws IOException { if(!passwordField.getText().equals(confirmField.getText())){errorLabel.setText("Passwords do not match.");return;} try{if(!AuthService.getInstance().register(usernameField.getText(),passwordField.getText(),emailField.getText(),"STUDENT"))errorLabel.setText("Registration failed. Check the fields or choose another username.");else back(e);}catch(SQLException x){errorLabel.setText("Unable to register: "+x.getMessage());} }
 @FXML private void back(ActionEvent e)throws IOException{Stage s=(Stage)((Node)e.getSource()).getScene().getWindow();s.setScene(new Scene(FXMLLoader.load(getClass().getResource("/fxml/login.fxml"))));}
}
