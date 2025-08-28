package app.paralelafinal.escenario2.simulation;
import app.paralelafinal.config.LanePositionAdjustment;
import app.paralelafinal.config.SimulationConfig;
import app.paralelafinal.escenario2.entidades.Vehicle;
import app.paralelafinal.escenario2.controladores.TrafficController;
import app.paralelafinal.escenario2.entidades.Intersection;
import javafx.application.Platform;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;


public class SimulationEngine2 {
    private final List<Intersection> intersections;
    private final TrafficController trafficController;

    // Animation loop for UI updates
    private Timeline animationLoop;
    private Consumer<Void> uiUpdateCallback;

    // Add field to track last added lane
    private String lastAddedLaneId;

    public SimulationEngine2() {
        this.intersections = new ArrayList<>();
        List<Intersection> r = setupRIntersections();
        List<Intersection> l = setupLIntersections();
        intersections.addAll(r);
        intersections.addAll(l);
        this.trafficController = new TrafficController(r, l);
    }

    private List<Intersection> setupLIntersections() {
        List<Intersection> i = new ArrayList<>();
        i.add(new Intersection("East1"));
        i.add(new Intersection("East2")); 
        i.add(new Intersection("East3"));
        return i;
    }

    private List<Intersection> setupRIntersections() {
        List<Intersection> i = new ArrayList<>();
        i.add(new Intersection("West1"));
        i.add(new Intersection("West2")); 
        i.add(new Intersection("West3"));
        return i;
    }

    private int getQueueIndexForDirection(Intersection intersection, String direction) {
        switch(direction.toLowerCase()) {
            case "right":
            case "right-south-first":
            case "right-south-second":
            case "right-north-first":
            case "right-north-second":
                return intersection.getRightVQueue().size();
            case "left":
            case "left-north-first":
            case "left-north-second":
            case "left-south-first":
            case "left-south-second":
                // Count all left-based vehicles in left queue
                int leftCount = 0;
                for (Vehicle v : intersection.getLeftVQueue()) {
                    String vDir = v.getDirection().toLowerCase();
                    if (vDir.startsWith("left") || vDir.startsWith("u-turn")) {
                        leftCount++;
                    }
                }
                return leftCount;
            case "straight":
                return intersection.getMidVQueue().size();
            case "u-turn":
            case "u-turn-second":
                // U-turn vehicles are in the UTurnVQueue
                return intersection.getUTurnVQueue().size();
            default:
                throw new IllegalArgumentException("Invalid direction: " + direction);
        }
    }
    
    public void addVehicle(String type, String dir, String laneId) {
        lastAddedLaneId = laneId;
       
        String vehicleId = "V" + System.currentTimeMillis();
        Intersection intersection = findIntersectionById(laneId);
        if (intersection == null) return;

        Vehicle vehicle = new Vehicle(vehicleId, type, dir, laneId, intersection.getId());

        int queueIndex = getQueueIndexForDirection(intersection, dir);
        Point2D spawnPos = calculateSpawnPosition(laneId, dir, queueIndex);
        
        // Verificar que no haya colisión con vehículos existentes
        if (isPositionOccupied(spawnPos, intersection)) {
            System.out.println("[WARNING] Cannot add vehicle at position (" + 
                              spawnPos.getX() + ", " + spawnPos.getY() + 
                              ") - Position already occupied!");
            return; // No agregar el vehículo si la posición está ocupada
        }
        
        vehicle.setPosition(spawnPos);

        // Only queue the final UI update
        Platform.runLater(() -> {
            switch (dir.toLowerCase()) {
                case "left":
                case "left-north-first":
                case "left-north-second":
                case "left-south-first":
                case "left-south-second":
                    // All left-based movements start from left queue
                    intersection.getLeftVQueue().add(vehicle);
                    break;
                case "right":
                case "right-south-first":
                case "right-south-second":
                case "right-north-first":
                case "right-north-second":
                    // All right-based movements start from right queue
                    intersection.getRightVQueue().add(vehicle);
                    break;
                case "straight":
                    intersection.getMidVQueue().add(vehicle);
                    break;
                case "u-turn":
                case "u-turn-second":
                    // U-turn vehicles go in the UTurnVQueue
                    intersection.getUTurnVQueue().add(vehicle);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid direction: " + dir);
            }
        });
    }

