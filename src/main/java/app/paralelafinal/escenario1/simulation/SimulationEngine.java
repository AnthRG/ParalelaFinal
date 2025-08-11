package app.paralelafinal.escenario1.simulation;

import app.paralelafinal.escenario1.config.SimulationConfig;
import app.paralelafinal.escenario1.controladores.TrafficController;
import app.paralelafinal.escenario1.entidades.Intersection;
import app.paralelafinal.escenario1.entidades.Vehicle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static app.paralelafinal.escenario1.config.SimulationConfig.*;
import static app.paralelafinal.escenario1.simulation.SimulationPane.getVehiclePosition;

public class SimulationEngine {

    // --- Simulation State ---
    private final List<Intersection> intersections;
    private final TrafficController trafficController;

    // --- Animation & Timing ---
    private Timeline animationLoop;
    private Consumer<Void> uiUpdateCallback;

    // --- Vehicle Movement Constants ---

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
     * Añade un nuevo vehiculo a una intersección específica.
     * Calcula el posicionamiento inicial del vehículo y lo añade a la cola correspondiente.
     * This method is designed to be called from the UI thread to ensure thread safety.
     * 
     * 
     * @param tipo de vehiculo
     * @param para una direccion específica a llegar
     * @param el id de la intersección donde el vehículo debe comenzar.
     *
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
     * logica para actualizar la posicion del vehiculo en la simulación.
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
     * procesa el movimiento de cada vehiculon por posicion.
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
     * maneja el movimiento del primer vehiculo en la cola.
     */
    private void handleLeadVehicle(Vehicle vehicle, Intersection intersection, boolean isGreen, Point2D center, double stopLineDist, double removeDist) {
        if (isGreen && trafficController.isVehicleAuthorizedToMove(vehicle)) {
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
     * maneja el movimmiento logico para un vehiculo que sigue a otro vehiculo.
     */
    private void handleFollowingVehicle(Vehicle current, Vehicle preceding, Intersection intersection, Point2D center) {
        
        if (!trafficController.isVehicleAuthorizedToMove(current)) {
            // Mantener posición actual
            return;
        }

        Point2D movementVector = calculateMovementVector(intersection, current, center);
        Point2D nextPosition = current.getPosition().add(movementVector);

        double distanceToPreceding = nextPosition.distance(preceding.getPosition());
        if (distanceToPreceding < SimulationConfig.VEHICLE_LENGTH + SAFE_FOLLOWING_GAP) {
            return;
        }

        current.move(movementVector);
    }


    /**
     * 
     * 
     * calcula el movimiento del vector (deltaX, deltaY) para un vehiculo basado en su origen,
     * maniobra prevista y posición relativa al centro de la intersección.
     * .
     */
    Point2D calculateMovementVector(Intersection intersection, Vehicle vehicle, Point2D center) {
        if ("u-turn".equalsIgnoreCase(vehicle.getDirection())) {
            return handleUTurnMovement(intersection, vehicle, center);
        }


        String origin = intersection.getId().toLowerCase();
        String maneuver = vehicle.getDirection().toLowerCase();
        Point2D position = vehicle.getPosition();
        boolean hasPassedCenter = hasVehiclePassedCenter(origin, position, center, maneuver);

        // If the vehicle has passed the center, it may need to turn.
        if (hasPassedCenter) {
            return getPostCenterMovement(origin, maneuver);
        } else {
            // Otherwise, it moves straight towards the center.
            return getPreCenterMovement(origin);
        }
    }

// In SimulationEngine.java


    private Point2D handleUTurnMovement(Intersection intersection, Vehicle vehicle, Point2D center) {
        String origin = intersection.getId().toLowerCase();
        Point2D position = vehicle.getPosition();
        String manuver = vehicle.getDirection().toLowerCase();

        switch (vehicle.getUTurnPhase()) {
         
            //fase 0: El vehiculo se acerca al centro de la intersección.
            case 0:
                Point2D approachVector = getPreCenterMovement(origin);

                if (hasVehiclePassedCenter(origin, position, center, manuver)) {
                    vehicle.setUTurnPhase(1);
                }
                return approachVector;

            //fase 1: El vehiculo realiza el giro lateral.
             
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

            //fase 2: El vehiculo sale de la intersección.
            case 2:
                return getPreCenterMovement(origin).multiply(-1);

            default:
                return Point2D.ZERO;
        }
    }
    /**
     
     * Determina si el vehiculo ha cruzado el punto central de la intersección.
     */
    private boolean hasVehiclePassedCenter(String origin, Point2D position, Point2D center, String maneuver) {
        if("left".equalsIgnoreCase(maneuver)){
            return switch (origin) {
                case "north" -> position.getY() >= center.getY() + 30;
                case "south" -> position.getY() <= center.getY() - 50;
                case "east" -> position.getX() <= center.getX() - 60;
                case "west" -> position.getX() >= center.getX() + 30;
                default -> false;
            };

        } else if ("right".equalsIgnoreCase(maneuver)) {
            return switch (origin) {
                case "north" -> position.getY() >= center.getY() - 50;
                case "south" -> position.getY() <= center.getY() + 35;
                case "east" -> position.getX() <= center.getX() + 30;
                case "west" -> position.getX() >= center.getX() - 65;
                default -> false;
            };
        } else{
            return switch (origin) {
                case "north" -> position.getY() >= center.getY() ;
                case "south" -> position.getY() <= center.getY() ;
                case "east" -> position.getX() <= center.getX();
                case "west" -> position.getX() >= center.getX();
                default -> false;
            };

        }

    }


    /**
     
     * tome el movimiento del vector para  un vehiculo que se acerca al centro de la intersección.
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



    public List<Intersection> getIntersections() {
        return intersections;
    }

    private java.util.Optional<Intersection> findIntersectionById(String id) {
        return intersections.stream()
                .filter(i -> i.getId().equalsIgnoreCase(id))
                .findFirst();
    }

    
    public void setUiUpdateCallback(Consumer<Void> uiUpdateCallback) {
        this.uiUpdateCallback = uiUpdateCallback;
    }

}
