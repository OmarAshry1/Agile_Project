module edu.facilities {
    // JavaFX modules we need
    requires javafx.controls;
    requires javafx.fxml;
    
    // Java SQL module for database access
    requires java.sql;

    // Export and open packages for JavaFX
    opens edu.facilities.ui to javafx.fxml, javafx.graphics;
    opens edu.facilities to javafx.fxml, javafx.graphics;
    
    // Export packages
    exports edu.facilities;
    exports edu.facilities.ui;
    exports edu.facilities.model;
    exports edu.facilities.service;
}

