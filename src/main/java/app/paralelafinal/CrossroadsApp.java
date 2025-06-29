package app.paralelafinal;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import app.paralelafinal.entidades.TrafficLight;

// This
public class CrossroadsApp extends Application {

    // --- SCENE & ROAD DIMENSIONS ---
    private final double SCENE_WIDTH = 1200;
    private final double SCENE_HEIGHT = 900;
    private final double ROAD_WIDTH = 160; // Total width for 2 lanes

    // --- ROAD MARKING DIMENSIONS ---
    private final double DASHED_LINE_LENGTH = 25;
    private final double DASHED_LINE_GAP = 20;
    private final double LINE_THICKNESS = 4;
    private final double CENTER_LINE_GAP = 5; // Gap between the two yellow center lines

    // --- TRAFFIC LIGHT DIMENSIONS ---
    private final double LIGHT_RADIUS = 15;
    private final double LIGHT_SPACING = 10;
    private final double TRAFFIC_LIGHT_WIDTH = LIGHT_RADIUS * 2 + 10;
    private final double TRAFFIC_LIGHT_HEIGHT = LIGHT_RADIUS * 4 + LIGHT_SPACING * 3;

    // --- BACKEND LOGIC ---
    private TrafficLight trafficLightNorth; // For traffic going South
    private TrafficLight trafficLightSouth; // For traffic going North
    private TrafficLight trafficLightEast;  // For traffic going West
    private TrafficLight trafficLightWest;  // For traffic going East

    // Map to link backend TrafficLight objects to their JavaFX visual representations
    private Map<String, TrafficLightVisuals> trafficLightVisualsMap;

    private ScheduledExecutorService scheduler;

    @Override
    public void start(Stage primaryStage) {
        trafficLightVisualsMap = new HashMap<>();

        BorderPane root = new BorderPane();
        Pane simulationPane = new Pane();
        simulationPane.setPrefSize(SCENE_WIDTH, SCENE_HEIGHT);
        simulationPane.setStyle("-fx-background-color: #89CFF0;"); // Grassy green background

        // --- Draw all visual elements ---
        drawRoads(simulationPane);
        drawRoadMarkings(simulationPane);
        setupTrafficLightsAndLabels(simulationPane); // Renamed for clarity

        // --- UI CONTROLS ---
        Button toggleLightsButton = new Button("Manual Light Change");
        toggleLightsButton.setOnAction(e -> toggleTrafficLightsLogic());

        HBox buttonBox = new HBox(toggleLightsButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));
        root.setBottom(buttonBox);
        root.setCenter(simulationPane);

        // --- INITIALIZE SIMULATION ---
        initializeBackendLogic();
        updateAllVisuals();
        startAutomaticTrafficLightSwitching();

        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        primaryStage.setTitle("Improved Crossroads Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Ensure the background task is terminated when the window is closed
        primaryStage.setOnCloseRequest(event -> {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
        });
    }

    /**
     * Draws the main road infrastructure.
     */
    private void drawRoads(Pane pane) {
        double centerX = SCENE_WIDTH / 2;
        double centerY = SCENE_HEIGHT / 2;

        // A single rectangle for the entire horizontal road
        Rectangle horizontalRoad = new Rectangle(0, centerY - ROAD_WIDTH / 2, SCENE_WIDTH, ROAD_WIDTH);
        horizontalRoad.setFill(Color.DARKGRAY);

        // A single rectangle for the entire vertical road
        Rectangle verticalRoad = new Rectangle(centerX - ROAD_WIDTH / 2, 0, ROAD_WIDTH, SCENE_HEIGHT);
        verticalRoad.setFill(Color.DARKGRAY);

        pane.getChildren().addAll(horizontalRoad, verticalRoad);
    }

