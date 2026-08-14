package edu.facilities.ui;
import edu.facilities.service.AuthService; import javafx.fxml.*; import javafx.event.*; import javafx.scene.*; import javafx.scene.control.*; import javafx.stage.*; import java.io.*; import java.sql.*;
public class LoginController { @FXML private TextField usernameField; @FXML private PasswordField passwordField; @FXML private Label errorLabel;
 @FXML private void handleLogin(ActionEvent e){errorLabel.setText("");if(usernameField.getText().isBlank()||passwordField.getText().isBlank()){errorLabel.setText("Username and password are required.");return;}try{if(AuthService.getInstance().login(usernameField.getText(),passwordField.getText())==null){errorLabel.setText("Invalid username or password.");return;}Stage s=(Stage)((Node)e.getSource()).getScene().getWindow();s.setScene(new Scene(FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"))));}catch(SQLException|IOException x){errorLabel.setText("Unable to sign in: "+x.getMessage());}}
 @FXML private void openRegister(ActionEvent e)throws IOException{Stage s=(Stage)((Node)e.getSource()).getScene().getWindow();s.setScene(new Scene(FXMLLoader.load(getClass().getResource("/fxml/register.fxml"))));}
}
