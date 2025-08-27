module app.paralelafinal {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing; // <--- ADDED THIS
    requires javafx.media; // <--- ADDED THIS
    requires kotlin.stdlib;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires annotations;
    requires javafx.graphics;

    opens app.paralelafinal to javafx.fxml;
    exports app.paralelafinal;
    exports app.paralelafinal.escenario1.entidades;
    exports app.paralelafinal.escenario1.simulation;
    opens app.paralelafinal.escenario1.entidades to javafx.fxml;
    exports app.paralelafinal.escenario1.controladores;
    opens app.paralelafinal.escenario1.controladores to javafx.fxml;
    exports app.paralelafinal.escenario2.entidades;
    exports app.paralelafinal.escenario2.controladores;
    exports app.paralelafinal.escenario2.simulation;
    opens app.paralelafinal.escenario2.entidades to javafx.fxml;
    opens app.paralelafinal.escenario2.controladores to javafx.fxml;
    opens app.paralelafinal.escenario2.simulation to javafx.fxml;

}