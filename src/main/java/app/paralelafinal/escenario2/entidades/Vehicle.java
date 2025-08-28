package app.paralelafinal.escenario2.entidades;

import javafx.geometry.Point2D;

import java.util.concurrent.ThreadLocalRandom;

public class Vehicle {
    private String id;
    private String type; // "normal" or "emergency"
    private String direction; // "right", "straight", "left", "u-turn"
    private String originalDirection; // Store original direction for color consistency
    private String goal; // ID of the intersection of the goal
    private String inIntersection;
    private long arrivalTime;
    private int uTurnPhase = 0; // 0: approaching, 1: turning, 2: exiting

    // Nuevo campo para la posición
    private Point2D position;

    public Vehicle() {}

    public Vehicle(String id) {
        this.id = id;
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

        randomInt = ThreadLocalRandom.current().nextInt(0,1);
        switch (randomInt) {
            case 0:
                this.type = "emergency";
                break;
            case 1:
                this.type = "normal";
                break;
            default:
                this.direction = "normal"; // Fallback case
        }
        this.arrivalTime = System.nanoTime();
        // Posición inicial, puede ajustarse según tu lógica
        this.position = new Point2D(0, 0);
    }

    public Vehicle(String id, String type, String direction, String goal, String inIntersection) {
        this.id = id;
        this.type = type;
        this.direction = direction;
        this.originalDirection = direction; // Store original direction
        this.goal = goal;
        this.inIntersection = inIntersection;
        this.arrivalTime = System.nanoTime();
        // Asigna una posición por defecto
        this.position = new Point2D(0, 0);
    }

    // Getters y setters existentes...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getGoal() {
        return this.goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getInIntersection() {
        return inIntersection;
    }

    public void setInIntersection(String inIntersection) {
        this.inIntersection = inIntersection;
    }

    public int getuTurnPhase() {
        return uTurnPhase;
    }

    public void setuTurnPhase(int uTurnPhase) {
        this.uTurnPhase = uTurnPhase;
    }

    public long getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(long arrivalTime) { this.arrivalTime = arrivalTime; }
    
    // Métodos nuevos para la posición
    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {

        this.position = position;
    }

    // Método para mover el vehículo
    public void move(Point2D movementVector) {
        if (this.position != null && movementVector != null) {
            this.position = this.position.add(movementVector);
        }
    }

    public int getUTurnPhase() {
        return uTurnPhase;
    }

    public void setUTurnPhase(int uTurnPhase) {
        this.uTurnPhase = uTurnPhase;
    }


    public boolean isEmergency() {
        return this.type.equalsIgnoreCase("emergency");
    }
    
    public String getOriginalDirection() {
        return originalDirection;
    }
    
    public void setOriginalDirection(String originalDirection) {
        this.originalDirection = originalDirection;
    }
}