package app.paralelafinal.simulation;

import app.paralelafinal.config.SimulationConfig;
import app.paralelafinal.controladores.TrafficController;
import app.paralelafinal.entidades.Intersection;
import app.paralelafinal.entidades.Vehicle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static app.paralelafinal.simulation.SimulationPane.getVehiclePosition;

/**
 * The core engine for the traffic simulation.
 * This class manages the simulation state, including intersections and vehicles,
 * controls the simulation loop, and communicates with the UI for updates.
 */
public class SimulationEngine {

    // --- Simulation State ---
    private final List<Intersection> intersections;
    private final TrafficController trafficController;

    // --- Animation & Timing ---
    private Timeline animationLoop;
    private Consumer<Void> uiUpdateCallback;

    // --- Vehicle Movement Constants ---
    private static final double VEHICLE_SPEED = 50.0; // Speed in pixels per update frame.
    private static final double SAFE_FOLLOWING_GAP = 5.0; // Minimum space between vehicles.

    /**
     * Constructs the SimulationEngine, setting up the intersections and traffic controller.
     */
    public SimulationEngine() {
        this.intersections = new ArrayList<>();
        setupIntersections();
        this.trafficController = new TrafficController(intersections);
    }

    /**
     * Initializes the four intersections (North, South, East, West).
     */
    private void setupIntersections() {
        intersections.add(new Intersection("North"));
        intersections.add(new Intersection("South"));
        intersections.add(new Intersection("East"));
        intersections.add(new Intersection("West"));
    }

    /**
     * Starts the simulation.
     * This begins the traffic controller's logic and starts the main animation loop
     * for moving vehicles and updating the UI.
     */
    public void startSimulation() {
        trafficController.startControl();

        animationLoop = new Timeline(new KeyFrame(
                Duration.millis(SimulationConfig.VEHICLE_UPDATE_INTERVAL_MS),
                event -> {
                    updateVehiclePositions();
                    if (uiUpdateCallback != null) {
                        uiUpdateCallback.accept(null);
                    }
                }
        ));
        animationLoop.setCycleCount(Timeline.INDEFINITE);
        animationLoop.play();
    }

    /**
     * Stops the simulation, halting the traffic controller and the animation loop.
     */
    public void stopSimulation() {
        if (animationLoop != null) {
            animationLoop.stop();
        }
        if (trafficController != null) {
            trafficController.stopControl();
        }
    }

    /**
     * Adds a new vehicle to a specific intersection.
     * This method calculates the vehicle's initial position and adds it to the correct queue.
     * It ensures the operation is run on the JavaFX Application Thread for UI safety.
     *
     * @param type The type of vehicle ("normal", "emergency").
     * @param direction The intended maneuver ("straight", "left", "right").
     * @param intersectionId The ID of the intersection where the vehicle should start.
     */
    public void addVehicle(String type, String direction, String intersectionId) {
        Platform.runLater(() -> {
            findIntersectionById(intersectionId).ifPresent(intersection -> {
                // Create the new vehicle.
                String vehicleId = "V" + System.currentTimeMillis();
                Vehicle newVehicle = new Vehicle(vehicleId, type, direction, false);

                // Calculate the vehicle's starting position at the edge of the simulation area.
                // This assumes a dependency on a UI class (SimulationPane) for position calculation,
                // which is not ideal but retained from the original structure.
                double centerX = SimulationConfig.SCENE_WIDTH / 2.0;
                double centerY = SimulationConfig.SCENE_HEIGHT / 2.0;
                double laneWidth = SimulationConfig.ROAD_WIDTH / 2.0;
                int queueIndex = intersection.getVehicleQueue().size();

                // Note: Ideally, this position calculation should not depend on a UI class.
                double[] pos = getVehiclePosition(intersection.getId(), centerX, centerY, laneWidth, queueIndex);
                newVehicle.setPosition(new Point2D(pos[0], pos[1]));

                // Add the vehicle to the intersection's queue.
                intersection.addVehicle(newVehicle);
            });

            // Force a UI refresh to show the new vehicle.
            if (uiUpdateCallback != null) {
                uiUpdateCallback.accept(null);
            }
        });
    }

