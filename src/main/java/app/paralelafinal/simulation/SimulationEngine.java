// app.paralelafinal.simulation.SimulationEngine
package app.paralelafinal.simulation;

import app.paralelafinal.config.SimulationConfig;
import app.paralelafinal.controladores.TrafficController;
import app.paralelafinal.entidades.Intersection;
import app.paralelafinal.entidades.TrafficLight;
import app.paralelafinal.entidades.Vehicle;

import javafx.application.Platform;
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
        trafficController.startControl();

        // Vehicle visualization updater
        vehicleUpdater = Executors.newSingleThreadScheduledExecutor();
        vehicleUpdater.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            // This is where you'd trigger the UI to redraw vehicles
            if (uiUpdateCallback != null) {
                uiUpdateCallback.accept(null);
            }
        }), 0, SimulationConfig.VEHICLE_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void stopSimulation() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        if (vehicleUpdater != null && !vehicleUpdater.isShutdown()) {
            vehicleUpdater.shutdownNow();
        }
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
}