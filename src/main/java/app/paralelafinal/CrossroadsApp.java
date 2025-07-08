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
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     *
     * @param primaryStage the primary stage for this application, onto which
     * the application scene can be set.
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

    /**
     * The main method is only needed for the IDE in case JavaFX
     * components are not launched properly.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
