// app.paralelafinal.ui.SimulationPane
package app.paralelafinal.simulation;

import app.paralelafinal.config.SimulationConfig;
import app.paralelafinal.entidades.Intersection;
import app.paralelafinal.entidades.TrafficLight;
import app.paralelafinal.entidades.Vehicle; 
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;




public class SimulationPane {

    private static BorderPane root;
    private static Pane simulationCanvas;
    private static SimulationEngine simulationEngine;
    private static SimulationPane simulationPane;
    private Map<Vehicle, Circle> vehicleNodes = new HashMap<>();
    private Map<String, TrafficLightVisuals> trafficLightVisualsMap;

    public SimulationPane(SimulationEngine engine) throws InterruptedException {
        simulationEngine = engine;
        root = new BorderPane();
        simulationCanvas = new Pane();
        this.trafficLightVisualsMap = new HashMap<>();
        setupUI();
       
        simulationEngine.setUiUpdateCallback(v -> {
            try {
                updateAllVisuals();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
    private void onVehicleAdded(Vehicle v) {
    // crea el círculo (o imagen) en su posición inicial
    Circle c = new Circle(8);
    c.setCenterX(v.getPosition().getX());
    c.setCenterY(v.getPosition().getY());
    // color según tipo
    c.setFill(v.getType().equals("emergency") ? Color.RED : Color.BLUE);
    vehicleNodes.put(v, c);
    root.getChildren().add(c);
}

    public static SimulationEngine getSimulationEngine() {
        return simulationEngine;
    }

    public static SimulationPane getInstance() throws InterruptedException {
        if(simulationPane == null) {
            simulationPane = new SimulationPane(new SimulationEngine());
        }
        return simulationPane;
    }

    private static void setupUI() throws InterruptedException {
        simulationCanvas.setPrefSize(SimulationConfig.SCENE_WIDTH, SimulationConfig.SCENE_HEIGHT);
        simulationCanvas.setStyle("-fx-background-color:rgba(31, 194, 118, 0.99);"); // GREEN background

        // --- Draw all static visual elements ---
        drawRoads(simulationCanvas);
        drawRoadMarkings(simulationCanvas);
        addPareSigns(simulationCanvas);
        //setupTrafficLightsAndLabels(simulationCanvas);

        root.setCenter(simulationCanvas);

        // --- Botón para agregar vehículo aleatorio ---
       // Button addVehicleButton = new Button("Add Vehicle");
       // addVehicleButton.setOnAction(e -> simulationEngine.trafficController.generateRandomVehicle());
       // HBox randomBox = new HBox(10, addVehicleButton);
        //randomBox.setAlignment(Pos.CENTER);
        //randomBox.setPadding(new Insets(10));

        // — Controles de generación manual — 
       /*  Label originLabel = new Label("Intersección:");
        ComboBox<String> originCombo = new ComboBox<>();
        originCombo.getItems().addAll("North", "South", "East", "West");
        originCombo.setValue("North");

        Label typeLabel = new Label("Tipo:");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("normal", "emergency");
        typeCombo.setValue("normal");

        Label dirLabel = new Label("Giro:");
        ComboBox<String> dirCombo = new ComboBox<>();
        dirCombo.getItems().addAll("right", "straight", "left", "u-turn");
        dirCombo.setValue("straight");*/

        // Botón para agregar vehículo manual 
        Button addVehicleButton = new Button("Add Vehicle");
        addVehicleButton.setOnAction(e -> VehicleAddMenu.display(simulationEngine));

        // Creamos un HBox solo con el botón, centrado
        HBox manualControls = new HBox(addVehicleButton);
        manualControls.setAlignment(Pos.CENTER);
        manualControls.setPadding(new Insets(10));

        // Añadimos al layout principal (en lugar del HBox anterior)
        root.setBottom(manualControls);

        // Initial update to draw everything based on the simulation engine's initial state
        updateAllVisuals();
    }

    public BorderPane getRoot() {
        return root;
    }

    /**
     * 
     * este método se encarga de actualizar todos los elementos visuales de la simulación.
     */
    public static void updateAllVisuals() throws InterruptedException {
        //updateTrafficLightVisuals();
        drawVehicles();
    }


    private static void drawRoads(Pane pane) {
        double centerX = SimulationConfig.SCENE_WIDTH / 2;
        double centerY = SimulationConfig.SCENE_HEIGHT / 2;

        Rectangle horizontalRoad = new Rectangle(0, centerY - SimulationConfig.ROAD_WIDTH / 2, SimulationConfig.SCENE_WIDTH, SimulationConfig.ROAD_WIDTH);
        horizontalRoad.setFill(Color.DARKGRAY);

        Rectangle verticalRoad = new Rectangle(centerX - SimulationConfig.ROAD_WIDTH / 2, 0, SimulationConfig.ROAD_WIDTH, SimulationConfig.SCENE_HEIGHT);
        verticalRoad.setFill(Color.DARKGRAY);

        pane.getChildren().addAll(horizontalRoad, verticalRoad);
    }

    private static void drawRoadMarkings(Pane pane) {
        double centerX = SimulationConfig.SCENE_WIDTH / 2;
        double centerY = SimulationConfig.SCENE_HEIGHT / 2;
        Pane markingsPane = new Pane();

        // --- DOUBLE SOLID YELLOW CENTER LINES ---
        markingsPane.getChildren().add(new Rectangle(0, centerY - SimulationConfig.CENTER_LINE_GAP / 2 - SimulationConfig.LINE_THICKNESS / 2, centerX - SimulationConfig.ROAD_WIDTH / 2, SimulationConfig.LINE_THICKNESS));
        markingsPane.getChildren().add(new Rectangle(0, centerY + SimulationConfig.CENTER_LINE_GAP / 2 - SimulationConfig.LINE_THICKNESS / 2, centerX - SimulationConfig.ROAD_WIDTH / 2, SimulationConfig.LINE_THICKNESS));
        markingsPane.getChildren().add(new Rectangle(centerX + SimulationConfig.ROAD_WIDTH / 2, centerY - SimulationConfig.CENTER_LINE_GAP / 2 - SimulationConfig.LINE_THICKNESS / 2, SimulationConfig.SCENE_WIDTH - (centerX + SimulationConfig.ROAD_WIDTH / 2), SimulationConfig.LINE_THICKNESS));
        markingsPane.getChildren().add(new Rectangle(centerX + SimulationConfig.ROAD_WIDTH / 2, centerY + SimulationConfig.CENTER_LINE_GAP / 2 - SimulationConfig.LINE_THICKNESS / 2, SimulationConfig.SCENE_WIDTH - (centerX + SimulationConfig.ROAD_WIDTH / 2), SimulationConfig.LINE_THICKNESS));
        markingsPane.getChildren().add(new Rectangle(centerX - SimulationConfig.CENTER_LINE_GAP / 2 - SimulationConfig.LINE_THICKNESS / 2, 0, SimulationConfig.LINE_THICKNESS, centerY - SimulationConfig.ROAD_WIDTH / 2));
        markingsPane.getChildren().add(new Rectangle(centerX + SimulationConfig.CENTER_LINE_GAP / 2 - SimulationConfig.LINE_THICKNESS / 2, 0, SimulationConfig.LINE_THICKNESS, centerY - SimulationConfig.ROAD_WIDTH / 2));
        markingsPane.getChildren().add(new Rectangle(centerX - SimulationConfig.CENTER_LINE_GAP / 2 - SimulationConfig.LINE_THICKNESS / 2, centerY + SimulationConfig.ROAD_WIDTH / 2, SimulationConfig.LINE_THICKNESS, SimulationConfig.SCENE_HEIGHT - (centerY + SimulationConfig.ROAD_WIDTH / 2)));
        markingsPane.getChildren().add(new Rectangle(centerX + SimulationConfig.CENTER_LINE_GAP / 2 - SimulationConfig.LINE_THICKNESS / 2, centerY + SimulationConfig.ROAD_WIDTH / 2, SimulationConfig.LINE_THICKNESS, SimulationConfig.SCENE_HEIGHT - (centerY + SimulationConfig.ROAD_WIDTH / 2)));

        markingsPane.getChildren().stream()
                .filter(node -> node instanceof Rectangle)
                .forEach(node -> ((Rectangle) node).setFill(Color.YELLOW));
        pane.getChildren().removeIf(node -> node.getUserData() != null && node.getUserData().equals("markingsPane"));
        markingsPane.setUserData("markingsPane");
        pane.getChildren().add(markingsPane);
    }

    private void setupTrafficLightsAndLabels(Pane pane) {
        double centerX = SimulationConfig.SCENE_WIDTH / 2;
        double centerY = SimulationConfig.SCENE_HEIGHT / 2;
        double offset = SimulationConfig.TRAFFIC_LIGHT_OFFSET;

        String southBoundText = "⬇ Tráfico Sur";
        Group tl_NorthGroup = createTrafficLightVisualsGroup();
        double tl_NorthX = centerX - SimulationConfig.ROAD_WIDTH / 2 - SimulationConfig.TRAFFIC_LIGHT_WIDTH - offset;
        double tl_NorthY = centerY - SimulationConfig.ROAD_WIDTH / 2 - SimulationConfig.TRAFFIC_LIGHT_HEIGHT - offset;
        addImprovedLightAndLabel(pane, tl_NorthGroup, tl_NorthX, tl_NorthY, southBoundText, tl_NorthX - 80, tl_NorthY - 30);
        trafficLightVisualsMap.put("North", new TrafficLightVisuals((Circle) tl_NorthGroup.getChildren().get(4), (Circle) tl_NorthGroup.getChildren().get(5)));

        String northBoundText = "⬆ Tráfico Norte";
        Group tl_SouthGroup = createTrafficLightVisualsGroup();
        double tl_SouthX = centerX + SimulationConfig.ROAD_WIDTH / 2 + offset;
        double tl_SouthY = centerY + SimulationConfig.ROAD_WIDTH / 2 + offset;
        addImprovedLightAndLabel(pane, tl_SouthGroup, tl_SouthX, tl_SouthY, northBoundText, tl_SouthX - 50, tl_SouthY + SimulationConfig.TRAFFIC_LIGHT_HEIGHT + 70);
        trafficLightVisualsMap.put("South", new TrafficLightVisuals((Circle) tl_SouthGroup.getChildren().get(4), (Circle) tl_SouthGroup.getChildren().get(5)));

        String westBoundText = "⬅ Tráfico Oeste";
        Group tl_EastGroup = createTrafficLightVisualsGroup();
        double tl_EastX = centerX + SimulationConfig.ROAD_WIDTH / 2 + offset;
        double tl_EastY = centerY - SimulationConfig.ROAD_WIDTH / 2 - SimulationConfig.TRAFFIC_LIGHT_HEIGHT - offset;
        addImprovedLightAndLabel(pane, tl_EastGroup, tl_EastX, tl_EastY, westBoundText, tl_EastX - 50, tl_EastY - 30);
        trafficLightVisualsMap.put("East", new TrafficLightVisuals((Circle) tl_EastGroup.getChildren().get(4), (Circle) tl_EastGroup.getChildren().get(5)));

        String eastBoundText = "➡ Tráfico Este";
        Group tl_WestGroup = createTrafficLightVisualsGroup();
        double tl_WestX = centerX - SimulationConfig.ROAD_WIDTH / 2 - SimulationConfig.TRAFFIC_LIGHT_WIDTH - offset;
        double tl_WestY = centerY + SimulationConfig.ROAD_WIDTH / 2 + offset;
        addImprovedLightAndLabel(pane, tl_WestGroup, tl_WestX, tl_WestY, eastBoundText, tl_WestX - 80, tl_WestY + SimulationConfig.TRAFFIC_LIGHT_HEIGHT + 70);
        trafficLightVisualsMap.put("West", new TrafficLightVisuals((Circle) tl_WestGroup.getChildren().get(4), (Circle) tl_WestGroup.getChildren().get(5)));
    }

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

    private Group createTrafficLightVisualsGroup() {
        Group lightGroup = new Group();

        Rectangle pole = new Rectangle(8, 60);
        pole.setFill(Color.DARKGRAY);
        pole.setX(SimulationConfig.TRAFFIC_LIGHT_WIDTH / 2 - 4);
        pole.setY(SimulationConfig.TRAFFIC_LIGHT_HEIGHT);

        Rectangle casing = new Rectangle(SimulationConfig.TRAFFIC_LIGHT_WIDTH, SimulationConfig.TRAFFIC_LIGHT_HEIGHT);
        casing.setFill(Color.BLACK);
        casing.setStroke(Color.DARKGRAY);
        casing.setStrokeWidth(2);
        casing.setArcWidth(8);
        casing.setArcHeight(8);

        Rectangle redShade = new Rectangle(SimulationConfig.TRAFFIC_LIGHT_WIDTH - 4, 8);
        redShade.setFill(Color.DARKGRAY);
        redShade.setX(2);
        redShade.setY(8);

        Rectangle greenShade = new Rectangle(SimulationConfig.TRAFFIC_LIGHT_WIDTH - 4, 8);
        greenShade.setFill(Color.DARKGRAY);
        greenShade.setX(2);
        greenShade.setY(SimulationConfig.LIGHT_RADIUS * 3 + SimulationConfig.LIGHT_SPACING * 2 - 8);

        Circle redLight = new Circle(SimulationConfig.TRAFFIC_LIGHT_WIDTH / 2, SimulationConfig.LIGHT_RADIUS + SimulationConfig.LIGHT_SPACING, SimulationConfig.LIGHT_RADIUS - 2);
        redLight.setFill(Color.DARKRED.desaturate());
        redLight.setStroke(Color.BLACK);
        redLight.setStrokeWidth(1);

        Circle greenLight = new Circle(SimulationConfig.TRAFFIC_LIGHT_WIDTH / 2, SimulationConfig.LIGHT_RADIUS * 3 + SimulationConfig.LIGHT_SPACING * 2, SimulationConfig.LIGHT_RADIUS - 2);
        greenLight.setFill(Color.DARKGREEN.desaturate());
        greenLight.setStroke(Color.BLACK);
        greenLight.setStrokeWidth(1);

        lightGroup.getChildren().addAll(pole, casing, redShade, greenShade, redLight, greenLight);
        return lightGroup;
    }

    private static void drawVehicles() {
        // Brra todos los sprites de vehículos anteriores
        simulationCanvas.getChildren().removeIf(node ->
            "vehicle".equals(node.getUserData())
        );

        //  Calcula una vez el centro de la intersección
        Point2D center = new Point2D(
            SimulationConfig.SCENE_WIDTH  / 2.0,
            SimulationConfig.SCENE_HEIGHT / 2.0
        );

        //  Por cada intersección y cada vehículo en su cola...
        for (Intersection intersection : simulationEngine.getIntersections()) {
            for (Vehicle v : intersection.getVehicleQueue()) {
                // Crea el sprite y márcalo
                Group sprite = createVehicleShape(v);
                sprite.setUserData("vehicle");

                //  Obtiene el vector unitario de movimiento
                Point2D heading = simulationEngine.calculateMovementVector(intersection, v, center);

                //  Convierte ese vector a un ángulo en grados [0,360)
                double rawAngle = Math.toDegrees(Math.atan2(heading.getY(), heading.getX()));
                double angle    = (rawAngle + 360) % 360;

                //  Aplica la rotación al sprite
                sprite.setRotate(angle);

                //  Y lo posiciona según la coordenada lógica del vehículo
                Point2D pos = v.getPosition();
                sprite.setLayoutX(pos.getX());
                sprite.setLayoutY(pos.getY());

                //  Finalmente, se añade al canvas de simulación
                simulationCanvas.getChildren().add(sprite);
            }
        }
    }

    private static Group createVehicleShape(Vehicle v) {
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

    public static double[] getVehiclePosition(String intersectionId, double centerX, double centerY, double laneWidth, int index) {
        double x = 0, y = 0;
        double spacing = SimulationConfig.VEHICLE_SPACING; // Use spacing from config

        switch (intersectionId) {
            case "North":
                x = centerX - laneWidth / 2 - SimulationConfig.VEHICLE_LENGTH / 2;
                y = centerY - SimulationConfig.ROAD_WIDTH  / 2 - spacing * (index + 1);
                break;
            case "South":
                x = centerX + laneWidth / 2 - SimulationConfig.VEHICLE_LENGTH / 2;
                y = centerY + SimulationConfig.ROAD_WIDTH / 2 + spacing * (index + 1);
                break;
            case "East":
                x = centerX + SimulationConfig.ROAD_WIDTH / 2 + spacing * (index + 1);
                y = centerY - laneWidth / 2 - SimulationConfig.VEHICLE_LENGTH / 2;
                break;
            case "West":
                x = centerX - SimulationConfig.ROAD_WIDTH / 2 - spacing * (index + 1);
                y = centerY + laneWidth / 2 - SimulationConfig.VEHICLE_LENGTH / 2;
                break;
        }

        return new double[]{x, y};
    }

   private static double getVehicleRotation(String intersectionId) {
        switch(intersectionId) {
            case "North": return  90;
            case "South": return 270;
            case "East":  return   0;
            case "West":  return 180;  
            default:      return   0;
        }
}


    private double[] getFinishedVehiclePosition(String intersectionId, double centerX, double centerY, double laneWidth) {
        double x = 0, y = 0;
        double spacing = SimulationConfig.VEHICLE_SPACING; // Use spacing from config

        switch (intersectionId) {
            case "North":
                x = centerX + laneWidth / 2 - SimulationConfig.VEHICLE_LENGTH / 2;
                y = centerY - SimulationConfig.ROAD_WIDTH / 2 - spacing;
                break;
            case "South":
                x = centerX - laneWidth / 2 - SimulationConfig.VEHICLE_LENGTH / 2;
                y = centerY + SimulationConfig.ROAD_WIDTH / 2 + spacing;
                break;
            case "East":
                x = centerX + SimulationConfig.ROAD_WIDTH / 2 + spacing;
                y = centerY + laneWidth / 2 - SimulationConfig.VEHICLE_LENGTH / 2;
                break;
            case "West":
                x = centerX - SimulationConfig.ROAD_WIDTH / 2 - spacing;
                y = centerY - laneWidth / 2 - SimulationConfig.VEHICLE_LENGTH / 2;

                break;
        }

        return new double[]{x, y};
    }

    public static String getFinishedVehicleLocation(Vehicle finishedVehicle, String finishedLoc) {
        if (finishedLoc.equalsIgnoreCase("North")) {
            return switch (finishedVehicle.getDirection()) {
                // "right", "straight", "left", "u-turn"
                case "right" -> "East";
                case "left" -> "West";
                case "straight" -> "South";
                default -> "North";
            };
        }else if(finishedLoc.equalsIgnoreCase("South")){
            return switch (finishedVehicle.getDirection()) {
                // "right", "straight", "left", "u-turn"
                case "left" -> "East";
                case "right" -> "West";
                case "straight" -> "North";
                default -> "South";
            };

        }else if(finishedLoc.equalsIgnoreCase("East")){
            return switch (finishedVehicle.getDirection()) {
                // "right", "straight", "left", "u-turn"
                case "right" -> "North";
                case "left" -> "South";
                case "straight" -> "West";
                default -> "East";
            };

        }else { // west
            return switch (finishedVehicle.getDirection()) {
                // "right", "straight", "left", "u-turn"
                case "right" -> "South";
                case "left" -> "North";
                case "straight" -> "East";
                default -> "West";
            };

        }

    }

    private double getFinishedVehicleRotation(String intersectionId) {
        if (intersectionId.equalsIgnoreCase("North") || intersectionId.equalsIgnoreCase("South")) {
            return 90;
        }else {
            return 0;
        }
    }

    public void drawFinishedVehicles(Vehicle finishedVehicle, String intersectionid) throws InterruptedException {
        simulationCanvas.getChildren().removeIf(node -> node.getUserData() != null && node.getUserData().equals("vehicle"));

        double centerX = SimulationConfig.SCENE_WIDTH / 2;
        double centerY = SimulationConfig.SCENE_HEIGHT / 2;
        double laneWidth = SimulationConfig.ROAD_WIDTH / 2.0;

        String finishedLoc = getFinishedVehicleLocation(finishedVehicle, intersectionid);

        Group vehicle = createVehicleShape(finishedVehicle);
        double[] pos = getFinishedVehiclePosition(finishedLoc, centerX, centerY, laneWidth);
        double angle = getFinishedVehicleRotation(finishedLoc);

        vehicle.setLayoutX(pos[0]);
        vehicle.setLayoutY(pos[1]);
        vehicle.setRotate(angle);

        simulationCanvas.getChildren().add(vehicle);


    }

    private static void addPareSigns(Pane simulationPane) {
        double centerX = SimulationConfig.SCENE_WIDTH / 2;
        double centerY = SimulationConfig.SCENE_HEIGHT / 2;
        double signOffset = 5 + (SimulationConfig.ROAD_WIDTH / 2);

        createStopSign(simulationPane, centerX + signOffset + 20, centerY + signOffset/2 -5, "PARE");
        createStopSign(simulationPane, centerX + signOffset + 20, centerY - signOffset * 2 + 20, "PARE");
        createStopSign(simulationPane, centerX - signOffset - 20, centerY + signOffset/2 -5, "PARE");
        createStopSign(simulationPane, centerX - signOffset - 20, centerY - signOffset * 2 + 20, "PARE");
    }

    private static void createStopSign(Pane pane, double x, double y, String text) {
        Group stopSignGroup = new Group();

        Rectangle pole = new Rectangle(6, 40);
        pole.setFill(Color.GRAY);
        pole.setX(-3);
        pole.setY(25);

        Rectangle signBackground = new Rectangle(50, 50);
        signBackground.setFill(Color.RED);
        signBackground.setStroke(Color.WHITE);
        signBackground.setStrokeWidth(3);
        signBackground.setArcWidth(15);
        signBackground.setArcHeight(15);
        signBackground.setX(-25);
        signBackground.setY(-25);

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
}