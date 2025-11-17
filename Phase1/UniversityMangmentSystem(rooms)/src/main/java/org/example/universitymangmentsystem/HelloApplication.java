package org.example.universitymangmentsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the FXML file
        Parent root = FXMLLoader.load(
                getClass().getResource("/fxml/rooms.fxml")
        );

        // Create scene
        Scene scene = new Scene(root, 1200, 700);

        // Set up stage
        primaryStage.setTitle("University Management System - Rooms");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}