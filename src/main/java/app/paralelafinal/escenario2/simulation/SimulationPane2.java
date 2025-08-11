package app.paralelafinal.escenario2.simulation;

import app.paralelafinal.config.SimulationConfig;
import app.paralelafinal.escenario2.entidades.Intersection;
import app.paralelafinal.escenario2.entidades.Vehicle;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * JavaFX application that draws a road grid with two horizontal and two vertical roads.
 * Horizontal roads have 3 lanes per direction (6 lanes total).
 * Vertical roads have 1 lane per direction (2 lanes total).
 * The roads have dashed lane dividers that are interrupted at intersections.
 */
public class SimulationPane2 extends Application {
    
    // Canvas dimensions - using SimulationConfig
    private static final double CANVAS_WIDTH = SimulationConfig.SCENE_WIDTH;
    private static final double CANVAS_HEIGHT = SimulationConfig.SCENE_HEIGHT;
    
    // Road dimensions
    private static final double HORIZONTAL_ROAD_WIDTH = SimulationConfig.ROAD_WIDTH * 1.875; // 3 lanes per direction (6 lanes total)
    private static final double VERTICAL_ROAD_WIDTH = SimulationConfig.ROAD_WIDTH; // 2 lanes total
    private static final double LANE_WIDTH = SimulationConfig.ROAD_WIDTH / 2; // Width of a single lane
    
    // Calculate block sizes to fit the canvas
    private static final double HORIZONTAL_BLOCK_SIZE = (CANVAS_WIDTH - 2 * VERTICAL_ROAD_WIDTH) / 3;
    private static final double VERTICAL_BLOCK_SIZE = (CANVAS_HEIGHT - 2 * HORIZONTAL_ROAD_WIDTH) / 3;
    
    // Line properties for dashed lane dividers - using SimulationConfig
    private static final double LINE_WIDTH = SimulationConfig.LINE_THICKNESS;
    private static final double DASH_LENGTH = SimulationConfig.DASHED_LINE_LENGTH;
    private static final double GAP_LENGTH = SimulationConfig.DASHED_LINE_GAP;
    
