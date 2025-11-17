module org.example.universitymangmentsystem {
    // JavaFX modules we need
    requires javafx.controls;
    requires javafx.fxml;

    // Export main package to javafx.graphics (for Application)
    opens org.example.universitymangmentsystem to javafx.fxml, javafx.graphics;

    // Export controllers package to javafx.fxml (THIS IS THE FIX!)
    opens org.example.universitymangmentsystem.controllers to javafx.fxml;

    // Export main package
    exports org.example.universitymangmentsystem;

    // Export controllers
    exports org.example.universitymangmentsystem.controllers;
}