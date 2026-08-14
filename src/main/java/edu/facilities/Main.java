package edu.facilities;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Application entry point. Authentication always starts at login.fxml. */
public class Main extends Application {
    @Override public void start(Stage stage) {
        try {
            Parent root = FXMLLoader.load(Main.class.getResource("/fxml/login.fxml"));
            stage.setScene(new Scene(root, 520, 380));
            stage.setTitle("University Management System - Login");
            stage.show();
        } catch (Exception e) {
            System.err.println("Application startup failed: unable to load /fxml/login.fxml");
            e.printStackTrace();
        }
    }
    public static void main(String[] args) { launch(args); }
}