    private Point2D calculateSpawnPosition(String laneId, String direction, int queueIndex) {
        double horizRoadY = (SimulationConfig.SCENE_HEIGHT - SimulationConfig.ROAD_WIDTH) / 2;
        double laneWidth = SimulationConfig.ROAD_WIDTH / 3;
        double yPos;

       
        switch (direction.toLowerCase()) {
            case "left":
            case "left-north-first":
            case "left-north-second":
            case "left-south-first":
            case "left-south-second":
                if (laneId.startsWith("East")) {
                    yPos = horizRoadY + laneWidth * 2.5 + LanePositionAdjustment.EAST_LEFT_OFFSET;
                } else {
                    yPos = horizRoadY + laneWidth * 0.5 + LanePositionAdjustment.WEST_LEFT_OFFSET;
                }
                break;
            case "right":
            case "right-south-first":
            case "right-south-second":
            case "right-north-first":
            case "right-north-second":
                if (laneId.startsWith("East")) {
                    yPos = horizRoadY + laneWidth * 0.5 + LanePositionAdjustment.EAST_RIGHT_OFFSET;
                } else {
                    yPos = horizRoadY + laneWidth * 2.5 + LanePositionAdjustment.WEST_RIGHT_OFFSET;
                }
                break;
            case "straight":
                yPos = horizRoadY + laneWidth * 1.5 + (laneId.startsWith("East") 
                    ? LanePositionAdjustment.EAST_STRAIGHT_OFFSET 
                    : LanePositionAdjustment.WEST_STRAIGHT_OFFSET);
                break;
            case "u-turn":
                // U-turn vehicles use the left lane position
                if (laneId.startsWith("East")) {
                    yPos = horizRoadY + laneWidth * 2.5 + LanePositionAdjustment.EAST_U_TURN_OFFSET;
                } else {
                    yPos = horizRoadY + laneWidth * 0.5 + LanePositionAdjustment.WEST_U_TURN_OFFSET;
                }
                break;
            case "u-turn-second":
                // Second U-turn vehicles use slightly different position
                if (laneId.startsWith("East")) {
                    yPos = horizRoadY + laneWidth * 2.5 + LanePositionAdjustment.EAST_U_TURN_2ND_OFFSET;
                } else {
                    yPos = horizRoadY + laneWidth * 0.5 + LanePositionAdjustment.WEST_U_TURN_2ND_OFFSET;
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid direction: " + direction);
        }

        double vehicleSpacing = SimulationConfig.VEHICLE_LENGTH + 25;
        double xPos = laneId.startsWith("East")
            ? SimulationConfig.SCENE_WIDTH - 50 + (queueIndex * vehicleSpacing) 
            : 50 - (queueIndex * vehicleSpacing); 

        return new Point2D(xPos, yPos);
    }

    private Intersection findIntersectionById(String id) {
        for (Intersection i : intersections) {
            if (i.getId().equalsIgnoreCase(id)) return i;
        }
        return null;
    }

    private Intersection getEastIntersection() {
        for(Intersection i : intersections) {
            if(i.getId().startsWith("East")) {
                return i;
            }
        }
        return null;
    }

    private Intersection getWestIntersection() {
        for(Intersection i : intersections) {
            if(i.getId().startsWith("West")) {
                return i;
            }
        }
        return null;
    }

    // Expose start/stop for the traffic controller
    public void start() {
        this.trafficController.startControl();
        // Run a UI tick loop similar to Scenario 1 to request redraws
        animationLoop = new Timeline(new KeyFrame(
                Duration.millis(SimulationConfig.VEHICLE_UPDATE_INTERVAL_MS),
                event -> {
                    if (uiUpdateCallback != null) {
                        uiUpdateCallback.accept(null);
                    }
                }
        ));
        animationLoop.setCycleCount(Timeline.INDEFINITE);
        animationLoop.play();
    }

    public void stop() {
        if (animationLoop != null) {
            animationLoop.stop();
        }
        this.trafficController.stopControl();
    }

    public void setUiUpdateCallback(Consumer<Void> uiUpdateCallback) {
        this.uiUpdateCallback = uiUpdateCallback;
    }

    public List<Intersection> getIntersections() {
        return intersections;
    }

    // Método para verificar si una posición está ocupada por otro vehículo
    private boolean isPositionOccupied(Point2D position, Intersection intersection) {
        double minDistance = SimulationConfig.VEHICLE_LENGTH + 10; // Distancia mínima entre vehículos
        
        // Verificar en TODAS las intersecciones para mayor seguridad
        for (Intersection checkIntersection : intersections) {
            // Verificar en todas las colas
            List<Vehicle> allVehicles = new ArrayList<>();
            allVehicles.addAll(checkIntersection.getMidVQueue());
            allVehicles.addAll(checkIntersection.getRightVQueue());
            allVehicles.addAll(checkIntersection.getLeftVQueue());
            allVehicles.addAll(checkIntersection.getUTurnVQueue());
            
            for (Vehicle existingVehicle : allVehicles) {
                if (existingVehicle.getPosition() != null) {
                    double distance = position.distance(existingVehicle.getPosition());
                    
                    // Si la distancia es menor que el mínimo, la posición está ocupada
                    if (distance < minDistance) {
                        System.out.println("[COLLISION CHECK] Position conflict detected!");
                        System.out.println("  New vehicle position: (" + position.getX() + ", " + position.getY() + ")");
                        System.out.println("  Existing vehicle " + existingVehicle.getId() + 
                                         " at: (" + existingVehicle.getPosition().getX() + 
                                         ", " + existingVehicle.getPosition().getY() + ")");
                        System.out.println("  Distance: " + distance + " (min required: " + minDistance + ")");
                        return true;
                    }
                }
            }
        }
        
        return false; // La posición está libre
    }

    // Debug method to print U-turn vehicle status
    public void debugUTurnVehicles() {
        System.out.println("=== U-TURN DEBUG INFO ===");
        for (Intersection intersection : intersections) {
            System.out.println("Intersection: " + intersection.getId());
            System.out.println("  UTurn Queue size: " + intersection.getUTurnVQueue().size());
            for (Vehicle v : intersection.getUTurnVQueue()) {
                System.out.println("    Vehicle " + v.getId() + 
                    " - Direction: " + v.getDirection() + 
                    " - Phase: " + v.getUTurnPhase() + 
                    " - Position: " + (v.getPosition() != null ? 
                        "(" + v.getPosition().getX() + ", " + v.getPosition().getY() + ")" : "null"));
            }
        }
        System.out.println("========================");
    }
}
