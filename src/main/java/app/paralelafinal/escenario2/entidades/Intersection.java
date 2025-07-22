package app.paralelafinal.escenario2.entidades;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

public class Intersection {
    private String id;
    private boolean rightTurnAllowed;
    private PriorityBlockingQueue<Vehicle> LeftVehicleQueue; //left
    private PriorityBlockingQueue<Vehicle> RightVehicleQueue; //right
    private PriorityBlockingQueue<Vehicle> MidVehicleQueue; //middle
    private boolean greenLight;

    public Intersection(String id) {
        this.id = id;
        this.rightTurnAllowed = true;
        this.TopVehicleQueue = new PriorityBlockingQueue<>(10,
                Comparator.comparingLong(Vehicle::getArrivalTime)
        );
        this.BotVehicleQueue = new PriorityBlockingQueue<>(10,
                Comparator.comparingLong(Vehicle::getArrivalTime)
        );
        this.MidVehicleQueue = new PriorityBlockingQueue<>(10,
                Comparator.comparingLong(Vehicle::getArrivalTime)
        );

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PriorityBlockingQueue<Vehicle> getTopVehicleQueue() {
        return TopVehicleQueue;
    }

    public void setTopVehicleQueue(PriorityBlockingQueue<Vehicle> topVehicleQueue) {
        TopVehicleQueue = topVehicleQueue;
    }

    public PriorityBlockingQueue<Vehicle> getBotVehicleQueue() {
        return BotVehicleQueue;
    }

    public void setBotVehicleQueue(PriorityBlockingQueue<Vehicle> botVehicleQueue) {
        BotVehicleQueue = botVehicleQueue;
    }

    public PriorityBlockingQueue<Vehicle> getMidVehicleQueue() {
        return MidVehicleQueue;
    }

    public void setMidVehicleQueue(PriorityBlockingQueue<Vehicle> midVehicleQueue) {
        MidVehicleQueue = midVehicleQueue;
    }

    public boolean isGreenLight() {
        return greenLight;
    }

    public boolean isRightTurnAllowed() {
        return rightTurnAllowed;
    }

    public void setRightTurnAllowed(boolean allowed) {
        this.rightTurnAllowed = allowed;
    }

    // Encola un vehículo (se añade según prioridad) 
    public void addVehicle(Vehicle v) {
        switch (v.getDirection().toLowerCase()){
            case "left" -> LeftVehicleQueue.add(v);
            case "right" -> RightVehicleQueue.add(v);
            default -> MidVehicleQueue.add(v);
        }
    }

    // Devuelve sin quitar el vehículo que está al frente de la cola
    public Vehicle peekNextVehicle() {
        return MidVehicleQueue.peek();
    }

    // Quita de la cola al vehículo que acaba de cruzar
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

    public void setGreenLight(boolean on) {
        this.greenLight = on;
    }

    public boolean hasGreenLight() {
        return greenLight;
    }

    public boolean hasEmergencyVehicleInQueue() {
        return MidVehicleQueue.stream()
                .anyMatch(vehicle -> "emergency".equals(vehicle.getType()));
    }
}