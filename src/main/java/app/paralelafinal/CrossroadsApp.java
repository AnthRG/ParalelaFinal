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

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import app.paralelafinal.entidades.TrafficLight;
import app.paralelafinal.entidades.Intersection;
import app.paralelafinal.entidades.Vehicle;
import app.paralelafinal.controladores.TrafficController;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.effect.Glow;

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

    // --- VEHICLE VISUALIZATION ---
    private List<Intersection> intersections;
    private List<TrafficLight> trafficLights;
    private TrafficController trafficController;
    private Pane simulationPane;
    private Map<Vehicle, Circle> vehicleCircles = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        trafficLightVisualsMap = new HashMap<>();

        BorderPane root = new BorderPane();
        simulationPane = new Pane();
        addPareSigns(simulationPane);
        simulationPane.setPrefSize(SCENE_WIDTH, SCENE_HEIGHT);
        simulationPane.setStyle("-fx-background-color: #89CFF0;");

        // --- Draw all visual elements ---
        drawRoads(simulationPane);
        drawRoadMarkings(simulationPane);
        setupTrafficLightsAndLabels(simulationPane);

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

        // --- VEHICLE LOGIC ---
        setupIntersectionsAndController();
        startVehicleVisualizationUpdater();

        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        primaryStage.setTitle("Improved Crossroads Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Ensure the background task is terminated when the window is closed
        primaryStage.setOnCloseRequest(event -> {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
            if (trafficController != null) {
                trafficController.stopControl();
            }
        });
    }

    // --- VEHICLE & INTERSECTION SETUP ---
    private void setupIntersectionsAndController() {
        intersections = new ArrayList<>();
        trafficLights = new ArrayList<>();
        // Create 4 intersections (one for each direction)
        intersections.add(new Intersection("North", true));
        intersections.add(new Intersection("South", true));
        intersections.add(new Intersection("East", true));
        intersections.add(new Intersection("West", true));

        // Use the backend traffic lights
        trafficLights.add(trafficLightNorth);
        trafficLights.add(trafficLightSouth);
        trafficLights.add(trafficLightEast);
        trafficLights.add(trafficLightWest);

        trafficController = new TrafficController(intersections, trafficLights);
        trafficController.startControl();
    }
    private void setupTrafficLightsAndLabels(Pane pane) {
        double centerX = SCENE_WIDTH / 2;
        double centerY = SCENE_HEIGHT / 2;
        double offset = 35; // Offset mejorado

        // Crear labels con mejor estilo
        String southBoundText = "⬇ Tráfico Sur";
        Group tl_NorthGroup = createTrafficLightVisuals();
        double tl_NorthX = centerX - ROAD_WIDTH / 2 - TRAFFIC_LIGHT_WIDTH - offset;
        double tl_NorthY = centerY - ROAD_WIDTH / 2 - TRAFFIC_LIGHT_HEIGHT - offset;
        addImprovedLightAndLabel(pane, tl_NorthGroup, tl_NorthX, tl_NorthY, southBoundText, tl_NorthX - 80, tl_NorthY - 30);
        trafficLightVisualsMap.put("North", new TrafficLightVisuals((Circle) tl_NorthGroup.getChildren().get(4), (Circle) tl_NorthGroup.getChildren().get(5)));

        String northBoundText = "⬆ Tráfico Norte";
        Group tl_SouthGroup = createTrafficLightVisuals();
        double tl_SouthX = centerX + ROAD_WIDTH / 2 + offset;
        double tl_SouthY = centerY + ROAD_WIDTH / 2 + offset;
        addImprovedLightAndLabel(pane, tl_SouthGroup, tl_SouthX, tl_SouthY, northBoundText, tl_SouthX - 50, tl_SouthY + TRAFFIC_LIGHT_HEIGHT + 70);
        trafficLightVisualsMap.put("South", new TrafficLightVisuals((Circle) tl_SouthGroup.getChildren().get(4), (Circle) tl_SouthGroup.getChildren().get(5)));

        String westBoundText = "⬅ Tráfico Oeste";
        Group tl_EastGroup = createTrafficLightVisuals();
        double tl_EastX = centerX + ROAD_WIDTH / 2 + offset;
        double tl_EastY = centerY - ROAD_WIDTH / 2 - TRAFFIC_LIGHT_HEIGHT - offset;
        addImprovedLightAndLabel(pane, tl_EastGroup, tl_EastX, tl_EastY, westBoundText, tl_EastX - 50, tl_EastY - 30);
        trafficLightVisualsMap.put("East", new TrafficLightVisuals((Circle) tl_EastGroup.getChildren().get(4), (Circle) tl_EastGroup.getChildren().get(5)));

        String eastBoundText = "➡ Tráfico Este";
        Group tl_WestGroup = createTrafficLightVisuals();
        double tl_WestX = centerX - ROAD_WIDTH / 2 - TRAFFIC_LIGHT_WIDTH - offset;
        double tl_WestY = centerY + ROAD_WIDTH / 2 + offset;
        addImprovedLightAndLabel(pane, tl_WestGroup, tl_WestX, tl_WestY, eastBoundText, tl_WestX - 80, tl_WestY + TRAFFIC_LIGHT_HEIGHT + 70);
        trafficLightVisualsMap.put("West", new TrafficLightVisuals((Circle) tl_WestGroup.getChildren().get(4), (Circle) tl_WestGroup.getChildren().get(5)));
}


    // --- VEHICLE VISUALIZATION ---
    private void startVehicleVisualizationUpdater() {
        ScheduledExecutorService vehicleUpdater = Executors.newSingleThreadScheduledExecutor();
        vehicleUpdater.scheduleAtFixedRate(() -> Platform.runLater(this::drawVehicles), 0, 500, TimeUnit.MILLISECONDS);
}
    // Método actualizado que usa formas realistas de vehículos
    private void drawVehicles() {
        simulationPane.getChildren().removeIf(node -> node.getUserData() != null && node.getUserData().equals("vehicle"));
        vehicleCircles.clear();

        double centerX = SCENE_WIDTH / 2;
        double centerY = SCENE_HEIGHT / 2;
        double laneWidth = ROAD_WIDTH / 2.0;
        double vehicleLength = 40;
        double vehicleWidth = 20;

        for (Intersection intersection : intersections) {
            List<Vehicle> queue = new ArrayList<>(intersection.getVehicleQueue());
            for (int i = 0; i < queue.size(); i++) {
                Vehicle v = queue.get(i);
                Group vehicle = createVehicleShape(v, vehicleLength, vehicleWidth);
                double[] pos = getVehiclePosition(intersection.getId(), centerX, centerY, laneWidth, vehicleLength, i);
                double angle = getVehicleRotation(intersection.getId());

                vehicle.setLayoutX(pos[0]);
                vehicle.setLayoutY(pos[1]);
                vehicle.setRotate(angle);

                simulationPane.getChildren().add(vehicle);
            }
        }
    }

