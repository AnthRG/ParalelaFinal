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
        // 2) Línea de PARE (distancia del centro en la que arrancan a “frenar”)
        double stopLineDist = SimulationConfig.ROAD_WIDTH / 2.0
                            + SimulationConfig.VEHICLE_LENGTH / 2.0;
        // 3) Umbral de REMOCIÓN: cuando el coche lo sobrepasa, lo quitas
        double removeDist = stopLineDist + SimulationConfig.VEHICLE_LENGTH;

        for (Intersection intersection : intersections) {
            boolean green = intersection.hasGreenLight();

            // --- 4) Snapshot ordenado de la cola (prioridad + tiempo de llegada) ---
            List<Vehicle> ordered = new ArrayList<>(intersection.getVehicleQueue());
            Comparator<? super Vehicle> cmp = intersection.getVehicleQueue().comparator();
            if (cmp != null) ordered.sort(cmp);

            // 5) Procesa cabeza, segundo, tercero…
            for (int i = 0; i < ordered.size(); i++) {
                Vehicle v = ordered.get(i);
                Point2D pos = v.getPosition();
                double dist = pos.distance(center);

                // LÓGICA DE CABEZA (i==0)
                if (i == 0) {
                    // 1) Si llegó a la línea de PARE y la luz está en rojo → no se mueve
                    if (dist <= stopLineDist && !green) {
                        continue;
                    }
                    // 2) Si ya cruzó completamente y estaba en verde → lo quitas de la cola
                    if (dist > removeDist && green) {
                        intersection.removeNextVehicle();
                        continue;
                    }
                    // 3) Si está permitido, calculas su delta más abajo
                }
                // =====  B) SEGUIDORES (i>0): sólo espaciamiento =====
                else {
                    // Obtén la posición del vehículo anterior
                    Point2D prevPos = ordered.get(i - 1).getPosition();
                    // Calculamos delta para ver hasta dónde querría moverse…
                    Point2D delta = computeDelta(intersection.getId(), v.getDirection(), speed);
                    Point2D nextPos = pos.add(delta);
                    // Si al moverlo choca (< largo+gap) con el anterior → no se mueve
                    if (nextPos.distance(prevPos) < SimulationConfig.VEHICLE_LENGTH + safeGap) {
                        continue;
                    }
                    // Si no choca, lo movemos (y saltamos el resto de chequeos)
                    v.setPosition(nextPos);
                    continue;
                }

                // =====  C) Movimiento normal de la cabeza =====
                // calculamos delta tal y como ya tenías
                Point2D delta;
                switch (intersection.getId().toLowerCase()) {
                    case "north":
                        if ("straight".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(0,  speed);
                        } else if ("right".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D( speed, 0);
                        } else if ("left".equalsIgnoreCase(v.getDirection())) {
                            delta = new Point2D(-speed, 0);
                        } else {
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

                // Aplica movimiento a la cabeza
                v.setPosition(pos.add(delta));
            }
        }
    }

    /** 
     * Extrae exactamente el delta que se usa para cabeza y compatibles. 
    
     */
    private Point2D computeDelta(String intersectionId, String direction, double speed) {
        switch (intersectionId.toLowerCase()) {
          case "north":
            switch(direction.toLowerCase()) {
              case "straight": return new Point2D(0, speed);
              case "right":    return new Point2D(speed, 0);
              case "left":     return new Point2D(-speed,0);
              default:         return new Point2D(0,-speed);
            }
          case "south":
            switch(direction.toLowerCase()) {
              case "straight": return new Point2D(0, -speed);
              case "right":    return new Point2D(-speed,0);
              case "left":     return new Point2D(speed, 0);
              default:         return new Point2D(0, speed);
            }
          case "east":
            switch(direction.toLowerCase()) {
              case "straight": return new Point2D(-speed,0);
              case "right":    return new Point2D(0, speed);
              case "left":     return new Point2D(0,-speed);
              default:         return new Point2D(speed,0);
            }
          case "west":
            switch(direction.toLowerCase()) {
              case "straight": return new Point2D(speed,0);
              case "right":    return new Point2D(0,-speed);
              case "left":     return new Point2D(0, speed);
              default:         return new Point2D(-speed,0);
            }
          default:
            return new Point2D(speed,0);
        }
    }
}