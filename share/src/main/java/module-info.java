module com.share {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.share to javafx.fxml;
    exports com.share;
}
