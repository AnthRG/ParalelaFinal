package app.paralelafinal.escenario2.entidades;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Intersection {
    private String id;
    private PriorityBlockingQueue<Vehicle> RightVQueue;
    private PriorityBlockingQueue<Vehicle> MidVQueue;
    private PriorityBlockingQueue<Vehicle> LeftVQueue;
    private AtomicBoolean greenLight;
    private TrafficLight trafficLight;

    public Intersection(String id) {
        this.id = id;
        this.greenLight = new AtomicBoolean(false);
        this.RightVQueue = new PriorityBlockingQueue<>(10,
                Comparator.comparingLong(Vehicle::getArrivalTime)
        );
        this.MidVQueue = new PriorityBlockingQueue<>(10,
                Comparator.comparingLong(Vehicle::getArrivalTime)
        );
        this.LeftVQueue = new PriorityBlockingQueue<>(10,
                Comparator.comparingLong(Vehicle::getArrivalTime)
        );
        // Initialize a traffic light bound to this intersection's green flag to avoid NPEs
        this.trafficLight = new TrafficLight(id, greenLight);
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public PriorityBlockingQueue<Vehicle> getRightVQueue() {
        return RightVQueue;
    }

    public void setRightVQueue(PriorityBlockingQueue<Vehicle> rightVQueue) {
        RightVQueue = rightVQueue;
    }

    public PriorityBlockingQueue<Vehicle> getMidVQueue() {
        return MidVQueue;
    }

    public void setMidVQueue(PriorityBlockingQueue<Vehicle> midVQueue) {
        MidVQueue = midVQueue;
    }

    public PriorityBlockingQueue<Vehicle> getLeftVQueue() {
        return LeftVQueue;
    }

    public void setLeftVQueue(PriorityBlockingQueue<Vehicle> leftVQueue) {
        LeftVQueue = leftVQueue;
    }

    public boolean isGreenLight() {
        return greenLight.get();
    }

    // Encola un vehículo (se añade según prioridad)
    public void addVehicle(Vehicle v) {
        switch( v.getDirection()) {
            case "right":
                RightVQueue.add(v);
                break;
            case "straight":
                MidVQueue.add(v);
                break;
            case "left":
                LeftVQueue.add(v);
                break;
            case "u-turn":
                LeftVQueue.add(v);
                break;
            default:
                throw new IllegalArgumentException("Invalid direction: " + v.getDirection());
        }

    }

    // Devuelve sin quitar el vehículo que está al frente de la cola
    public Vehicle peekRightNextV() {
        return RightVQueue.peek();
    }

    public Vehicle peekLeftNextV() {
        return LeftVQueue.peek();
    }
    public Vehicle peekMidNextV() {
        return MidVQueue.peek();
    }

    // Quita de la cola al vehículo que acaba de cruzar
    public void removeRightNextV() {
        RightVQueue.poll();
    }
    public void removeLeftNextV() {
        LeftVQueue.poll();
    }
    public void removeMidNextV() {
        MidVQueue.poll();
    }

    public void setGreenLight(boolean on) {
        this.greenLight.set(on);
    }
    public boolean hasGreenLight() {
        return greenLight.get();
    }

    public Vehicle hasEmergencyVehicleInQueue() {
        for (Vehicle v : RightVQueue) {
            if (v.isEmergency()) {
                return v;
            }
        }
        for (Vehicle v : LeftVQueue) {
            if (v.isEmergency()) {
                return v;
            }
        }
        for (Vehicle v : MidVQueue) {
            if (v.isEmergency()) {
                return v;
            }
        }
        return null;
    }


    public TrafficLight getTrafficLight() {
        return trafficLight;
    }

    public void setTrafficLight(TrafficLight trafficLight) {
        this.trafficLight = trafficLight;
    }
}