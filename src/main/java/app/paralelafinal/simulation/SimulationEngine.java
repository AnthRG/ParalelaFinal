// app.paralelafinal.simulation.SimulationEngine
package app.paralelafinal.simulation;

import app.paralelafinal.config.SimulationConfig;
import app.paralelafinal.controladores.TrafficController;
import app.paralelafinal.entidades.Intersection;
import app.paralelafinal.entidades.TrafficLight;
import app.paralelafinal.entidades.Vehicle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer; // For callback to UI

public class SimulationEngine {

    private List<Intersection> intersections;
    private List<TrafficLight> trafficLights;
    public TrafficController trafficController;

    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService vehicleUpdater;

    // Callback for UI updates
    private Consumer<Void> uiUpdateCallback;

    private Timeline animation;


    public SimulationEngine() {
        //initializeLightBackendLogic();
        setupIntersectionsAndController();
    }

    private void initializeLightBackendLogic() {
        /*trafficLights = new ArrayList<>();
        trafficLights.add(new TrafficLight("North"));
        trafficLights.add(new TrafficLight("South"));
        trafficLights.add(new TrafficLight("East"));
        trafficLights.add(new TrafficLight("West"));

        // Initial state: North/South bound traffic is green, East/West is red.
        trafficLights.stream()
                .filter(tl -> tl.getId().equals("North") || tl.getId().equals("South"))
                .forEach(TrafficLight::changeLight); // becomes green*/
    }

    private void setupIntersectionsAndController() {
        intersections = new ArrayList<>();
        intersections.add(new Intersection("North", true));
        intersections.add(new Intersection("South", true));
        intersections.add(new Intersection("East", true));
        intersections.add(new Intersection("West", true));

        trafficController = new TrafficController(intersections, trafficLights);
    }

    public void startSimulation() {
        // Inicia el control de semáforos y demás, si lo requieres
        trafficController.startControl();

        // Crea una animación que actualiza la posición de los vehículos
        animation = new Timeline(new KeyFrame(Duration.millis(SimulationConfig.VEHICLE_UPDATE_INTERVAL_MS), event -> {
            moveVehicles();   // método que actualiza la posición de cada vehículo
            if (uiUpdateCallback != null) {
                uiUpdateCallback.accept(null);
            }
        }));
        animation.setCycleCount(Timeline.INDEFINITE);
        animation.play();
    }

    public void stopSimulation() {
        if (animation != null) animation.stop();
        if (trafficController != null) {
            trafficController.stopControl();
        }
    }

    // Getters for UI to access simulation state
    public List<Intersection> getIntersections() {
        return intersections;
    }

    public List<TrafficLight> getTrafficLights() {
        return trafficLights;
    }

    // Setter for UI to register a callback
    public void setUiUpdateCallback(Consumer<Void> uiUpdateCallback) {
        this.uiUpdateCallback = uiUpdateCallback;
    }

