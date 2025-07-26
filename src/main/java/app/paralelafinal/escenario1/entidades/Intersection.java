package app.paralelafinal.escenario1.entidades;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

public class Intersection {
    private String id;
    private boolean rightTurnAllowed;
    private PriorityBlockingQueue<Vehicle> vehicleQueue;
    private boolean greenLight;  

    public Intersection(String id) {
        this.id = id;
        this.rightTurnAllowed = true;
        this.vehicleQueue = new PriorityBlockingQueue<>(10,
            Comparator.comparingLong(Vehicle::getArrivalTime)
        );

    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isRightTurnAllowed() { return rightTurnAllowed; }
    public void setRightTurnAllowed(boolean allowed) { this.rightTurnAllowed = allowed; }

    public PriorityBlockingQueue<Vehicle> getVehicleQueue() { return vehicleQueue; }
    public void setVehicleQueue(PriorityBlockingQueue<Vehicle> q) { this.vehicleQueue = q; }

    // Encola un vehículo (se añade según prioridad) 
    public void addVehicle(Vehicle v) {
        vehicleQueue.add(v);
    }

    // Devuelve sin quitar el vehículo que está al frente de la cola 
    public Vehicle peekNextVehicle() {
        return vehicleQueue.peek();
    }

    // Quita de la cola al vehículo que acaba de cruzar 
    public void removeNextVehicle() {
        vehicleQueue.poll();
    }

    
    public boolean canProceed(Vehicle v) {
        return v.equals(vehicleQueue.peek());
    }

    
    public List<Vehicle> getAllFrontVehicles() {
        return new ArrayList<>(vehicleQueue);
    }

    public void processVehicle() {
        removeNextVehicle();
    }
      public void setGreenLight(boolean on) {
        this.greenLight = on;
    }
    public boolean hasGreenLight() {
        return greenLight;
    }

    public boolean hasEmergencyVehicleInQueue() {
        return vehicleQueue.stream()
                .anyMatch(vehicle -> "emergency".equals(vehicle.getType()));
    }
}