    @Override
    public void start(Stage primaryStage) {
        // Create the canvas
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Draw the road grid
        drawRoadGrid(gc);
        
        // Dynamic layer for vehicles
        Pane vehiclesLayer = new Pane();
        vehiclesLayer.setPickOnBounds(false);

        // Create engine and add-vehicle menu button
        SimulationEngine2 simulationEngine = new SimulationEngine2();
        simulationEngine.setUiUpdateCallback(v -> drawVehicles(vehiclesLayer, simulationEngine));
        simulationEngine.start();

        Button addVehicleButton = new Button("Add Vehicle");
        addVehicleButton.setOnAction(e -> VehicleAddMenu2.display(simulationEngine));

        // Create the scene with overlay button and vehicles layer
        StackPane root = new StackPane();
        root.getChildren().addAll(canvas, vehiclesLayer, addVehicleButton);
        StackPane.setAlignment(addVehicleButton, Pos.BOTTOM_CENTER);

        Scene scene = new Scene(root, CANVAS_WIDTH, CANVAS_HEIGHT);
        scene.setFill(Color.WHITE); // Set white background
        
        // Configure and show the stage
        primaryStage.setTitle("Road Grid - Scenario 2");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    
    /**
     * Draws the complete road grid including roads and lane dividers
     */
    private void drawRoadGrid(GraphicsContext gc) {
        // Clear canvas with white background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        
        // Draw the roads (black rectangles)
        drawRoads(gc);
        
        // Draw the lane dividers (white dashed lines)
        drawLaneDividers(gc);
    }
    
    /**
     * Draws the four roads (2 horizontal and 2 vertical) as black rectangles
     */
    private void drawRoads(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        
        // Vertical roads (2 lanes each)
        // First vertical road: starts after first horizontal block
        double x1 = HORIZONTAL_BLOCK_SIZE;
        gc.fillRect(x1, 0, VERTICAL_ROAD_WIDTH, CANVAS_HEIGHT);
        
        // Second vertical road: starts after first block + road + second block
        double x2 = HORIZONTAL_BLOCK_SIZE + VERTICAL_ROAD_WIDTH + HORIZONTAL_BLOCK_SIZE;
        gc.fillRect(x2, 0, VERTICAL_ROAD_WIDTH, CANVAS_HEIGHT);
        
        // Horizontal roads (6 lanes each - 3 per direction)
        // First horizontal road: starts after first vertical block
        double y1 = VERTICAL_BLOCK_SIZE;
        gc.fillRect(0, y1, CANVAS_WIDTH, HORIZONTAL_ROAD_WIDTH);
        
        // Second horizontal road: starts after first block + road + second block
        double y2 = VERTICAL_BLOCK_SIZE + HORIZONTAL_ROAD_WIDTH + VERTICAL_BLOCK_SIZE;
        gc.fillRect(0, y2, CANVAS_WIDTH, HORIZONTAL_ROAD_WIDTH);
    }
    
    /**
     * Draws the white dashed lane dividers on the roads, avoiding intersections
     */
    private void drawLaneDividers(GraphicsContext gc) {
        // Configure line properties
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(LINE_WIDTH);
        gc.setLineDashes(DASH_LENGTH, GAP_LENGTH);
        
        // Calculate vertical road positions
        double vertRoad1Start = HORIZONTAL_BLOCK_SIZE;
        double vertRoad1End = HORIZONTAL_BLOCK_SIZE + VERTICAL_ROAD_WIDTH;
        double vertRoad2Start = HORIZONTAL_BLOCK_SIZE + VERTICAL_ROAD_WIDTH + HORIZONTAL_BLOCK_SIZE;
        double vertRoad2End = HORIZONTAL_BLOCK_SIZE + VERTICAL_ROAD_WIDTH + HORIZONTAL_BLOCK_SIZE + VERTICAL_ROAD_WIDTH;
        
        // Calculate horizontal road positions
        double horizRoad1Start = VERTICAL_BLOCK_SIZE;
        double horizRoad1End = VERTICAL_BLOCK_SIZE + HORIZONTAL_ROAD_WIDTH;
        double horizRoad2Start = VERTICAL_BLOCK_SIZE + HORIZONTAL_ROAD_WIDTH + VERTICAL_BLOCK_SIZE;
        double horizRoad2End = VERTICAL_BLOCK_SIZE + HORIZONTAL_ROAD_WIDTH + VERTICAL_BLOCK_SIZE + HORIZONTAL_ROAD_WIDTH;
        
        // Draw horizontal lane dividers (5 lines per road for 6 lanes)
        drawHorizontalLaneDividers(gc, horizRoad1Start, horizRoad1End, horizRoad2Start, horizRoad2End,
                                  vertRoad1Start, vertRoad1End, vertRoad2Start, vertRoad2End);
        
        // Draw vertical lane dividers (1 line per road for 2 lanes)
        drawVerticalLaneDividers(gc, vertRoad1Start, vertRoad1End, vertRoad2Start, vertRoad2End,
                                horizRoad1Start, horizRoad1End, horizRoad2Start, horizRoad2End);
    }
    
    /**
     * Draws horizontal lane dividers with gaps at intersections
     * Horizontal roads have 6 lanes (3 per direction), so we need 5 divider lines
     */
    private void drawHorizontalLaneDividers(GraphicsContext gc, double horizRoad1Start, double horizRoad1End, 
                                           double horizRoad2Start, double horizRoad2End,
                                           double vertRoad1Start, double vertRoad1End,
                                           double vertRoad2Start, double vertRoad2End) {
        
        double laneHeight = HORIZONTAL_ROAD_WIDTH / 3; // 6 lanes total
        
        // Draw lines for first horizontal road
        for (int i = 1; i <= 2; i++) {
            double y = horizRoad1Start + (i * laneHeight);
            
            // Segment 1: From start to first vertical road
            gc.strokeLine(0, y, vertRoad1Start, y);
            
            // Segment 2: Between the two vertical roads
            gc.strokeLine(vertRoad1End, y, vertRoad2Start, y);
            
            // Segment 3: From second vertical road to end
            gc.strokeLine(vertRoad2End, y, CANVAS_WIDTH, y);
        }
        
        // Draw lines for second horizontal road
        for (int i = 1; i <= 2; i++) {
            double y = horizRoad2Start + (i * laneHeight);
            
            // Segment 1: From start to first vertical road
            gc.strokeLine(0, y, vertRoad1Start, y);
            
            // Segment 2: Between the two vertical roads
            gc.strokeLine(vertRoad1End, y, vertRoad2Start, y);
            
            // Segment 3: From second vertical road to end
            gc.strokeLine(vertRoad2End, y, CANVAS_WIDTH, y);
        }
    }
    
    /**
     * Draws vertical lane dividers with gaps at intersections
     * Vertical roads have 2 lanes (1 per direction), so we need 1 divider line
     */
    private void drawVerticalLaneDividers(GraphicsContext gc, double vertRoad1Start, double vertRoad1End, 
                                         double vertRoad2Start, double vertRoad2End,
                                         double horizRoad1Start, double horizRoad1End,
                                         double horizRoad2Start, double horizRoad2End) {
        // First vertical road center line
        double x1 = vertRoad1Start + VERTICAL_ROAD_WIDTH / 2;
        
        // Segment 1: From start to first horizontal road
        gc.strokeLine(x1, 0, x1, horizRoad1Start);
        
        // Segment 2: Between the two horizontal roads
        gc.strokeLine(x1, horizRoad1End, x1, horizRoad2Start);
        
        // Segment 3: From second horizontal road to end
        gc.strokeLine(x1, horizRoad2End, x1, CANVAS_HEIGHT);
        
        // Second vertical road center line
        double x2 = vertRoad2Start + VERTICAL_ROAD_WIDTH / 2;
        
        // Segment 1: From start to first horizontal road
        gc.strokeLine(x2, 0, x2, horizRoad1Start);
        
        // Segment 2: Between the two horizontal roads
        gc.strokeLine(x2, horizRoad1End, x2, horizRoad2Start);
        
        // Segment 3: From second horizontal road to end
        gc.strokeLine(x2, horizRoad2End, x2, CANVAS_HEIGHT);
    }
    
    private void drawVehicles(Pane layer, SimulationEngine2 engine) {
        // Remove previous vehicle nodes
        layer.getChildren().removeIf(node -> "vehicle".equals(node.getUserData()));

        for (Intersection intersection : engine.getIntersections()) {
            // Iterate each queue: mid, right, left
            for (Vehicle v : intersection.getMidVQueue()) {
                addVehicleNode(layer, v, intersection.getId());
            }
            for (Vehicle v : intersection.getRightVQueue()) {
                addVehicleNode(layer, v, intersection.getId());
            }
            for (Vehicle v : intersection.getLeftVQueue()) {
                addVehicleNode(layer, v, intersection.getId());
            }
        }
    }

    private void addVehicleNode(Pane layer, Vehicle v, String intersectionId) {
        Group sprite = createVehicleShape(v);
        sprite.setUserData("vehicle");

        // Determine heading based on side: East* moves westbound (180 deg), West* moves eastbound (0 deg)
        double angle = intersectionId.startsWith("East") ? 180 : 0;
        sprite.setRotate(angle);

        // Position at vehicle's logical coordinates
        if (v.getPosition() != null) {
            sprite.setLayoutX(v.getPosition().getX());
            sprite.setLayoutY(v.getPosition().getY());
        }

        layer.getChildren().add(sprite);
    }

    private Group createVehicleShape(Vehicle v) {
        Group vehicleGroup = new Group();

        Rectangle body = new Rectangle(SimulationConfig.VEHICLE_LENGTH, SimulationConfig.VEHICLE_WIDTH);
        body.setArcWidth(5);
        body.setArcHeight(5);

        if ("emergency".equalsIgnoreCase(v.getType())) {
            body.setFill(Color.RED);
            body.setStroke(Color.YELLOW);
            body.setStrokeWidth(2);

            Rectangle light1 = new Rectangle(SimulationConfig.VEHICLE_LENGTH * 0.3, 3);
            light1.setFill(Color.YELLOW);
            light1.setX(SimulationConfig.VEHICLE_LENGTH * 0.1);
            light1.setY(-2);

            Rectangle light2 = new Rectangle(SimulationConfig.VEHICLE_LENGTH * 0.3, 3);
            light2.setFill(Color.BLUE);
            light2.setX(SimulationConfig.VEHICLE_LENGTH * 0.6);
            light2.setY(-2);

            vehicleGroup.getChildren().addAll(body, light1, light2);
        } else {
            Color[] carColors = {Color.BLUE, Color.GREEN, Color.PURPLE, Color.ORANGE, Color.BROWN, Color.NAVY};
            body.setFill(carColors[Math.abs(v.getId().hashCode()) % carColors.length]);
            body.setStroke(Color.BLACK);
            body.setStrokeWidth(1);
            vehicleGroup.getChildren().add(body);
        }

        Rectangle frontWindow = new Rectangle(SimulationConfig.VEHICLE_LENGTH * 0.15, SimulationConfig.VEHICLE_WIDTH * 0.6);
        frontWindow.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.7));
        frontWindow.setX(SimulationConfig.VEHICLE_LENGTH * 0.75);
        frontWindow.setY(SimulationConfig.VEHICLE_WIDTH * 0.2);

