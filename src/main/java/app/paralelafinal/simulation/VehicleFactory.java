package app.paralelafinal.simulation;

import app.paralelafinal.entidades.Vehicle;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class VehicleFactory {
    private static final AtomicInteger vehicleCount = new AtomicInteger(0);
    private static final Random random = new Random();

    public static Vehicle createRandomVehicle() {
        String id = "V" + vehicleCount.incrementAndGet();
        String type = (random.nextDouble() < 0.30) ? "emergency" : "normal"; // 15% chance for emergency
        // You could add more complex properties here
        return new Vehicle(id, type);
    }
}