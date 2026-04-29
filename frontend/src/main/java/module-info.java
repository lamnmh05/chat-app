module com.doan.frontend {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.doan.frontend to javafx.fxml;
    exports com.doan.frontend;
}