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
 * Lanes/IDs: East1, West1
 * Options per lane: Left, Left North First, Left North Second, Straight, Right, 
 *                   Right South First, Right South Second, U-turn, U-turn Second
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

        String[] lanes = {"East1", "West1"}; 
        
        // Different directions for East and West
        String[] westDirections = {
            "Left", 
            "Left North First",   // West: left -> north
            "Left North Second",  // West: left -> north (second)
            "Straight", 
            "Right", 
            "Right South First",  // West: right -> south
            "Right South Second", // West: right -> south (second)
            "U-turn", 
            "U-turn Second"
        };
        
        String[] eastDirections = {
            "Left", 
            "Left South First",   // East: left -> south
            "Left South Second",  // East: left -> south (second)
            "Straight", 
            "Right", 
            "Right North First",  // East: right -> north
            "Right North Second", // East: right -> north (second)
            "U-turn", 
            "U-turn Second"
        };

        for (int col = 0; col < lanes.length; col++) {
            String laneId = lanes[col];
            String[] directions = laneId.startsWith("East") ? eastDirections : westDirections;

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
                btn.setMinWidth(140); // Increased width for longer button names
                btn.setOnAction(e -> {
                    String dir = direction.toLowerCase().replace(" ", "-"); // converts to lowercase with hyphens
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
