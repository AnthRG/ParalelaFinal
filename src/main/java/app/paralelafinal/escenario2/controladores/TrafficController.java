package app.paralelafinal.escenario2.controladores;

import app.paralelafinal.escenario2.entidades.Intersection;
import app.paralelafinal.escenario2.entidades.TrafficLight;
import app.paralelafinal.escenario2.entidades.Vehicle;
import app.paralelafinal.config.SimulationConfig;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Gestiona el flujo de tráfico para un conjunto de intersecciones controlando los semáforos.
 * La lógica sigue un estricto sistema de prioridades:
 * 1. Vehículos de emergencia: Se le da luz verde al carril completo hasta que el vehículo de emergencia pase.
 * Si hay múltiples vehículos de emergencia, se prioriza el que ha esperado más tiempo.
 * 2. Primer llegado, primer servido: En condiciones normales, el vehículo que ha esperado más tiempo
 * en todo el sistema tiene prioridad para pasar.
 */
public class TrafficController {

    private final List<Intersection> RightIntersections; // West1, West2 (moving eastbound)
    private final List<Intersection> LeftIntersections;  // East1, East2 (moving westbound)
    private final List<Intersection> Intersections;
    private final ScheduledExecutorService scheduler;
    private final ReentrantLock controlLock = new ReentrantLock();

    public TrafficController(List<Intersection> RightIntersections, List<Intersection> LeftIntersections) {
        this.RightIntersections = RightIntersections;
        this.LeftIntersections = LeftIntersections;
        this.Intersections = new ArrayList<>();
        this.Intersections.addAll(RightIntersections);
        this.Intersections.addAll(LeftIntersections);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }


