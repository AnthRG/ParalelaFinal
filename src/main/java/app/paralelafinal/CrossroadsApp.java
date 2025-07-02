// app.paralelafinal.CrossroadsApp
package app.paralelafinal;

import app.paralelafinal.config.SimulationConfig;
import app.paralelafinal.simulation.SimulationEngine;
import app.paralelafinal.simulation.SimulationPane;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CrossroadsApp extends Application {

    private SimulationEngine simulationEngine;
    private SimulationPane simulationPane;

    @Override
    public void start(Stage primaryStage) throws InterruptedException {
        // Initialize simulation logic

        // Initialize UI components
        simulationPane = SimulationPane.getInstance(); // Pass engine to pane for drawing state

        // Setup UI (buttons, etc.) - could be handled by a UIController or directly in SimulationPane
        // Example: If you have a separate UIController
        // UIController uiController = new UIController(simulationEngine, simulationPane);
        // uiController.setupControls(root); // root is the BorderPane

        Scene scene = new Scene(simulationPane.getRoot(), SimulationConfig.SCENE_WIDTH, SimulationConfig.SCENE_HEIGHT);
        primaryStage.setTitle("Improved Crossroads Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start simulation updates (e.g., traffic light cycles, vehicle movements)
        simulationPane.getSimulationEngine().startSimulation();

        primaryStage.setOnCloseRequest(event -> {
            simulationPane.getSimulationEngine().stopSimulation();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}