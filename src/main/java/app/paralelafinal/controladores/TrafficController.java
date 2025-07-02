package app.paralelafinal.controladores;

import app.paralelafinal.entidades.Intersection;
import app.paralelafinal.entidades.TrafficLight;
import app.paralelafinal.entidades.Vehicle;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.Random;
import java.util.UUID;

public class TrafficController {
    private List<Intersection> intersections;
    private List<TrafficLight> trafficLights;
    private ScheduledExecutorService scheduler;

    private ScheduledExecutorService vehicleGeneratorScheduler;
    private Random random = new Random();

    public TrafficController(List<Intersection> intersections, List<TrafficLight> trafficLights) {
        this.intersections = intersections;
        this.trafficLights = trafficLights;
        this.scheduler = Executors.newScheduledThreadPool(10);
        this.vehicleGeneratorScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startControl() {
/*        for (TrafficLight light : trafficLights) {
            scheduler.scheduleAtFixedRate(light::changeLight, 0, 60, TimeUnit.SECONDS);
        }*/
        scheduler.scheduleAtFixedRate(this::manageIntersections, 0, 1, TimeUnit.SECONDS);

        // Generar vehículos automáticamente cada 1 segundo
        vehicleGeneratorScheduler.scheduleAtFixedRate(this::generateRandomVehicle, 0, 1, TimeUnit.SECONDS);
    }

    private void manageIntersections() {
        int i =0;
        int imayor = 0;
        long mayor = -1000000;
        for (Intersection intersection : intersections) {
            Vehicle nextVehicle = intersection.getVehicleQueue().peek();
            if(nextVehicle != null){
                if(System.nanoTime() - nextVehicle.getArrivalTime() > mayor) {
                    mayor = nextVehicle.getArrivalTime();
                    imayor = i;
                }
            }
            i++;
        }
        Vehicle vehicle = intersections.get(imayor).getNextVehicle();
    }

    // Genera un vehículo aleatorio y lo agrega a una intersección aleatoria
    public void generateRandomVehicle() {
        if (intersections.isEmpty()) return;

        String[] types = {"normal", "emergency"};
        String[] directions = {"right", "straight", "left", "u-turn"};

        String id = UUID.randomUUID().toString();
        String type = types[random.nextInt(types.length)];
        String direction = directions[random.nextInt(directions.length)];

        Vehicle vehicle = new Vehicle(id, type, direction, false);

        // Selecciona una intersección aleatoria
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