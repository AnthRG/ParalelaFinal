package app.paralelafinal.escenario2.simulation;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Vehicle add menu for Scenario 2.
 * Lanes/IDs: East1, East2, East3, West1, West2, West3
 * Options per lane: Straight, Left, Right, U-turn
 */
public class VehicleAddMenu2 {

    public static void display(SimulationEngine2 simulationEngine) {
        Stage window = new Stage();
        window.initModality(Modality.NONE);
        window.setTitle("Add Vehicle - Scenario 2");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10, 10, 10, 10));

        String[] lanes = {"East1", "East2", "West1", "West2"}; 
        String[] directions = {"Straight", "Left", "Right", "U-turn", "U-turn 2nd"}; 

        for (int col = 0; col < lanes.length; col++) {
            String laneId = lanes[col];

            Label headerLabel = new Label(laneId);
            headerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            GridPane.setConstraints(headerLabel, col, 0);
            grid.getChildren().add(headerLabel);

            VBox buttonContainer = new VBox(5);
            buttonContainer.setAlignment(Pos.CENTER_LEFT);

            // Emergency toggle per lane
            CheckBox emergencyCheck = new CheckBox("Emergency");
            buttonContainer.getChildren().add(emergencyCheck);

            for (String direction : directions) {
                Button btn = new Button(direction);
                btn.setMinWidth(90);
                btn.setOnAction(e -> {
                    String dir = direction.toLowerCase().replace(" ", "-"); // "straight", "left", "right", "u-turn", "u-turn-2nd"
                    String type = emergencyCheck.isSelected() ? "emergency" : "normal";
                    // Expects SimulationEngine2 to provide addVehicle(String type, String direction, String laneId)
                    simulationEngine.addVehicle(type, dir, laneId);
                });
                buttonContainer.getChildren().add(btn);
            }

            GridPane.setConstraints(buttonContainer, col, 1);
            grid.getChildren().add(buttonContainer);
        }

        // Add debug button for U-turn vehicles
        Button debugButton = new Button("Debug U-Turn");
        debugButton.setOnAction(e -> {
            simulationEngine.debugUTurnVehicles();
        });
        GridPane.setConstraints(debugButton, 0, 2);
        GridPane.setColumnSpan(debugButton, lanes.length);
        grid.getChildren().add(debugButton);

        Scene scene = new Scene(grid);
        window.setScene(scene);
        window.show();
    }
}
