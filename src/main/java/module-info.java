module edu.facilities {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive java.sql;
    requires org.postgresql.jdbc;
    requires com.zaxxer.hikari;
    requires org.apache.pdfbox;

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