        Rectangle backWindow = new Rectangle(SimulationConfig.VEHICLE_LENGTH * 0.15, SimulationConfig.VEHICLE_WIDTH * 0.6);
        backWindow.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.7));
        backWindow.setX(SimulationConfig.VEHICLE_LENGTH * 0.1);
        backWindow.setY(SimulationConfig.VEHICLE_WIDTH * 0.2);

        Circle wheel1 = new Circle(SimulationConfig.VEHICLE_LENGTH * 0.2, SimulationConfig.VEHICLE_WIDTH + 2, 3);
        wheel1.setFill(Color.BLACK);
        Circle wheel2 = new Circle(SimulationConfig.VEHICLE_LENGTH * 0.8, SimulationConfig.VEHICLE_WIDTH + 2, 3);
        wheel2.setFill(Color.BLACK);
        Circle wheel3 = new Circle(SimulationConfig.VEHICLE_LENGTH * 0.2, -2, 3);
        wheel3.setFill(Color.BLACK);
        Circle wheel4 = new Circle(SimulationConfig.VEHICLE_LENGTH * 0.8, -2, 3);
        wheel4.setFill(Color.BLACK);

        vehicleGroup.getChildren().addAll(frontWindow, backWindow, wheel1, wheel2, wheel3, wheel4);
        vehicleGroup.setUserData("vehicle");

        return vehicleGroup;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
