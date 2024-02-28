module com.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.share;
    requires java.sql;

    opens com.client to javafx.fxml;
    exports com.client;
}
