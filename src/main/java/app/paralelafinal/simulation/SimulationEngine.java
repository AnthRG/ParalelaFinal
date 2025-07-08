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

import static app.paralelafinal.config.SimulationConfig.*;
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
                String vehicleId = "V" + System.currentTimeMillis();
                Vehicle newVehicle = new Vehicle(vehicleId, type, direction, false);

                double centerX = SimulationConfig.SCENE_WIDTH / 2.0;
                double centerY = SimulationConfig.SCENE_HEIGHT / 2.0;
                double laneWidth = SimulationConfig.ROAD_WIDTH / 2.0;
                int queueIndex = intersection.getVehicleQueue().size();

                double[] pos = getVehiclePosition(intersection.getId(), centerX, centerY, laneWidth, queueIndex);
                newVehicle.setPosition(new Point2D(pos[0], pos[1]));

                intersection.addVehicle(newVehicle);
            });

            if (uiUpdateCallback != null) {
                uiUpdateCallback.accept(null);
            }
        });
    }

    /**
     * The main logic loop for updating the position of every vehicle in the simulation.
     */
    private void updateVehiclePositions() {
        final Point2D center = new Point2D(SimulationConfig.SCENE_WIDTH / 2.0, SimulationConfig.SCENE_HEIGHT / 2.0);
        final double stopLineDistance = (SimulationConfig.ROAD_WIDTH / 2.0) + (SimulationConfig.VEHICLE_LENGTH / 2.0);
        final double removalThreshold = SCENE_WIDTH / 2 ;

        for (Intersection intersection : intersections) {
            processVehiclesForIntersection(intersection, center, stopLineDistance, removalThreshold);
        }


    }

    /**
     * Processes the movement for all vehicles waiting at a single intersection.
     */
    private void processVehiclesForIntersection(Intersection intersection, Point2D center, double stopLineDist, double removeDist) {
        boolean isGreenLight = intersection.hasGreenLight();

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
     * Handles the movement for the first vehicle in a queue.
     */
    private void handleLeadVehicle(Vehicle vehicle, Intersection intersection, boolean isGreen, Point2D center, double stopLineDist, double removeDist) {
        if (isGreen) {
            double distanceToCenter = vehicle.getPosition().distance(center);

            if (distanceToCenter > removeDist) {
                intersection.removeNextVehicle();
                return;
            }

            Point2D movementVector = calculateMovementVector(intersection, vehicle, center);
            vehicle.move(movementVector);

        } else {
            double[] exactStopPosition = getVehiclePosition(
                    intersection.getId(),
                    center.getX(),
                    center.getY(),
                    SimulationConfig.LANE_WIDTH,
                    0
            );

            vehicle.setPosition(new Point2D(exactStopPosition[0], exactStopPosition[1]));
        }
    }

    /**
     * Handles the movement logic for a vehicle that is following another vehicle.
     */
    private void handleFollowingVehicle(Vehicle current, Vehicle preceding, Intersection intersection, Point2D center) {
        Point2D movementVector = calculateMovementVector(intersection, current, center);
        Point2D nextPosition = current.getPosition().add(movementVector);

        double distanceToPreceding = nextPosition.distance(preceding.getPosition());
        if (distanceToPreceding < SimulationConfig.VEHICLE_LENGTH + SAFE_FOLLOWING_GAP) {
            return;
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
        if ("u-turn".equalsIgnoreCase(vehicle.getDirection())) {
            return handleUTurnMovement(intersection, vehicle, center);
        }


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

// In SimulationEngine.java

    /**
     * Calculates the movement vector for a vehicle performing a U-turn.
     * This is a stateful turn broken into three phases.
     * @param intersection The intersection the vehicle is at.
     * @param vehicle The vehicle performing the turn.
     * @param center The center point of the intersection.
     * @return A Point2D representing the movement vector for the current frame.
     */
    private Point2D handleUTurnMovement(Intersection intersection, Vehicle vehicle, Point2D center) {
        String origin = intersection.getId().toLowerCase();
        Point2D position = vehicle.getPosition();

        switch (vehicle.getUTurnPhase()) {
            // --- PHASE 0: APPROACH THE CENTER ---
            case 0:
                Point2D approachVector = getPreCenterMovement(origin);

                if (hasVehiclePassedCenter(origin, position, center)) {
                    vehicle.setUTurnPhase(1);
                }
                return approachVector;

            // --- PHASE 1: PERFORM THE LATERAL PART OF THE TURN ---
            case 1:
                double turnFactor = 0.5;

                double laneWidth = SimulationConfig.ROAD_WIDTH / 2.0;
                double turnDistance = laneWidth * turnFactor;
                Point2D turnVector = Point2D.ZERO;

                switch (origin) {
                    case "north":
                        turnVector = new Point2D(VEHICLE_SPEED, 0);
                        if (position.getX() >= center.getX() + turnDistance) {
                            vehicle.setUTurnPhase(2);
                        }
                        break;
                    case "south":
                        turnVector = new Point2D(-VEHICLE_SPEED, 0);
                        if (position.getX() <= center.getX() - turnDistance) {
                            vehicle.setUTurnPhase(2);
                        }
                        break;
                    case "east":
                        turnVector = new Point2D(0, VEHICLE_SPEED);
                        if (position.getY() >= center.getY() + turnDistance) {
                            vehicle.setUTurnPhase(2);
                        }
                        break;
                    case "west":
                        turnVector = new Point2D(0, -VEHICLE_SPEED);
                        if (position.getY() <= center.getY() - turnDistance) {
                            vehicle.setUTurnPhase(2);
                        }
                        break;
                }
                return turnVector;

            // --- PHASE 2: EXIT THE INTERSECTION ---
            case 2:
                return getPreCenterMovement(origin).multiply(-1);

            default:
                return Point2D.ZERO;
        }
    }
    /**
     * Determines if a vehicle has crossed the central point of the intersection.
     */
    private boolean hasVehiclePassedCenter(String origin, Point2D position, Point2D center) {
        double laneWidth = SimulationConfig.ROAD_WIDTH / 2.0;
        double turnFactor = 0.5;
        double turnDistance = laneWidth * turnFactor;

        return switch (origin) {
            case "north" -> position.getY() >= center.getY() + turnDistance;
            case "south" -> position.getY() <= center.getY() - turnDistance;
            case "east" -> position.getX() <= center.getX() - turnDistance;
            case "west" -> position.getX() >= center.getX() + turnDistance;
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
            return getPreCenterMovement(origin);
        }


        return switch (origin) {
            case "north" -> switch (maneuver) {
                case "left" -> new Point2D(VEHICLE_SPEED, 0);
                case "right" -> new Point2D(-VEHICLE_SPEED, 0);
                default -> getPreCenterMovement(origin);
            };
            case "south" -> switch (maneuver) {
                case "left" -> new Point2D(-VEHICLE_SPEED, 0);
                case "right" -> new Point2D(VEHICLE_SPEED, 0);
                default -> getPreCenterMovement(origin);
            };
            case "east" -> switch (maneuver) {
                case "left" -> new Point2D(0, VEHICLE_SPEED);
                case "right" -> new Point2D(0, -VEHICLE_SPEED);
                default -> getPreCenterMovement(origin);
            };
            case "west" -> switch (maneuver) {
                case "left" -> new Point2D(0, -VEHICLE_SPEED);
                case "right" -> new Point2D(0, VEHICLE_SPEED);
                default -> getPreCenterMovement(origin);
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