    /**
     * Draws the road lines (center and lane dividers).
     */
    private void drawRoadMarkings(Pane pane) {
        double centerX = SCENE_WIDTH / 2;
        double centerY = SCENE_HEIGHT / 2;
        Pane markingsPane = new Pane(); // Use a separate pane for markings to ensure they are on top of the road

        // --- DOUBLE SOLID YELLOW CENTER LINES ---
        // Horizontal
        markingsPane.getChildren().add(new Rectangle(0, centerY - CENTER_LINE_GAP / 2 - LINE_THICKNESS / 2, centerX - ROAD_WIDTH / 2, LINE_THICKNESS));
        markingsPane.getChildren().add(new Rectangle(0, centerY + CENTER_LINE_GAP / 2 - LINE_THICKNESS / 2, centerX - ROAD_WIDTH / 2, LINE_THICKNESS));
        markingsPane.getChildren().add(new Rectangle(centerX + ROAD_WIDTH / 2, centerY - CENTER_LINE_GAP / 2 - LINE_THICKNESS / 2, SCENE_WIDTH - (centerX + ROAD_WIDTH / 2), LINE_THICKNESS));
        markingsPane.getChildren().add(new Rectangle(centerX + ROAD_WIDTH / 2, centerY + CENTER_LINE_GAP / 2 - LINE_THICKNESS / 2, SCENE_WIDTH - (centerX + ROAD_WIDTH / 2), LINE_THICKNESS));
        // Vertical
        markingsPane.getChildren().add(new Rectangle(centerX - CENTER_LINE_GAP / 2 - LINE_THICKNESS / 2, 0, LINE_THICKNESS, centerY - ROAD_WIDTH / 2));
        markingsPane.getChildren().add(new Rectangle(centerX + CENTER_LINE_GAP / 2 - LINE_THICKNESS / 2, 0, LINE_THICKNESS, centerY - ROAD_WIDTH / 2));
        markingsPane.getChildren().add(new Rectangle(centerX - CENTER_LINE_GAP / 2 - LINE_THICKNESS / 2, centerY + ROAD_WIDTH / 2, LINE_THICKNESS, SCENE_HEIGHT - (centerY + ROAD_WIDTH / 2)));
        markingsPane.getChildren().add(new Rectangle(centerX + CENTER_LINE_GAP / 2 - LINE_THICKNESS / 2, centerY + ROAD_WIDTH / 2, LINE_THICKNESS, SCENE_HEIGHT - (centerY + ROAD_WIDTH / 2)));

        // Apply yellow color to all center lines
        markingsPane.getChildren().stream()
                .filter(node -> node instanceof Rectangle)
                .forEach(node -> ((Rectangle) node).setFill(Color.YELLOW));


        // --- WHITE DASHED LANE LINES ---
        // Horizontal Lanes
        double laneCenterY1 = centerY - ROAD_WIDTH / 4;
        double laneCenterY2 = centerY + ROAD_WIDTH / 4;
        for (double x = 0; x < SCENE_WIDTH; x += DASHED_LINE_LENGTH + DASHED_LINE_GAP) {
            if (x + DASHED_LINE_LENGTH < centerX - ROAD_WIDTH / 2 || x > centerX + ROAD_WIDTH / 2) {
                Rectangle dash1 = new Rectangle(x, laneCenterY1 - LINE_THICKNESS / 2, DASHED_LINE_LENGTH, LINE_THICKNESS);
                Rectangle dash2 = new Rectangle(x, laneCenterY2 - LINE_THICKNESS / 2, DASHED_LINE_LENGTH, LINE_THICKNESS);
                dash1.setFill(Color.WHITE);
                dash2.setFill(Color.WHITE);
                markingsPane.getChildren().addAll(dash1, dash2);
            }
        }

        // Vertical Lanes
        double laneCenterX1 = centerX - ROAD_WIDTH / 4;
        double laneCenterX2 = centerX + ROAD_WIDTH / 4;
        for (double y = 0; y < SCENE_HEIGHT; y += DASHED_LINE_LENGTH + DASHED_LINE_GAP) {
            if (y + DASHED_LINE_LENGTH < centerY - ROAD_WIDTH / 2 || y > centerY + ROAD_WIDTH / 2) {
                Rectangle dash1 = new Rectangle(laneCenterX1 - LINE_THICKNESS / 2, y, LINE_THICKNESS, DASHED_LINE_LENGTH);
                Rectangle dash2 = new Rectangle(laneCenterX2 - LINE_THICKNESS / 2, y, LINE_THICKNESS, DASHED_LINE_LENGTH);
                dash1.setFill(Color.WHITE);
                dash2.setFill(Color.WHITE);
                markingsPane.getChildren().addAll(dash1, dash2);
            }
        }
        pane.getChildren().add(markingsPane);
    }

