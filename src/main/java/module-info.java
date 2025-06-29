module app.paralelafinal {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    opens app.paralelafinal to javafx.fxml;
    exports app.paralelafinal;
    exports app.paralelafinal.entidades;
    opens app.paralelafinal.entidades to javafx.fxml;
    exports app.paralelafinal.examples;
    opens app.paralelafinal.examples to javafx.fxml;
    exports app.paralelafinal.controladores;
    opens app.paralelafinal.controladores to javafx.fxml;
}