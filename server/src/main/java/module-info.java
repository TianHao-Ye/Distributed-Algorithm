module com.server {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.share;
    requires java.sql;

    opens com.server to javafx.fxml;
    exports com.server;
}
