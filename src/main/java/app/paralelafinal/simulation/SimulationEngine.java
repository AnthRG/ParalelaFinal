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
import java.util.Comparator;
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
        double speed   = 5.0;
        double safeGap = 5.0;  // espacio mínimo entre vehículos

        // 1) Centro de la intersección
        Point2D center = new Point2D(
            SimulationConfig.SCENE_WIDTH  / 2.0,
            SimulationConfig.SCENE_HEIGHT / 2.0
        );
        // 2) Línea de PARE: mitad de la carretera + mitad del vehículo
        double stopLineDist = SimulationConfig.ROAD_WIDTH / 2.0
                            + SimulationConfig.VEHICLE_LENGTH / 2.0;
        // 3) Umbral de remoción
        double removeDist = stopLineDist + SimulationConfig.VEHICLE_LENGTH;

        for (Intersection intersection : intersections) {
            boolean green = intersection.hasGreenLight();

            // 4) Snapshot ordenado de la cola
            List<Vehicle> ordered = new ArrayList<>(intersection.getVehicleQueue());
            Comparator<? super Vehicle> cmp = intersection.getVehicleQueue().comparator();
            if (cmp != null) ordered.sort(cmp);

            // 5) Procesa cabeza y seguidores
            for (int i = 0; i < ordered.size(); i++) {
                Vehicle v = ordered.get(i);
                Point2D pos = v.getPosition();
                double dist = pos.distance(center);

                // A) Cabeza
                if (i == 0) {
                    if (dist <= stopLineDist && !green) {
                        // llegó a PARE y la luz está en rojo → no avanza
                        continue;
                    }
                    if (dist > removeDist && green) {
                        // ya cruzó → lo quitamos
                        intersection.removeNextVehicle();
                        continue;
                    }
                    // si puede, calculamos delta más abajo…
                }
                // B) Seguidores: sólo espacio
                else {
                    Point2D prevPos = ordered.get(i - 1).getPosition();
                    Point2D delta   = computeDelta(intersection, v, speed, center);
                    Point2D nextPos = pos.add(delta);
                    if (nextPos.distance(prevPos) < SimulationConfig.VEHICLE_LENGTH + safeGap) {
                        continue;  // respeta el hueco
                    }
                    v.setPosition(nextPos);
                    continue;
                }

                // C) Movimiento de la cabeza (light + giro post-centro)
                Point2D delta = computeDelta(intersection, v, speed, center);
                v.setPosition(pos.add(delta));
            }
        }
}

/**
 * Delta X/Y según origen, maniobra y posición relativa al centro.
 */
    public Point2D computeDelta(Intersection inter, Vehicle v, double speed, Point2D center) {
        String origin = inter.getId().toLowerCase();
        String cmd    = v.getDirection().toLowerCase();
        Point2D pos   = v.getPosition();
        double cx     = center.getX(), cy = center.getY();

        switch (origin) {
        case "north":
            if (pos.getY() < cy) {
                return new Point2D(0, speed);
            }
            if ("left".equals(cmd))  return new Point2D(-speed,  0);
            if ("right".equals(cmd)) return new Point2D( speed,  0);
            return "straight".equals(cmd)
                ? new Point2D(0, speed)
                : new Point2D(0, -speed);

        case "south":
            if (pos.getY() > cy) {
                return new Point2D(0, -speed);
            }
            if ("left".equals(cmd))  return new Point2D( speed, 0);
            if ("right".equals(cmd)) return new Point2D(-speed,0);
            return "straight".equals(cmd)
                ? new Point2D(0, -speed)
                : new Point2D(0, speed);

        case "east":
            if (pos.getX() > cx) {
                return new Point2D(-speed, 0);
            }
            if ("left".equals(cmd))  return new Point2D(0, -speed);
            if ("right".equals(cmd)) return new Point2D(0,  speed);
            return "straight".equals(cmd)
                ? new Point2D(-speed, 0)
                : new Point2D( speed, 0);

        case "west":
            if (pos.getX() < cx) {
                return new Point2D(speed, 0);
            }
            if ("left".equals(cmd))  return new Point2D(0,  speed);
            if ("right".equals(cmd)) return new Point2D(0, -speed);
            return "straight".equals(cmd)
                ? new Point2D(speed, 0)
                : new Point2D(-speed, 0);

        default:
            return Point2D.ZERO;
        }
    }

}