    /**
     * Creates, positions, and labels all four traffic lights with a clear and simple layout.
     */
    private void setupTrafficLightsAndLabels(Pane pane) {
        double centerX = SCENE_WIDTH / 2;
        double centerY = SCENE_HEIGHT / 2;
        double offset = 25; // Offset from the corner of the road

        // --- Create and position each traffic light and its label ---

        // Light for SOUTHBOUND traffic (from North) -> Positioned Top-Left of intersection
        String southBoundText = "Controls Southbound Traffic";
        Group tl_NorthGroup = createTrafficLightVisuals();
        double tl_NorthX = centerX - ROAD_WIDTH / 2 - TRAFFIC_LIGHT_WIDTH - offset;
        double tl_NorthY = centerY - ROAD_WIDTH / 2 - TRAFFIC_LIGHT_HEIGHT - offset;
        addLightAndLabel(pane, tl_NorthGroup, tl_NorthX, tl_NorthY, southBoundText, tl_NorthX - 150, tl_NorthY - 40);
        trafficLightVisualsMap.put("North", new TrafficLightVisuals((Circle) tl_NorthGroup.getChildren().get(1), (Circle) tl_NorthGroup.getChildren().get(2)));

        // Light for NORTHBOUND traffic (from South) -> Positioned Bottom-Right of intersection
        String northBoundText = "Controls Northbound Traffic";
        Group tl_SouthGroup = createTrafficLightVisuals();
        double tl_SouthX = centerX + ROAD_WIDTH / 2 + offset;
        double tl_SouthY = centerY + ROAD_WIDTH / 2 + offset;
        addLightAndLabel(pane, tl_SouthGroup, tl_SouthX, tl_SouthY, northBoundText, tl_SouthX, tl_SouthY + TRAFFIC_LIGHT_HEIGHT + 20);
        trafficLightVisualsMap.put("South", new TrafficLightVisuals((Circle) tl_SouthGroup.getChildren().get(1), (Circle) tl_SouthGroup.getChildren().get(2)));

        // Light for WESTBOUND traffic (from East) -> Positioned Top-Right of intersection
        String westBoundText = "Controls Westbound Traffic";
        Group tl_EastGroup = createTrafficLightVisuals();
        double tl_EastX = centerX + ROAD_WIDTH / 2 + offset;
        double tl_EastY = centerY - ROAD_WIDTH / 2 - TRAFFIC_LIGHT_HEIGHT - offset;
        addLightAndLabel(pane, tl_EastGroup, tl_EastX, tl_EastY, westBoundText, tl_EastX, tl_EastY - 40);
        trafficLightVisualsMap.put("East", new TrafficLightVisuals((Circle) tl_EastGroup.getChildren().get(1), (Circle) tl_EastGroup.getChildren().get(2)));


        // Light for EASTBOUND traffic (from West) -> Positioned Bottom-Left of intersection
        String eastBoundText = "Controls Eastbound Traffic";
        Group tl_WestGroup = createTrafficLightVisuals();
        double tl_WestX = centerX - ROAD_WIDTH / 2 - TRAFFIC_LIGHT_WIDTH - offset;
        double tl_WestY = centerY + ROAD_WIDTH / 2 + offset;
        addLightAndLabel(pane, tl_WestGroup, tl_WestX, tl_WestY, eastBoundText, tl_WestX - 150, tl_WestY + TRAFFIC_LIGHT_HEIGHT + 20);
        trafficLightVisualsMap.put("West", new TrafficLightVisuals((Circle) tl_WestGroup.getChildren().get(1), (Circle) tl_WestGroup.getChildren().get(2)));
    }