    /**
     * Inicia la lógica de control de tráfico, programándola para que se ejecute a intervalos fijos.
     */
    public void startControl() {
        InitializeTrafficLights();
        scheduler.scheduleAtFixedRate(this::autoLights, 0, 10, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::EmergencyCheck, 3, 4, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::stepVehicles, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void autoLights() {
        controlLock.lock();
        try {
            // Do not toggle lights while an emergency vehicle is present
            if (HasEmergencyVehicle() != null) {
                return;
            }

            RightIntersections.forEach(intersection -> {
                TrafficLight light = intersection.getTrafficLight();
                light.changeLight();
            });

            LeftIntersections.forEach(intersection -> {
                TrafficLight light = intersection.getTrafficLight();
                light.changeLight();
            });
        } finally {
            controlLock.unlock();
        }
    }

    private void InitializeTrafficLights() {
        RightIntersections.forEach(intersection -> intersection.getTrafficLight().changeLight());
        LeftIntersections.forEach(intersection -> intersection.getTrafficLight().changeLight());
    }

    private void EmergencyCheck() {
        controlLock.lock();
        try {
            Vehicle emergency = HasEmergencyVehicle();
            if (emergency == null) {
                return;
            }
            Intersection target = null;
            for (Intersection intersection : Intersections) {
                if (intersection.getRightVQueue().contains(emergency)
                        || intersection.getMidVQueue().contains(emergency)
                        || intersection.getLeftVQueue().contains(emergency)) {
                    target = intersection;
                    break;
                }
            }
            if (target == null) {
                return;
            }
            for (Intersection intersection : Intersections) {
                TrafficLight light = intersection.getTrafficLight();
                if (light != null) {
                    light.getGreen().set(intersection == target);
                }
            }
        } finally {
            controlLock.unlock();
        }
    }

    private Vehicle HasEmergencyVehicle() {
        Vehicle candidate = null;
        for (Intersection intersection : Intersections) {
            for (Vehicle v : intersection.getRightVQueue()) {
                if (v.isEmergency() && (candidate == null || v.getArrivalTime() < candidate.getArrivalTime())) {
                    candidate = v;
                }
            }
            for (Vehicle v : intersection.getMidVQueue()) {
                if (v.isEmergency() && (candidate == null || v.getArrivalTime() < candidate.getArrivalTime())) {
                    candidate = v;
                }
            }
            for (Vehicle v : intersection.getLeftVQueue()) {
                if (v.isEmergency() && (candidate == null || v.getArrivalTime() < candidate.getArrivalTime())) {
                    candidate = v;
                }
            }
        }
        return candidate;
    }


    /**
     * Detiene el planificador de control de tráfico.
     */
    public void stopControl() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(50, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public List<Intersection> getIntersections() {
        return Intersections;
    }

    // Core stepping logic: move the head vehicle of a green-light intersection toward the next intersection
    private void stepVehicles() {
        controlLock.lock();
        try {
            // Process westbound lanes (East1 -> East2)
            for (int idx = 0; idx < LeftIntersections.size(); idx++) {
                Intersection current = LeftIntersections.get(idx);
                Intersection next = (idx + 1 < LeftIntersections.size()) ? LeftIntersections.get(idx + 1) : null;
                processLane(current, next, true);
            }
            // Process eastbound lanes (West1 -> West2)
            for (int idx = 0; idx < RightIntersections.size(); idx++) {
                Intersection current = RightIntersections.get(idx);
                Intersection next = (idx + 1 < RightIntersections.size()) ? RightIntersections.get(idx + 1) : null;
                processLane(current, next, false);
            }
        } finally {
            controlLock.unlock();
        }
    }

    private void processLane(Intersection current, Intersection next, boolean westbound) {
        if (current == null) return;
        TrafficLight light = current.getTrafficLight();
        if (light == null || !light.isGreen()) return;

        // Process all three lanes simultaneously for smoother traffic flow
        processVehicleInLane(current.getMidVQueue().peek(), "straight", current, next, westbound);
        processVehicleInLane(current.getRightVQueue().peek(), "right", current, next, westbound);
        processVehicleInLane(current.getLeftVQueue().peek(), "left", current, next, westbound);
    }
    
    private void processVehicleInLane(Vehicle v, String sourceLane, Intersection current, Intersection next, boolean westbound) {
        if (v == null) return;
        
        // Check for collision with vehicles ahead in the same intersection
        if (!canMoveWithoutCollision(v, current, westbound)) {
            return; // Don't move if there's a vehicle too close ahead
        }

        // Move position toward next intersection or offscreen
        Point2D pos = v.getPosition();
        double speed = 5.0; // Increased speed from 2.5 to 5.0 px per tick
        double dx = westbound ? -speed : speed;
        double dy = 0;
        v.setPosition(new Point2D(pos.getX() + dx, pos.getY() + dy));

        // Determine the x position for the next intersection's vertical road center
        if (next != null) {
            double targetX = intersectionX(next.getId());
            boolean arrived = westbound ? v.getPosition().getX() <= targetX : v.getPosition().getX() >= targetX;
            if (arrived) {
                // dequeue from current
                switch (sourceLane) {
                    case "straight" -> current.getMidVQueue().poll();
                    case "right" -> current.getRightVQueue().poll();
                    case "left" -> current.getLeftVQueue().poll();
                }
                // snap position to target line and enqueue into next
                v.setPosition(new Point2D(targetX, v.getPosition().getY()));
                next.addVehicle(v);
            }
        } else {
            // No next intersection: once off the canvas, remove from current
            if (westbound && v.getPosition().getX() < -20) {
                switch (sourceLane) {
                    case "straight" -> current.getMidVQueue().poll();
                    case "right" -> current.getRightVQueue().poll();
                    case "left" -> current.getLeftVQueue().poll();
                }
            } else if (!westbound && v.getPosition().getX() > SimulationConfig.SCENE_WIDTH + 20) {
                switch (sourceLane) {
                    case "straight" -> current.getMidVQueue().poll();
                    case "right" -> current.getRightVQueue().poll();
                    case "left" -> current.getLeftVQueue().poll();
                }
            }
        }
    }

    // Compute x centers of the two vertical roads used as intersection reference lines
    private double[] verticalCenters() {
        double canvasW = SimulationConfig.SCENE_WIDTH;
        double verticalRoadW = SimulationConfig.ROAD_WIDTH; // 2 lanes total
        double horizontalBlockSize = (canvasW - 2 * verticalRoadW) / 3.0;
        double x1 = horizontalBlockSize + verticalRoadW / 2.0; // center of first vertical road
        double x2 = horizontalBlockSize + verticalRoadW + horizontalBlockSize + verticalRoadW / 2.0; // center of second vertical road
        return new double[]{x1, x2};
    }

    // Map logical intersection IDs to the corresponding vertical road center x
    private double intersectionX(String intersectionId) {
        double[] centers = verticalCenters();
        double x1 = centers[0];
        double x2 = centers[1];
        if (intersectionId.equalsIgnoreCase("East1")) return x2; // easternmost
        if (intersectionId.equalsIgnoreCase("East2")) return x1; // western
        if (intersectionId.equalsIgnoreCase("West1")) return x1; // westernmost
        if (intersectionId.equalsIgnoreCase("West2")) return x2; // eastern
        // Default fallback
        return (x1 + x2) / 2.0;
    }

    // Check if vehicle can move without colliding with vehicles ahead
    private boolean canMoveWithoutCollision(Vehicle movingVehicle, Intersection intersection, boolean westbound) {
        if (movingVehicle == null || movingVehicle.getPosition() == null) return false;
        
        double minSafeDistance = SimulationConfig.VEHICLE_LENGTH + 15; // Minimum safe following distance
        Point2D movingPos = movingVehicle.getPosition();
        
        // Check all vehicles in all queues of the same intersection
        for (Vehicle other : intersection.getMidVQueue()) {
            if (other != movingVehicle && other.getPosition() != null) {
                if (isTooClose(movingPos, other.getPosition(), minSafeDistance, westbound)) {
                    return false;
                }
            }
        }
        for (Vehicle other : intersection.getRightVQueue()) {
            if (other != movingVehicle && other.getPosition() != null) {
                if (isTooClose(movingPos, other.getPosition(), minSafeDistance, westbound)) {
                    return false;
                }
            }
        }
        for (Vehicle other : intersection.getLeftVQueue()) {
            if (other != movingVehicle && other.getPosition() != null) {
                if (isTooClose(movingPos, other.getPosition(), minSafeDistance, westbound)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    // Check if another vehicle is too close ahead in the direction of travel
    private boolean isTooClose(Point2D movingPos, Point2D otherPos, double minDistance, boolean westbound) {
        // Check if vehicles are in roughly the same lane (Y position)
        if (Math.abs(movingPos.getY() - otherPos.getY()) > SimulationConfig.VEHICLE_WIDTH * 2) {
            return false; // Different lanes, no collision risk
        }
        
        // Check distance in direction of travel
        if (westbound) {
            // Moving left (westbound): other vehicle should be to the left
            double distance = movingPos.getX() - otherPos.getX();
            return distance > 0 && distance < minDistance;
        } else {
            // Moving right (eastbound): other vehicle should be to the right
            double distance = otherPos.getX() - movingPos.getX();
            return distance > 0 && distance < minDistance;
        }
    }

}