/* 
    private Circle createVehicleCircle(Vehicle v) {
        Circle c = new Circle(12);
        if ("emergency".equalsIgnoreCase(v.getType())) {
            c.setFill(Color.ORANGERED);
            c.setStroke(Color.YELLOW);
            c.setStrokeWidth(3);
        } else {
            c.setFill(Color.DODGERBLUE);
            c.setStroke(Color.BLACK);
            c.setStrokeWidth(2);
        }
        c.setUserData("vehicle");
        return c;
    }*/


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
        Pane markingsPane = new Pane();

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

        // Antes de agregar markingsPane, elimínalo si ya existe
        pane.getChildren().removeIf(node -> node.getUserData() != null && node.getUserData().equals("markingsPane"));
        markingsPane.setUserData("markingsPane");
        pane.getChildren().add(markingsPane);
    }

    /**
     * Creates, positions, and labels all four traffic lights with a clear and simple layout.
     */
    

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
    private void addImprovedLightAndLabel(Pane pane, Group lightGroup, double lightX, double lightY, String labelText, double labelX, double labelY) {
    lightGroup.setLayoutX(lightX);
    lightGroup.setLayoutY(lightY);

    Label label = new Label(labelText);
    label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
    label.setTextFill(Color.WHITE);
    label.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-padding: 8px; -fx-background-radius: 8; -fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 8;");
    label.setLayoutX(labelX);
    label.setLayoutY(labelY);

    pane.getChildren().addAll(lightGroup, label);
}


    /**
     * Creates a single traffic light visual group (the black casing with two lights).
     */
    private Group createTrafficLightVisuals() {
    Group lightGroup = new Group();
    
    // Poste del semáforo
    Rectangle pole = new Rectangle(8, 60);
    pole.setFill(Color.DARKGRAY);
    pole.setX(TRAFFIC_LIGHT_WIDTH / 2 - 4);
    pole.setY(TRAFFIC_LIGHT_HEIGHT);
    
    // Casing del semáforo más realista
    Rectangle casing = new Rectangle(TRAFFIC_LIGHT_WIDTH, TRAFFIC_LIGHT_HEIGHT);
    casing.setFill(Color.BLACK);
    casing.setStroke(Color.DARKGRAY);
    casing.setStrokeWidth(2);
    casing.setArcWidth(8);
    casing.setArcHeight(8);
    
    // Visera/sombra para cada luz
    Rectangle redShade = new Rectangle(TRAFFIC_LIGHT_WIDTH - 4, 8);
    redShade.setFill(Color.DARKGRAY);
    redShade.setX(2);
    redShade.setY(8);
    
    Rectangle greenShade = new Rectangle(TRAFFIC_LIGHT_WIDTH - 4, 8);
    greenShade.setFill(Color.DARKGRAY);
    greenShade.setX(2);
    greenShade.setY(LIGHT_RADIUS * 3 + LIGHT_SPACING * 2 - 8);

    // Luces con mejor efecto visual
    Circle redLight = new Circle(TRAFFIC_LIGHT_WIDTH / 2, LIGHT_RADIUS + LIGHT_SPACING, LIGHT_RADIUS - 2);
    redLight.setFill(Color.DARKRED.desaturate());
    redLight.setStroke(Color.BLACK);
    redLight.setStrokeWidth(1);
    
    Circle greenLight = new Circle(TRAFFIC_LIGHT_WIDTH / 2, LIGHT_RADIUS * 3 + LIGHT_SPACING * 2, LIGHT_RADIUS - 2);
    greenLight.setFill(Color.DARKGREEN.desaturate());
    greenLight.setStroke(Color.BLACK);
    greenLight.setStrokeWidth(1);

    lightGroup.getChildren().addAll(pole, casing, redShade, greenShade, redLight, greenLight);
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
                // Luz verde encendida con efecto de brillo
                visuals.greenLight.setFill(Color.LIMEGREEN);
                visuals.greenLight.setEffect(new javafx.scene.effect.Glow(0.8));
                
                // Luz roja apagada
                visuals.redLight.setFill(Color.DARKRED.desaturate());
                visuals.redLight.setEffect(null);
            } else {
                // Luz roja encendida con efecto de brillo
                visuals.redLight.setFill(Color.RED);
                visuals.redLight.setEffect(new javafx.scene.effect.Glow(0.8));
                
                // Luz verde apagada
                visuals.greenLight.setFill(Color.DARKGREEN.desaturate());
                visuals.greenLight.setEffect(null);
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

        // Nuevo método para crear forma de vehículo más realista
    private Group createVehicleShape(Vehicle v, double length, double width) {
        Group vehicleGroup = new Group();

        // Cuerpo principal del vehículo
        Rectangle body = new Rectangle(length, width);
        body.setArcWidth(5);
        body.setArcHeight(5);

        if ("emergency".equalsIgnoreCase(v.getType())) {
            body.setFill(Color.RED);
            body.setStroke(Color.YELLOW);
            body.setStrokeWidth(2);

            // Luces de emergencia
            Rectangle light1 = new Rectangle(length * 0.3, 3);
            light1.setFill(Color.YELLOW);
            light1.setX(length * 0.1);
            light1.setY(-2);

            Rectangle light2 = new Rectangle(length * 0.3, 3);
            light2.setFill(Color.BLUE);
            light2.setX(length * 0.6);
            light2.setY(-2);

            vehicleGroup.getChildren().addAll(body, light1, light2);
        } else {
            Color[] carColors = {Color.BLUE, Color.GREEN, Color.PURPLE, Color.ORANGE, Color.BROWN, Color.NAVY};
            body.setFill(carColors[Math.abs(v.getId().hashCode()) % carColors.length]);
            body.setStroke(Color.BLACK);
            body.setStrokeWidth(1);
            vehicleGroup.getChildren().add(body);
        }

        // Ventanas
        Rectangle frontWindow = new Rectangle(length * 0.15, width * 0.6);
        frontWindow.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.7));
        frontWindow.setX(length * 0.75);
        frontWindow.setY(width * 0.2);

        Rectangle backWindow = new Rectangle(length * 0.15, width * 0.6);
        backWindow.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.7));
        backWindow.setX(length * 0.1);
        backWindow.setY(width * 0.2);

        // Ruedas
        Circle wheel1 = new Circle(length * 0.2, width + 2, 3);
        wheel1.setFill(Color.BLACK);
        Circle wheel2 = new Circle(length * 0.8, width + 2, 3);
        wheel2.setFill(Color.BLACK);
        Circle wheel3 = new Circle(length * 0.2, -2, 3);
        wheel3.setFill(Color.BLACK);
        Circle wheel4 = new Circle(length * 0.8, -2, 3);
        wheel4.setFill(Color.BLACK);

        vehicleGroup.getChildren().addAll(frontWindow, backWindow, wheel1, wheel2, wheel3, wheel4);
        vehicleGroup.setUserData("vehicle");

        return vehicleGroup;
    }

    // Posiciona vehículos en los carriles
    private double[] getVehiclePosition(String intersectionId, double centerX, double centerY, double laneWidth, double vehicleLength, int index) {
        double x = 0, y = 0;
        double spacing = vehicleLength + 10;

        switch (intersectionId) {
            case "North":
                x = centerX - laneWidth / 2 - vehicleLength / 2;
                y = centerY - ROAD_WIDTH / 2 - spacing * (index + 1);
                break;
            case "South":
                x = centerX + laneWidth / 2 - vehicleLength / 2;
                y = centerY + ROAD_WIDTH / 2 + spacing * (index + 1);
                break;
            case "East":
                x = centerX + ROAD_WIDTH / 2 + spacing * (index + 1);
                y = centerY - laneWidth / 2 - vehicleLength / 2;
                break;
            case "West":
                x = centerX - ROAD_WIDTH / 2 - spacing * (index + 1);
                y = centerY + laneWidth / 2 - vehicleLength / 2;
                break;
        }

        return new double[]{x, y};
    }

    // Rotación de vehículos según dirección
    private double getVehicleRotation(String intersectionId) {
        switch (intersectionId) {
            case "North": return 90;
            case "South": return 270;
            case "East":  return 0;
            case "West":  return 0;
            default: return 0;
        }
    }

    // Simulación de cruce de intersección
    private void animateVehicleCrossing(Vehicle vehicle, String fromIntersection) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(2), e -> {
                for (Intersection intersection : intersections) {
                    intersection.getVehicleQueue().remove(vehicle);
                }
            })
        );
        timeline.play();
    }

    
    private void addPareSigns(Pane simulationPane) {
    double centerX = SCENE_WIDTH / 2;
    double centerY = SCENE_HEIGHT / 2;
    double signOffset = 120; // Distancia del centro de la intersección
    
    // Crear letreros PARE en posiciones más lógicas
    createStopSign(simulationPane, centerX, centerY - signOffset - 60, "PARE"); // Norte
    createStopSign(simulationPane, centerX, centerY + signOffset + 60, "PARE"); // Sur
    createStopSign(simulationPane, centerX - signOffset - 60, centerY, "PARE"); // Oeste
    createStopSign(simulationPane, centerX + signOffset + 60, centerY, "PARE"); // Este
}
    private void createStopSign(Pane pane, double x, double y, String text) {
    Group stopSignGroup = new Group();
    
    // Poste del letrero
    Rectangle pole = new Rectangle(6, 40);
    pole.setFill(Color.DARKGRAY);
    pole.setX(-3);
    pole.setY(25);
    
    // Fondo octagonal del letrero PARE
    // Simulamos octágono con un rectángulo con esquinas muy redondeadas
    Rectangle signBackground = new Rectangle(50, 50);
    signBackground.setFill(Color.RED);
    signBackground.setStroke(Color.WHITE);
    signBackground.setStrokeWidth(3);
    signBackground.setArcWidth(15);
    signBackground.setArcHeight(15);
    signBackground.setX(-25);
    signBackground.setY(-25);
    
    // Texto PARE
    Label pareText = new Label(text);
    pareText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
    pareText.setTextFill(Color.WHITE);
    pareText.setLayoutX(-18);
    pareText.setLayoutY(-8);
    
    stopSignGroup.getChildren().addAll(pole, signBackground, pareText);
    stopSignGroup.setLayoutX(x);
    stopSignGroup.setLayoutY(y);
    
    pane.getChildren().add(stopSignGroup);
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