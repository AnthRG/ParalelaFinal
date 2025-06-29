package app.paralelafinal.entidades;

import java.util.concurrent.PriorityBlockingQueue;

public class Intersection {
    private String id;
    private boolean rightTurnAllowed;
    private PriorityBlockingQueue<Vehicle> vehicleQueue;

    public Intersection(String id, boolean rightTurnAllowed) {
        this.id = id;
        this.rightTurnAllowed = rightTurnAllowed;
        this.vehicleQueue = new PriorityBlockingQueue<>(10, (v1, v2) -> {
            // Priority comparison logic goes here
            // ...
            return 0;
        });
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isRightTurnAllowed() {
        return rightTurnAllowed;
    }

    public void setRightTurnAllowed(boolean rightTurnAllowed) {
        this.rightTurnAllowed = rightTurnAllowed;
    }

    public PriorityBlockingQueue<Vehicle> getVehicleQueue() {
        return vehicleQueue;
    }

    public void setVehicleQueue(PriorityBlockingQueue<Vehicle> vehicleQueue) {
        this.vehicleQueue = vehicleQueue;
    }

    public void addVehicle(Vehicle vehicle) {
        vehicleQueue.add(vehicle);
    }

    public Vehicle getNextVehicle() {
        return vehicleQueue.poll();
    }
}