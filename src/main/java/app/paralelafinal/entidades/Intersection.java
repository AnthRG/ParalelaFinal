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
            // Priority comparison logic:
            // 1. Emergency vehicles have highest priority.
            // 2. For vehicles of the same type, earlier arrival time has higher priority.

            boolean v1IsEmergency = "emergency".equalsIgnoreCase(v1.getType());
            boolean v2IsEmergency = "emergency".equalsIgnoreCase(v2.getType());

            if (v1IsEmergency && !v2IsEmergency) {
                return -1; // v1 has higher priority
            }
            if (!v1IsEmergency && v2IsEmergency) {
                return 1; // v2 has higher priority
            }

            return Long.compare(v1.getArrivalTime(), v2.getArrivalTime()); // Earlier arrival time gets higher priority
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
    public Vehicle peekNextVehicle() {
        return vehicleQueue.peek();
    }
}