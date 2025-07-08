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
        // 1. Initialize the simulation logic engine first.
        simulationEngine = new SimulationEngine();

        // 2. Initialize the main UI component, passing the engine to it.
        //    The SimulationPane is the root of our scene.
        SimulationPane simulationPane = new SimulationPane(simulationEngine);

        // 3. Create the main scene with the desired dimensions.
        Scene scene = new Scene(simulationPane.getRoot(), SimulationConfig.SCENE_WIDTH, SimulationConfig.SCENE_HEIGHT);

        // 4. Configure and show the primary stage (the application window).
        primaryStage.setTitle("Crossroads Traffic Simulation");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // Optional: lock window size
        primaryStage.show();

        // 5. Start the simulation logic (traffic light cycles, vehicle movement).
        simulationEngine.startSimulation();

        // 6. Ensure the simulation's background threads are stopped when the window is closed.
        primaryStage.setOnCloseRequest(event -> {
            simulationEngine.stopSimulation();
            // Platform.exit(); // Not strictly necessary but good practice
            // System.exit(0);
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