    // Example: Add a method to randomly add vehicles
    public void addRandomVehicle() {
        Platform.runLater(() -> { // Ensure any UI update from vehicle creation is on FX thread
            // Implement logic to add vehicles to intersections (e.g., to the vehicle queue of an intersection)
            // For example, add to a random intersection's queue
            if (!intersections.isEmpty()) {
                Intersection randomIntersection = intersections.get(new Random().nextInt(intersections.size()));
                String type = (new Random().nextDouble() < 0.1) ? "emergency" : "normal"; // 10% chance for emergency
                Vehicle newVehicle = new Vehicle("V" + System.currentTimeMillis(), type);
                randomIntersection.addVehicle(newVehicle);
                if (uiUpdateCallback != null) {
                    uiUpdateCallback.accept(null);
                }
            }
        });
    }
    public void addVehicle(String type, String direction, String intersectionId) {
        Platform.runLater(() -> {
            //  Crear vehículo
            String id = "V" + System.currentTimeMillis();
            Vehicle v = new Vehicle(id, type, direction, false);

            //  Buscar la intersección y, al encontrarla, calcular posición y encolar
            intersections.stream()
                .filter(i -> i.getId().equalsIgnoreCase(intersectionId))
                .findFirst()
                .ifPresent(i -> {
                    // calcularposición de origen ---
                    double centerX   = SimulationConfig.SCENE_WIDTH  / 2.0;
                    double centerY   = SimulationConfig.SCENE_HEIGHT / 2.0;
                    double laneWidth = SimulationConfig.ROAD_WIDTH   / 2.0;
                    int queueIndex   = i.getVehicleQueue().size();
                    double[] pos = SimulationPane.getVehiclePosition(
                        i.getId(), centerX, centerY, laneWidth, queueIndex);

                    // Fijar la posición lógica antes de encolar
                    v.setPosition(new Point2D(pos[0], pos[1]));

                    // Ahora sí encolar en la intersección
                    i.addVehicle(v);
                });

            //  Forzar refresco de UI
            if (uiUpdateCallback != null) 
                uiUpdateCallback.accept(null);
        });
}


    private void moveVehicles() {
        double speed = 5.0;

        // Calcula el centro de la intersección
        Point2D center = new Point2D(
            SimulationConfig.SCENE_WIDTH  / 2.0,
            SimulationConfig.SCENE_HEIGHT / 2.0
        );
        //  Distancia hasta la línea de PARE: mitad de la carretera + mitad del largo del vehículo
        double stopLineDist = SimulationConfig.ROAD_WIDTH / 2.0
                            + SimulationConfig.VEHICLE_LENGTH / 2.0;

        //  Por cada intersección…
        for (Intersection intersection : intersections) {
            boolean green = intersection.hasGreenLight();

            //  Por cada vehículo en la cola…
            for (Vehicle v : intersection.getVehicleQueue()) {
                Point2D pos = v.getPosition();
                double distToCenter = pos.distance(center);

                //  Si NO es el primero (head) y ya llegó a la línea de PARE → se detiene
                if (!intersection.canProceed(v) && distToCenter <= stopLineDist) {
                    continue;
                }
                // Si es head pero la luz está en rojo y aún no cruzó → espera en PARE
                if ( intersection.canProceed(v)
                  && !green
                  && distToCenter <= stopLineDist) {
                    continue;
                }

                // Calcula el vector de movimiento según origen y dirección
                Point2D delta;
                switch (intersection.getId().toLowerCase()) {
                    case "north":
                        if ("straight".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(0,  speed);
                        } else if ("right".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D( speed, 0);
                        } else if ("left".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(-speed, 0);
                        } else { // u-turn
                            delta = new Point2D(0, -speed);
                        }
                        break;
                    case "south":
                        if ("straight".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(0, -speed);
                        } else if ("right".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(-speed, 0);
                        } else if ("left".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D( speed, 0);
                        } else {
                            delta = new Point2D(0,  speed);
                        }
                        break;
                    case "east":
                        if ("straight".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(-speed, 0);
                        } else if ("right".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(0,  speed);
                        } else if ("left".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(0, -speed);
                        } else {
                            delta = new Point2D( speed, 0);
                        }
                        break;
                    case "west":
                        if ("straight".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D( speed, 0);
                        } else if ("right".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(0, -speed);
                        } else if ("left".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(0,  speed);
                        } else {
                            delta = new Point2D(-speed, 0);
                        }
                        break;
                    default:
                        delta = new Point2D(speed, 0);
                }

                // Mueve el vehículo
                Point2D newPos = pos.add(delta);
                v.setPosition(newPos);

                // Si era head, la luz estaba en verde y ya sobrepasó la intersección → remuévelo
                double newDist = newPos.distance(center);
                if ( intersection.canProceed(v)
                  && green
                  && newDist > stopLineDist + SimulationConfig.VEHICLE_LENGTH) {
                    intersection.removeNextVehicle();
                }
            }
        }
    }
}