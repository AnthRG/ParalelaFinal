package app.paralelafinal.simulation;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class VehicleAddMenu {

    public static void display(SimulationEngine simulationEngine) {
        Stage window = new Stage();
        window.initModality(Modality.NONE);
        window.setTitle("Add Vehicle Menu");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10, 10, 10, 10));

        String[] intersections = {"North", "East", "West", "South"};
        String[] directions = {"Straight", "Left", "Right", "U-turn"};

        for (int col = 0; col < intersections.length; col++) {
            String intersectionId = intersections[col];

            // Header Label (e.g., "North")
            Label headerLabel = new Label(intersectionId);
            headerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            GridPane.setConstraints(headerLabel, col, 0);
            grid.getChildren().add(headerLabel);

            VBox buttonContainer = new VBox(5);
            buttonContainer.setAlignment(Pos.CENTER_LEFT);

            // Maneuver Buttons
            for (String direction : directions) {
                Button btn = new Button(direction);
                btn.setMinWidth(80);
                btn.setOnAction(e -> {
                    // This is a placeholder for the CheckBox logic
                });
                buttonContainer.getChildren().add(btn);
            }

            // Emergency CheckBox
            CheckBox emergencyCheck = new CheckBox("Emergency");
            buttonContainer.getChildren().add(emergencyCheck);

            // Add action handlers to buttons now that checkbox exists
            for (int i = 0; i < directions.length; i++) {
                Button btn = (Button) buttonContainer.getChildren().get(i);
                final String finalDirection = directions[i];
                btn.setOnAction(e -> {
                    String vehicleType = emergencyCheck.isSelected() ? "emergency" : "normal";
                    simulationEngine.addVehicle(vehicleType, finalDirection.toLowerCase(), intersectionId);
                });
            }

            GridPane.setConstraints(buttonContainer, col, 1);
            grid.getChildren().add(buttonContainer);
        }

        Scene scene = new Scene(grid);
        window.setScene(scene);
        window.show(); // Use show() instead of showAndWait() to allow interaction with main window
    }
}