    /**
     * Adds a traffic light group and a descriptive label to the pane at specified coordinates.
     *
     * @param pane       The pane to add the elements to.
     * @param lightGroup The visual group for the traffic light.
     * @param lightX     The X position for the traffic light.
     * @param lightY     The Y position for the traffic light.
     * @param labelText  The text for the label.
     * @param labelX     The X position for the label.
     * @param labelY     The Y position for the label.
     */
    private void addLightAndLabel(Pane pane, Group lightGroup, double lightX, double lightY, String labelText, double labelX, double labelY) {
        lightGroup.setLayoutX(lightX);
        lightGroup.setLayoutY(lightY);

        Label label = new Label(labelText);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-padding: 5px; -fx-background-radius: 5;");
        label.setLayoutX(labelX);
        label.setLayoutY(labelY);

        pane.getChildren().addAll(lightGroup, label);
    }


    /**
     * Creates a single traffic light visual group (the black casing with two lights).
     */
    private Group createTrafficLightVisuals() {
        Group lightGroup = new Group();
        Rectangle casing = new Rectangle(TRAFFIC_LIGHT_WIDTH, TRAFFIC_LIGHT_HEIGHT);
        casing.setFill(Color.BLACK);
        casing.setArcWidth(10);
        casing.setArcHeight(10);

        // Lights are initially off (gray)
        Circle redLight = new Circle(TRAFFIC_LIGHT_WIDTH / 2, LIGHT_RADIUS + LIGHT_SPACING, LIGHT_RADIUS, Color.DARKRED.desaturate());
        Circle greenLight = new Circle(TRAFFIC_LIGHT_WIDTH / 2, LIGHT_RADIUS * 3 + LIGHT_SPACING * 2, LIGHT_RADIUS, Color.DARKGREEN.desaturate());

        lightGroup.getChildren().addAll(casing, redLight, greenLight);
        return lightGroup;
    }

    /**
     * Initializes the backend traffic light objects and sets their initial state.
     */
    private void initializeBackendLogic() {
        // Note: The name of the light corresponds to the origin of the traffic it controls.
        // e.g., trafficLightNorth controls traffic coming FROM the North.
        trafficLightNorth = new TrafficLight("North");
        trafficLightSouth = new TrafficLight("South");
        trafficLightEast = new TrafficLight("East");
        trafficLightWest = new TrafficLight("West");

        // Initial state: North/South bound traffic is green, East/West is red.
        trafficLightNorth.changeLight(); // becomes green
        trafficLightSouth.changeLight(); // becomes green
    }


    /**
     * Toggles the state of all backend TrafficLight objects.
     */
    private void toggleTrafficLightsLogic() {
        trafficLightNorth.changeLight();
        trafficLightSouth.changeLight();
        trafficLightEast.changeLight();
        trafficLightWest.changeLight();

        // The logic runs in a background thread, so we must update the UI on the JavaFX Application Thread.
        Platform.runLater(this::updateAllVisuals);
    }

    /**
     * Updates the color of all traffic light visuals based on the state
     * of their corresponding backend `TrafficLight` objects.
     */
    private void updateAllVisuals() {
        updateLightVisual("North", trafficLightNorth);
        updateLightVisual("South", trafficLightSouth);
        updateLightVisual("East", trafficLightEast);
        updateLightVisual("West", trafficLightWest);
    }

    /**
     * Helper method to update a single traffic light's visuals by changing the fill color.
     */
    private void updateLightVisual(String direction, TrafficLight lightLogic) {
        TrafficLightVisuals visuals = trafficLightVisualsMap.get(direction);
        if (visuals != null) {
            if (lightLogic.isGreen()) {
                visuals.greenLight.setFill(Color.LIMEGREEN);
                visuals.redLight.setFill(Color.DARKRED.desaturate()); // Dim the red light
            } else {
                visuals.greenLight.setFill(Color.DARKGREEN.desaturate()); // Dim the green light
                visuals.redLight.setFill(Color.RED);
            }
        }
    }

    /**
     * Starts a scheduled task to automatically switch traffic lights every 10 seconds.
     */
    private void startAutomaticTrafficLightSwitching() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // The first change happens after an initial delay of 10 seconds.
        scheduler.scheduleAtFixedRate(this::toggleTrafficLightsLogic, 10, 10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * A helper class to store the Circle objects for a single traffic light's visuals.
     */
    private static class TrafficLightVisuals {
        final Circle redLight;
        final Circle greenLight;

        public TrafficLightVisuals(Circle redLight, Circle greenLight) {
            this.redLight = redLight;
            this.greenLight = greenLight;
        }
    }
}
