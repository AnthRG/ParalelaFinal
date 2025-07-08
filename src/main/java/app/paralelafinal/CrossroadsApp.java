package app.paralelafinal;

import app.paralelafinal.config.SimulationConfig;
import app.paralelafinal.simulation.SimulationEngine;
import app.paralelafinal.simulation.SimulationPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The main entry point for the traffic simulation application.
 * This class initializes the simulation engine and the user interface,
 * sets up the main window (Stage), and starts the simulation.
 */
public class CrossroadsApp extends Application {

    private SimulationEngine simulationEngine;

    /**
     * Esta es la clase principal de la aplicación JavaFX.
     * Aquí se inicializa el motor de simulación y se configura la interfaz de usuario.
     * 
     *@param primaryStage la principal ventana de la aplicación.
     *
     */
    @Override
    public void start(Stage primaryStage) throws InterruptedException {
        simulationEngine = new SimulationEngine();
        SimulationPane simulationPane = new SimulationPane(simulationEngine);
        Scene scene = new Scene(simulationPane.getRoot(), SimulationConfig.SCENE_WIDTH, SimulationConfig.SCENE_HEIGHT);

        primaryStage.setTitle("Crossroads Traffic Simulation");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        simulationEngine.startSimulation();

        primaryStage.setOnCloseRequest(event -> {
            simulationEngine.stopSimulation();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