    /**
     * The main logic loop for updating the position of every vehicle in the simulation.
     */
    private void updateVehiclePositions() {
        // Define the geometry of the intersection area.
        final Point2D center = new Point2D(SimulationConfig.SCENE_WIDTH / 2.0, SimulationConfig.SCENE_HEIGHT / 2.0);
        final double stopLineDistance = (SimulationConfig.ROAD_WIDTH / 2.0) + (SimulationConfig.VEHICLE_LENGTH / 2.0);
        final double removalThreshold = stopLineDistance + SimulationConfig.VEHICLE_LENGTH;

        for (Intersection intersection : intersections) {
            processVehiclesForIntersection(intersection, center, stopLineDistance, removalThreshold);
        }


    }

    /**
     * Processes the movement for all vehicles waiting at a single intersection.
     */
    private void processVehiclesForIntersection(Intersection intersection, Point2D center, double stopLineDist, double removeDist) {
        boolean isGreenLight = intersection.hasGreenLight();

        // Create a snapshot of the queue to avoid modification issues while iterating.
        List<Vehicle> orderedVehicles = new ArrayList<>(intersection.getVehicleQueue());

        for (int i = 0; i < orderedVehicles.size(); i++) {
            Vehicle currentVehicle = orderedVehicles.get(i);
            if (i == 0) {
                handleLeadVehicle(currentVehicle, intersection, isGreenLight, center, stopLineDist, removeDist);
            } else {
                Vehicle precedingVehicle = orderedVehicles.get(i - 1);
                handleFollowingVehicle(currentVehicle, precedingVehicle, intersection, center);
            }
        }
    }

    /**
     * Handles the movement logic for the first vehicle in a queue.
     */
    /**
     * Handles the movement for the first vehicle in a queue.
     */
    private void handleLeadVehicle(Vehicle vehicle, Intersection intersection, boolean isGreen, Point2D center, double stopLineDist, double removeDist) {
        // If the light is green, the vehicle is free to move.
        if (isGreen) {
            double distanceToCenter = vehicle.getPosition().distance(center);

            // Case 1: Vehicle has cleared the intersection and can be removed.
            if (distanceToCenter > removeDist) {
                intersection.removeNextVehicle();
                return;
            }

            // Case 2: Vehicle is clear to move through the intersection.
            Point2D movementVector = calculateMovementVector(intersection, vehicle, center);
            vehicle.move(movementVector);

        } else {
            // If the light is RED, force the vehicle to the stop line position.
            // This ensures it stops at the correct spot instead of just freezing past the line.

            // Calculate the exact position where the first vehicle should stop.
            // We reuse your static helper method for this.
            double[] exactStopPosition = getVehiclePosition(
                    intersection.getId(),
                    center.getX(),
                    center.getY(),
                    SimulationConfig.LANE_WIDTH, // Assuming LANE_WIDTH is accessible here
                    0 // The lead vehicle is always at index 0
            );

            // Snap the vehicle to its final stopping position.
            vehicle.setPosition(new Point2D(exactStopPosition[0], exactStopPosition[1]));
        }
    }
    /**
     * Handles the movement logic for a vehicle that is following another vehicle.
     */
    private void handleFollowingVehicle(Vehicle current, Vehicle preceding, Intersection intersection, Point2D center) {
        Point2D movementVector = calculateMovementVector(intersection, current, center);
        Point2D nextPosition = current.getPosition().add(movementVector);

        // Check for safe distance from the vehicle ahead.
        double distanceToPreceding = nextPosition.distance(preceding.getPosition());
        if (distanceToPreceding < SimulationConfig.VEHICLE_LENGTH + SAFE_FOLLOWING_GAP) {
            return; // Too close, do not move.
        }

        current.move(movementVector);
    }


