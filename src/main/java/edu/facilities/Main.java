package edu.facilities;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main application entry point for University Management System
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Try to load login.fxml first, if it doesn't exist, try dashboard
            Parent root;
            try {
                root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
                primaryStage.setTitle("University Management System - Login");
            } catch (Exception e) {
                // If login.fxml doesn't exist, try dashboard
                try {
                    root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
                    primaryStage.setTitle("University Management System");
                } catch (Exception e2) {
                    // If neither exists, create a simple scene
                    root = createDefaultScene();
                    primaryStage.setTitle("University Management System");
                }
            }

            // Set appropriate window size based on content
            int width = 1400;
            int height = 800;
            Scene scene = new Scene(root, width, height);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.setResizable(true);
            primaryStage.setMaximized(true);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading application: " + e.getMessage());
        }
    }

    private Parent createDefaultScene() throws IOException {
        // Create a simple default scene if FXML files are not found
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(20));
        javafx.scene.control.Label label = new javafx.scene.control.Label("University Management System");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        vbox.getChildren().add(label);
        vbox.getChildren().add(new javafx.scene.control.Label("Please ensure FXML files are in src/main/resources/fxml/"));
        return vbox;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

