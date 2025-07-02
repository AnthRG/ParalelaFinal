package app.paralelafinal.entidades;

import java.util.concurrent.ThreadLocalRandom;

public class Vehicle {
    private String id;
    private String type; // "normal" or "emergency"
    private String direction; // "right", "straight", "left", "u-turn"
    private boolean inIntersection;
    private long arrivalTime;

    public Vehicle() {}

    public Vehicle(String id, String type) {
        this.id = id;
        this.type = type;
        int randomInt = ThreadLocalRandom.current().nextInt(0,4);
        switch (randomInt) {
            case 0:
                this.direction = "right";
                break;
            case 1:
                this.direction = "straight";
                break;
            case 2:
                this.direction = "left";
                break;
            default:
                this.direction = "u-turn"; // Fallback case
        }
        this.inIntersection = false; // Default state
        this.arrivalTime = System.nanoTime();
    }

    public Vehicle(String id, String type, String direction, boolean inIntersection) {
        this.id = id;
        this.type = type;
        this.direction = direction;
        this.inIntersection = inIntersection;
        this.arrivalTime = System.nanoTime();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public boolean isInIntersection() {
        return inIntersection;
    }

    public void setInIntersection(boolean inIntersection) {
        this.inIntersection = inIntersection;
    }

    public long getArrivalTime() {return arrivalTime;}

    public void setArrivalTime(long arrivalTime) {this.arrivalTime = arrivalTime;}
}