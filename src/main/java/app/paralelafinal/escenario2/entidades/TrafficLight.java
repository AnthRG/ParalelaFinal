package app.paralelafinal.escenario2.entidades;

import java.util.concurrent.atomic.AtomicBoolean;

public class TrafficLight {
    private String id;
    private AtomicBoolean green;

    public TrafficLight(String id, AtomicBoolean green) {
        this.id = id;
        this.green = green;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AtomicBoolean getGreen() {
        return green;
    }

    public void setGreen(AtomicBoolean green) {
        this.green = green;
    }

    public void changeLight() {
        green.set(!green.get());
    }

    public boolean isGreen() {
        return green.get();
    }
}