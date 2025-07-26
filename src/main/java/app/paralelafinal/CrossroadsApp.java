package app.paralelafinal;

import app.paralelafinal.config.SimulationConfig;
import app.paralelafinal.escenario1.simulation.SimulationEngine;
import app.paralelafinal.escenario1.simulation.SimulationPane;
import app.paralelafinal.escenario2.simulation.RoadGridApp;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * The main entry point for the traffic simulation application.
 * This class initializes the simulation engine and the user interface,
 * sets up the main window (Stage), and starts the simulation.
 */
public class CrossroadsApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showMainMenu();
    }

    /**
     * Shows the main menu with options to choose between scenarios
     */
    private void showMainMenu() {
        VBox menuLayout = new VBox(20);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setPadding(new Insets(50));
        menuLayout.setStyle("-fx-background-color: #f0f0f0;");

        // Title
        Label titleLabel = new Label("Traffic Simulation");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: #333333;");

        Label subtitleLabel = new Label("Choose a scenario:");
        subtitleLabel.setFont(Font.font("Arial", 18));
        subtitleLabel.setStyle("-fx-text-fill: #666666;");

        // Scenario 1 Button
        Button scenario1Button = new Button("Scenario 1: Crossroads Traffic");
        scenario1Button.setPrefWidth(300);
        scenario1Button.setPrefHeight(50);
        scenario1Button.setFont(Font.font("Arial", 16));
        scenario1Button.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 5;"
        );
        scenario1Button.setOnMouseEntered(e ->
                scenario1Button.setStyle(
                        "-fx-background-color: #45a049; " +
                                "-fx-text-fill: white; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-radius: 5;"
                )
        );
        scenario1Button.setOnMouseExited(e ->
                scenario1Button.setStyle(
                        "-fx-background-color: #4CAF50; " +
                                "-fx-text-fill: white; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-radius: 5;"
                )
        );
        scenario1Button.setOnAction(e -> launchScenario1());

        // Scenario 2 Button
        Button scenario2Button = new Button("Scenario 2: Road Grid");
        scenario2Button.setPrefWidth(300);
        scenario2Button.setPrefHeight(50);
        scenario2Button.setFont(Font.font("Arial", 16));
        scenario2Button.setStyle(
                "-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 5;"
        );
        scenario2Button.setOnMouseEntered(e ->
                scenario2Button.setStyle(
                        "-fx-background-color: #1976D2; " +
                                "-fx-text-fill: white; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-radius: 5;"
                )
        );
        scenario2Button.setOnMouseExited(e ->
                scenario2Button.setStyle(
                        "-fx-background-color: #2196F3; " +
                                "-fx-text-fill: white; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-radius: 5;"
                )
        );
        scenario2Button.setOnAction(e -> launchScenario2());

        // Exit Button
        Button exitButton = new Button("Exit");
        exitButton.setPrefWidth(300);
        exitButton.setPrefHeight(50);
        exitButton.setFont(Font.font("Arial", 16));
        exitButton.setStyle(
                "-fx-background-color: #f44336; " +
                        "-fx-text-fill: white; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 5;"
        );
        exitButton.setOnMouseEntered(e ->
                exitButton.setStyle(
                        "-fx-background-color: #d32f2f; " +
                                "-fx-text-fill: white; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-radius: 5;"
                )
        );
        exitButton.setOnMouseExited(e ->
                exitButton.setStyle(
                        "-fx-background-color: #f44336; " +
                                "-fx-text-fill: white; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-radius: 5;"
                )
        );
        exitButton.setOnAction(e -> primaryStage.close());

        // Add all elements to layout
        menuLayout.getChildren().addAll(
                titleLabel,
                subtitleLabel,
                scenario1Button,
                scenario2Button,
                exitButton
        );

        // Create and show scene
        Scene menuScene = new Scene(menuLayout, 500, 400);
        primaryStage.setTitle("Traffic Simulation - Main Menu");
        primaryStage.setScene(menuScene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    /**
     * Launches Scenario 1: Crossroads Traffic Simulation
     */
    private void launchScenario1() {
        try {
            // Hide the menu
            primaryStage.hide();

            // Create a new stage for scenario 1
            Stage scenario1Stage = new Stage();
            SimulationEngine simulationEngine = new SimulationEngine();
            SimulationPane simulationPane = new SimulationPane(simulationEngine);
            Scene scene = new Scene(simulationPane.getRoot(), SimulationConfig.SCENE_WIDTH, SimulationConfig.SCENE_HEIGHT);

            scenario1Stage.setTitle("Crossroads Traffic Simulation");
            scenario1Stage.setScene(scene);
            scenario1Stage.setResizable(false);
            scenario1Stage.show();

            simulationEngine.startSimulation();

            // Handle closing
            scenario1Stage.setOnCloseRequest(event -> {
                simulationEngine.stopSimulation();
                scenario1Stage.close();
                showMainMenu(); // Return to main menu
            });

        } catch (Exception e) {
            e.printStackTrace();
            showMainMenu(); // Return to menu on error
        }
    }

    /**
     * Launches Scenario 2: Road Grid
     */
    private void launchScenario2() {
        try {
            // Hide the menu
            primaryStage.hide();

            // Create a new stage for scenario 2
            Stage scenario2Stage = new Stage();
            RoadGridApp roadGridApp = new RoadGridApp();
            roadGridApp.start(scenario2Stage);

            // Handle closing
            scenario2Stage.setOnCloseRequest(event -> {
                scenario2Stage.close();
                showMainMenu(); // Return to main menu
            });

        } catch (Exception e) {
            e.printStackTrace();
            showMainMenu(); // Return to menu on error
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
