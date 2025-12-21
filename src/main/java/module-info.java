module edu.facilities {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.microsoft.sqlserver.jdbc;
    requires org.apache.pdfbox;

    opens edu.facilities.ui to javafx.fxml, javafx.graphics;
    opens edu.facilities to javafx.fxml, javafx.graphics;
    opens edu.curriculum.ui to javafx.fxml, javafx.graphics;

    exports edu.facilities;
    exports edu.facilities.ui;
    exports edu.facilities.model;
    exports edu.facilities.service;
    exports edu.curriculum.model;
    exports edu.curriculum.service;
    exports edu.curriculum.ui;
}