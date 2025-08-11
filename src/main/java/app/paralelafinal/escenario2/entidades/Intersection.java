package app.paralelafinal.escenario2.entidades;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

public class Intersection {
    private String id;
    private boolean rightTurnAllowed;
    private PriorityBlockingQueue<Vehicle> LeftVehicleQueue;
    private PriorityBlockingQueue<Vehicle> RightVehicleQueue;
    private PriorityBlockingQueue<Vehicle> MidVehicleQueue;
    private boolean greenLight;

    public Intersection(String id) {
        this.id = id;
        this.rightTurnAllowed = true;
        this.greenLight = false;
        this.LeftVehicleQueue = new PriorityBlockingQueue<>(10,
            Comparator.comparingLong(Vehicle::getArrivalTime)
        );
        this.RightVehicleQueue = new PriorityBlockingQueue<>(10,
            Comparator.comparingLong(Vehicle::getArrivalTime)
        );
        this.MidVehicleQueue = new PriorityBlockingQueue<>(10,
            Comparator.comparingLong(Vehicle::getArrivalTime)
        );
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public PriorityBlockingQueue<Vehicle> getLeftVehicleQueue() {
        return LeftVehicleQueue;
    }
    public void setLeftVehicleQueue(PriorityBlockingQueue<Vehicle> q) {
        this.LeftVehicleQueue = q;
    }

    public PriorityBlockingQueue<Vehicle> getRightVehicleQueue() {
        return RightVehicleQueue;
    }
    public void setRightVehicleQueue(PriorityBlockingQueue<Vehicle> q) {
        this.RightVehicleQueue = q;
    }

    public PriorityBlockingQueue<Vehicle> getMidVehicleQueue() {
        return MidVehicleQueue;
    }
    public void setMidVehicleQueue(PriorityBlockingQueue<Vehicle> q) {
        this.MidVehicleQueue = q;
    }

    public boolean isGreenLight() { return greenLight; }
    public void setGreenLight(boolean on) { this.greenLight = on; }
    public boolean hasGreenLight() { return greenLight; }

    public boolean isRightTurnAllowed() { return rightTurnAllowed; }
    public void setRightTurnAllowed(boolean allowed) {
        this.rightTurnAllowed = allowed;
    }

    public void addVehicle(Vehicle v) {
        switch(v.getDirection().toLowerCase()) {
            case "left" -> LeftVehicleQueue.add(v);
            case "right" -> RightVehicleQueue.add(v);
            default -> MidVehicleQueue.add(v);
        }
    }

    public Vehicle peekNextVehicle() {
        return MidVehicleQueue.peek();
    }
    public void removeNextVehicle() {
        MidVehicleQueue.poll();
    }
    public boolean canProceed(Vehicle v) {
        return v.equals(MidVehicleQueue.peek());
    }
    public List<Vehicle> getAllFrontVehicles() {
        return new ArrayList<>(MidVehicleQueue);
    }
    public void processVehicle() {
        removeNextVehicle();
    }
    public boolean hasEmergencyVehicleInQueue() {
        return MidVehicleQueue.stream()
            .anyMatch(vehicle -> "emergency".equals(vehicle.getType()));
    }
}