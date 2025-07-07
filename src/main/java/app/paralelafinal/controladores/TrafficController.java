package app.paralelafinal.controladores;

import app.paralelafinal.entidades.Intersection;
import app.paralelafinal.entidades.TrafficLight;
import app.paralelafinal.entidades.Vehicle;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;

public class TrafficController {
    private List<Intersection> intersections;
    private List<TrafficLight> trafficLights;
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService vehicleGeneratorScheduler;
    private Random random = new Random();

    // Directions that can safely pass together
    private static final Map<String, Set<String>> COMPATIBLE_DIRECTIONS = Map.of(
            "right", Set.of("straight", "left"),
            "straight", Set.of("right", "u-turn"),
            "left", Set.of("right"),
            "u-turn", Set.of("straight")
    );

    public TrafficController(List<Intersection> intersections, List<TrafficLight> trafficLights) {
        this.intersections = intersections;
        this.trafficLights = trafficLights;
        this.scheduler = Executors.newScheduledThreadPool(10);
        this.vehicleGeneratorScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startControl() {
      scheduler.scheduleAtFixedRate(this::manageIntersections, 0, 2, TimeUnit.SECONDS);
     // vehicleGeneratorScheduler.scheduleAtFixedRate(this::generateRandomVehicle, 0, 1, TimeUnit.SECONDS);
    }

    private void manageIntersections() {

        // Get all vehicles at the front of each queue
        List<Vehicle> frontVehicles = getAllFrontVehicles();

        // 1. Check for emergency vehicles first
        /*List<Vehicle> emergencies = frontVehicles.stream()
                .filter(Objects::nonNull)
                .filter(v -> "emergency".equalsIgnoreCase(v.getType()))
                .sorted(Comparator.comparingLong(Vehicle::getArrivalTime))
                .toList();

        if (!emergencies.isEmpty()) {
            // Let the earliest emergency vehicle pass
            while (!emergencies.isEmpty()) {
                Vehicle vehiTemp= intersection.getNextVehicle();
                if(vehiTemp != null) {
                    if(vehiTemp.getType().equalsIgnoreCase("emergency")) {
                        emergencies = new ArrayList<>();
                    }
                }
            }
            continue;
        }
        */
        // 2. Find compatible non-emergency vehicles
        List<Vehicle> compatibleGroup = findCompatibleVehicles(frontVehicles);
        compatibleGroup.forEach(vehicle -> {
            try {
                this.processCompatibleVehi(vehicle);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                throw new RuntimeException(e);
            }
        });

        // 3. If no compatible groups, let the oldest vehicle pass
        frontVehicles.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingLong(Vehicle::getArrivalTime))
                .ifPresent(vehicle -> {
                    try {
                        this.processVehicle(vehicle);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore interrupt flag
                        throw new RuntimeException("Interrupted while processing vehicle", e);
                    }
                });
    }

    private void processCompatibleVehi(Vehicle vehicle) throws InterruptedException {
        for (Intersection inter : intersections) {
            if (inter.peekNextVehicle() != null
              && inter.peekNextVehicle().getId().equalsIgnoreCase(vehicle.getId())) {
                // Enciende verde
                inter.setGreenLight(true);
                // Apaga verde tras el ciclo (2s en este ejemplo)
                scheduler.schedule(() -> inter.setGreenLight(false), 2, TimeUnit.SECONDS);

                // Lógica de intersecciones cruzadas
                switch (inter.getId()) {
                    case "North" -> intersections.get(1).setGreenLight(true);
                    case "South" -> intersections.get(0).setGreenLight(true);
                    case "East"  -> intersections.get(3).setGreenLight(true);
                    case "West"  -> intersections.get(2).setGreenLight(true);
                }
                break;
            }
        }
    }

    private void processVehicle(@NotNull Vehicle vehicle) throws InterruptedException {
        for (Intersection inter : intersections) {
            if (inter.peekNextVehicle() != null
             && inter.peekNextVehicle().getId().equalsIgnoreCase(vehicle.getId())) {
                // Enciende verde para que avance
                inter.setGreenLight(true);
                // Apaga verde tras el ciclo
                scheduler.schedule(() -> inter.setGreenLight(false), 2, TimeUnit.SECONDS);
                break;
            }
        }
    }

    private List<Vehicle> getAllFrontVehicles() {
        List<Vehicle> frontVehicles = new ArrayList<>();
        for (Intersection intersection : intersections) {
            frontVehicles.add(intersection.peekNextVehicle());
        }
        return frontVehicles;
    }

    private List<Vehicle> findCompatibleVehicles(List<Vehicle> vehicles) {
        List<Vehicle> compatibleVehicles = new ArrayList<>();
        List<Vehicle> candidates = vehicles.stream()
                .filter(Objects::nonNull)
                .filter(v -> !"emergency".equals(v.getType()))
                .sorted(Comparator.comparingLong(Vehicle::getArrivalTime))
                .toList();

        for (Vehicle v1 : candidates) {
            if (compatibleVehicles.isEmpty()) {
                compatibleVehicles.add(v1);
                continue;
            }

            boolean canAdd = true;
            for (Vehicle v2 : compatibleVehicles) {
                if (!canPassTogether(v1, v2)) {
                    canAdd = false;
                    break;
                }
            }

            if (canAdd) {
                compatibleVehicles.add(v1);
            }
        }

        return compatibleVehicles;
    }

    private boolean canPassTogether(Vehicle v1, Vehicle v2) {
        // Check if directions are compatible
        return COMPATIBLE_DIRECTIONS.get(v1.getDirection()).contains(v2.getDirection()) ||
                COMPATIBLE_DIRECTIONS.get(v2.getDirection()).contains(v1.getDirection());
    }

    public void generateRandomVehicle() {
        if (intersections.isEmpty()) return;

        String[] types = {"normal", "emergency"};
        String[] directions = {"right", "straight", "left", "u-turn"};

        String id = UUID.randomUUID().toString();
        String type = types[random.nextInt(types.length)];
        String direction = directions[random.nextInt(directions.length)];

        Vehicle vehicle = new Vehicle(id, direction);

        // Select a random intersection
        Intersection intersection = intersections.get(random.nextInt(intersections.size()));
        intersection.addVehicle(vehicle);
    }

    public void stopControl() {
        scheduler.shutdown();
        vehicleGeneratorScheduler.shutdown();
    }

    @FunctionalInterface
    public interface VehicleCrossingListener {
        void onVehicleCross(Vehicle vehicle, String fromDirection);
    }
}