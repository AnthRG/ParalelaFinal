package app.paralelafinal.escenario2.simulation;

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
        return i;
    }

    private List<Intersection> setupRIntersections() {
        List<Intersection> i = new ArrayList<>();
        i.add(new Intersection("West1"));
        i.add(new Intersection("West2"));
        return i;
    }

    public void addVehicle(String type, String dir, String laneId) {
        Platform.runLater(() -> {
            String vehicleId = "V" + System.currentTimeMillis();
            Intersection i = findIntersectionById(laneId);
            if (i == null) return;
            
            // Get queue index BEFORE adding the vehicle (like SimulationEngine.java does)
            int queueIndex = getQueueIndexForDirection(i, dir);
            
            Vehicle v = new Vehicle(vehicleId, type, dir, laneId, i.getId());
            
            // Calculate spawn position with queue-based offset
            Point2D spawnPos = calculateSpawnPosition(laneId, dir, queueIndex);
            v.setPosition(spawnPos);
            
            // Add vehicle to intersection after setting position
            i.addVehicle(v);
        });
    }
    
    private int getQueueIndexForDirection(Intersection intersection, String direction) {
        // Return the current size of the appropriate queue
        switch(direction) {
            case "right":
                return intersection.getRightVQueue().size();
            case "left":
                return intersection.getLeftVQueue().size();
            case "straight":
            default:
                return intersection.getMidVQueue().size();
        }
    }
    
    private Point2D calculateSpawnPosition(String laneId, String direction, int queueIndex) {
        // Calculate horizontal road positions
        double horizRoad1Y = (SimulationConfig.SCENE_HEIGHT - 2 * SimulationConfig.ROAD_WIDTH * 1.875) / 3 
                           + SimulationConfig.ROAD_WIDTH * 1.875 / 2;
        double horizRoad2Y = horizRoad1Y + SimulationConfig.ROAD_WIDTH * 1.875 
                           + (SimulationConfig.SCENE_HEIGHT - 2 * SimulationConfig.ROAD_WIDTH * 1.875) / 3;
        
        // Calculate Y position based on lane and direction
        double laneWidth = SimulationConfig.ROAD_WIDTH * 1.875 / 6; // 6 lanes total
        double yPos;
        
        if (laneId.startsWith("East")) {
            // Westbound traffic - upper lanes
            double roadCenterY = laneId.equals("East1") ? horizRoad1Y : horizRoad2Y;
            if (direction.equals("left")) {
                yPos = roadCenterY - laneWidth * 2.5;
            } else if (direction.equals("right")) {
                yPos = roadCenterY - laneWidth * 0.5;
            } else {
                yPos = roadCenterY - laneWidth * 1.5;
            }
        } else {
            // Eastbound traffic - lower lanes
            double roadCenterY = laneId.equals("West1") ? horizRoad1Y : horizRoad2Y;
            if (direction.equals("left")) {
                yPos = roadCenterY + laneWidth * 0.5;
            } else if (direction.equals("right")) {
                yPos = roadCenterY + laneWidth * 2.5;
            } else {
                yPos = roadCenterY + laneWidth * 1.5;
            }
        }
        
        // Calculate X position with queue-based offset
        double vehicleSpacing = SimulationConfig.VEHICLE_LENGTH + 25; // Increased spacing
        double xPos;
        
        if (laneId.startsWith("East")) {
            // Westbound: spawn from right, offset further right for each queued vehicle
            xPos = SimulationConfig.SCENE_WIDTH - 50 + (queueIndex * vehicleSpacing);
        } else {
            // Eastbound: spawn from left, offset further left for each queued vehicle
            xPos = 50 - (queueIndex * vehicleSpacing);
        }
        
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
}
