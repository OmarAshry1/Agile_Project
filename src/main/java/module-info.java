module edu.facilities {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive java.sql;
    // Non-modularized libraries use automatic module names
    // Try JAR filename-based names first (most common)
    // HikariCP JAR: HikariCP-5.1.0.jar -> automatic module name: "hikaricp" (lowercase, no version)
    requires hikaricp;
    // PostgreSQL JAR: postgresql-42.7.1.jar -> automatic module name: "postgresql" (lowercase, no version)  
    requires postgresql;
    // PDFBox is loaded via reflection, so no module requirement needed

    opens edu.facilities.ui to javafx.fxml, javafx.graphics;
    opens edu.facilities to javafx.fxml, javafx.graphics;
    opens edu.curriculum.ui to javafx.fxml, javafx.graphics;
    opens edu.staff.ui to javafx.fxml, javafx.graphics;
    opens edu.community.ui to javafx.fxml, javafx.graphics;
    opens edu.community.model to javafx.base;

    exports edu.facilities;
    exports edu.facilities.ui;
    exports edu.facilities.model;
    exports edu.facilities.service;
    exports edu.curriculum.model;
    exports edu.curriculum.service;
    exports edu.curriculum.ui;
    exports edu.staff.model;
    exports edu.staff.service;
    exports edu.staff.ui;
    exports edu.community.model;
    exports edu.community.service;
    exports edu.community.ui;
}