    /**
     * Calculates the movement vector (deltaX, deltaY) for a vehicle based on its origin,
     * intended maneuver, and position relative to the intersection's center.
     *
     * @param intersection The intersection the vehicle is approaching.
     * @param vehicle The vehicle to move.
     * @param center The center point of the intersection.
     * @return A Point2D representing the change in X and Y for the next frame.
     */
    Point2D calculateMovementVector(Intersection intersection, Vehicle vehicle, Point2D center) {
        String origin = intersection.getId().toLowerCase();
        String maneuver = vehicle.getDirection().toLowerCase();
        Point2D position = vehicle.getPosition();
        boolean hasPassedCenter = hasVehiclePassedCenter(origin, position, center);

        // If the vehicle has passed the center, it may need to turn.
        if (hasPassedCenter) {
            return getPostCenterMovement(origin, maneuver);
        } else {
            // Otherwise, it moves straight towards the center.
            return getPreCenterMovement(origin);
        }
    }

    /**
     * Determines if a vehicle has crossed the central point of the intersection.
     */
    private boolean hasVehiclePassedCenter(String origin, Point2D position, Point2D center) {
        return switch (origin) {
            case "north" -> position.getY() >= center.getY();
            case "south" -> position.getY() <= center.getY();
            case "east" -> position.getX() <= center.getX();
            case "west" -> position.getX() >= center.getX();
            default -> false;
        };
    }

    /**
     * Gets the movement vector for a vehicle approaching the center.
     */
    private Point2D getPreCenterMovement(String origin) {
        return switch (origin) {
            case "north" -> new Point2D(0, VEHICLE_SPEED);  // Move South
            case "south" -> new Point2D(0, -VEHICLE_SPEED); // Move North
            case "east" -> new Point2D(-VEHICLE_SPEED, 0); // Move West
            case "west" -> new Point2D(VEHICLE_SPEED, 0);  // Move East
            default -> Point2D.ZERO;
        };
    }

    /**
     * Gets the movement vector for a vehicle that has passed the center, handling turns.
     */
    private Point2D getPostCenterMovement(String origin, String maneuver) {
        if ("straight".equals(maneuver)) {
            return getPreCenterMovement(origin); // Continue in the same direction
        }

        return switch (origin) {
            case "north" -> switch (maneuver) {
                case "left" -> new Point2D(-VEHICLE_SPEED, 0); // Turn West
                case "right" -> new Point2D(VEHICLE_SPEED, 0);  // Turn East
                default -> new Point2D(0, -VEHICLE_SPEED); // U-turn North
            };
            case "south" -> switch (maneuver) {
                case "left" -> new Point2D(VEHICLE_SPEED, 0);  // Turn East
                case "right" -> new Point2D(-VEHICLE_SPEED, 0); // Turn West
                default -> new Point2D(0, VEHICLE_SPEED);  // U-turn South
            };
            case "east" -> switch (maneuver) {
                case "left" -> new Point2D(0, -VEHICLE_SPEED); // Turn North
                case "right" -> new Point2D(0, VEHICLE_SPEED);  // Turn South
                default -> new Point2D(VEHICLE_SPEED, 0);  // U-turn East
            };
            case "west" -> switch (maneuver) {
                case "left" -> new Point2D(0, VEHICLE_SPEED);  // Turn South
                case "right" -> new Point2D(0, -VEHICLE_SPEED); // Turn North
                default -> new Point2D(-VEHICLE_SPEED, 0); // U-turn West
            };
            default -> Point2D.ZERO;
        };
    }

    // --- Getters and Helpers ---

    public List<Intersection> getIntersections() {
        return intersections;
    }

    private java.util.Optional<Intersection> findIntersectionById(String id) {
        return intersections.stream()
                .filter(i -> i.getId().equalsIgnoreCase(id))
                .findFirst();
    }

    // Setter for UI to register a callback
    public void setUiUpdateCallback(Consumer<Void> uiUpdateCallback) {
        this.uiUpdateCallback = uiUpdateCallback;
    }

}
