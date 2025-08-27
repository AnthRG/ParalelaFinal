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
import java.util.concurrent.PriorityBlockingQueue;
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

    private final List<Intersection> RightIntersections; 
    private final List<Intersection> LeftIntersections;  
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

    // Updated emergency vehicle detection
    private Vehicle HasEmergencyVehicle() {
        Vehicle candidate = null;
        for (Intersection intersection : Intersections) {
            List<PriorityBlockingQueue<Vehicle>> allQueues = List.of(
                intersection.getRightVQueue(),
                intersection.getMidVQueue(),
                intersection.getLeftVQueue(),
                intersection.getUTurnVQueue()
            );
            
            for (PriorityBlockingQueue<Vehicle> queue : allQueues) {
                for (Vehicle v : queue) {
                    if (v.isEmergency() && (candidate == null || v.getArrivalTime() < candidate.getArrivalTime())) {
                        candidate = v;
                    }
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
        // Create local copies to minimize lock time
        List<Intersection> leftCopy;
        List<Intersection> rightCopy;
        
        controlLock.lock();
        try {
            leftCopy = new ArrayList<>(LeftIntersections);
            rightCopy = new ArrayList<>(RightIntersections);
        } finally {
            controlLock.unlock();
        }
        
        // Process without holding the lock
        for (int idx = 0; idx < leftCopy.size(); idx++) {
            Intersection current = leftCopy.get(idx);
            Intersection next = (idx + 1 < leftCopy.size()) ? leftCopy.get(idx + 1) : null;
            processLane(current, next, true);
        }
        
        for (int idx = 0; idx < rightCopy.size(); idx++) {
            Intersection current = rightCopy.get(idx);
            Intersection next = (idx + 1 < rightCopy.size()) ? rightCopy.get(idx + 1) : null;
            processLane(current, next, false);
        }
    }

    private void processLane(Intersection current, Intersection next, boolean westbound) {
        if (current == null) return;
        TrafficLight light = current.getTrafficLight();
        if (light == null || !light.isGreen()) return;

        // Process all queues
        processAllVehiclesInQueue(current.getMidVQueue(), "straight", current, next, westbound);
        processAllVehiclesInQueue(current.getRightVQueue(), "right", current, next, westbound);
        processAllVehiclesInQueue(current.getLeftVQueue(), "left", current, next, westbound);
        
        // Process U-turn vehicles separately from their dedicated queue
        processUTurnVehicles(current, westbound);
    }

    private void processAllVehiclesInQueue(PriorityBlockingQueue<Vehicle> queue, String sourceLane, 
                                     Intersection current, Intersection next, boolean westbound) {
        if (queue.isEmpty()) return;
        
        // Process only a limited number of vehicles per cycle to prevent freezing
        int maxVehiclesPerCycle = 3;
        int processed = 0;
        
        // Use iterator to avoid copying the entire queue
        for (Vehicle v : queue) {
            if (v == null || processed >= maxVehiclesPerCycle) break;
            
            if (!canMoveWithoutCollision(v, current, westbound)) {
                break; // Stop if this vehicle can't move
            }
            
            processVehicleMovement(v, sourceLane, current, next, westbound, queue);
            processed++;
        }
    }

    private void processVehicleMovement(Vehicle v, String sourceLane, Intersection current, 
                                    Intersection next, boolean westbound, PriorityBlockingQueue<Vehicle> queue) {
    
        Point2D pos = v.getPosition();
        double speed = 5.0;
        double dx = westbound ? -speed : speed;
        v.setPosition(new Point2D(pos.getX() + dx, pos.getY()));

       
        if (next != null) {
            double targetX = intersectionX(next.getId());
            boolean arrived = westbound ? v.getPosition().getX() <= targetX : v.getPosition().getX() >= targetX;
            if (arrived) {
                queue.remove(v); 
                v.setPosition(new Point2D(targetX, v.getPosition().getY()));

               
                next.addVehicleToQueue(v, sourceLane); 
            }
        } else {
            
            if (westbound && v.getPosition().getX() < -20) {
                queue.remove(v);
            } else if (!westbound && v.getPosition().getX() > SimulationConfig.SCENE_WIDTH + 20) {
                queue.remove(v);
            }
        }
    }
    
    private void processUTurnVehicles(Intersection current, boolean westbound) {
        // U-turns now come from the dedicated UTurnVQueue
        PriorityBlockingQueue<Vehicle> uTurnQueue = current.getUTurnVQueue();
        
        if (uTurnQueue.isEmpty()) return;
        
        List<Vehicle> uTurnVehicles = new ArrayList<>();
        for (Vehicle v : uTurnQueue) {
            if ("u-turn".equalsIgnoreCase(v.getDirection()) || "u-turn-2nd".equalsIgnoreCase(v.getDirection())) {
                uTurnVehicles.add(v);
            }
        }
        
        for (Vehicle v : uTurnVehicles) {
            if (!canMoveWithoutCollision(v, current, westbound)) {
                continue;
            }
            
            processUTurnMovement(v, current, westbound, uTurnQueue);
        }
    }
    
    private void processUTurnMovement(Vehicle v, Intersection current, boolean westbound, PriorityBlockingQueue<Vehicle> queue) {
        Point2D pos = v.getPosition();
        double speed = 3.0; // Slower speed for U-turn maneuver
        
        // Get intersection center X position
        double[] centers = verticalCenters();
        double intersectionCenterX = current.getId().startsWith("East") ? centers[1] : centers[0];
        
        switch (v.getUTurnPhase()) {
            case 0: // Approaching intersection center
                double dx = westbound ? -speed : speed;
                v.setPosition(new Point2D(pos.getX() + dx, pos.getY()));
                
                // Check if reached intersection center for turning
                boolean reachedCenter = Math.abs(pos.getX() - intersectionCenterX) < 15;
                if (reachedCenter) {
                    v.setUTurnPhase(1);
                    System.out.println("Vehicle " + v.getId() + " starting U-turn at phase 1");
                }
                break;
                
            case 1: // Making the U-turn (turning around)
                // Calculate target lane Y position
                double horizRoadY = (SimulationConfig.SCENE_HEIGHT - SimulationConfig.ROAD_WIDTH) / 2;
                double laneWidth = SimulationConfig.ROAD_WIDTH / 3;
                double targetY;
                
                if (westbound) {
                    // West vehicles: move from south lane (bottom) to north lane (top)
                    targetY = horizRoadY + laneWidth * 0.5; // Top lane for eastbound traffic
                } else {
                    // East vehicles: move from north lane (top) to south lane (bottom)  
                    targetY = horizRoadY + laneWidth * 2.5; // Bottom lane for westbound traffic
                }
                
                // Move toward target Y position
                double dy = targetY - pos.getY();
                if (Math.abs(dy) > speed) {
                    double moveY = dy > 0 ? speed : -speed;
                    v.setPosition(new Point2D(pos.getX(), pos.getY() + moveY));
                } else {
                    // Reached target lane, complete the turn
                    v.setPosition(new Point2D(pos.getX(), targetY));
                    v.setUTurnPhase(2);
                    System.out.println("Vehicle " + v.getId() + " completed U-turn, entering phase 2");
                }
                break;
                
            case 2: // Exiting in opposite direction
                // Move in opposite direction
                double exitDx = westbound ? speed : -speed; // Opposite direction
                v.setPosition(new Point2D(pos.getX() + exitDx, pos.getY()));
                
                // Check if far enough from intersection to remove from queue
                boolean farEnough = Math.abs(pos.getX() - intersectionCenterX) > 50;
                if (farEnough) {
                    queue.remove(v);
                    v.setUTurnPhase(0); // Reset for potential future use
                    System.out.println("Vehicle " + v.getId() + " completed U-turn exit");
                }
                break;
        }
    }
    
    private Intersection findOppositeIntersection(Intersection current) {
        String currentId = current.getId();
        if (currentId.startsWith("East")) {
            // Find West intersection
            for (Intersection i : Intersections) {
                if (i.getId().startsWith("West")) {
                    return i;
                }
            }
        } else if (currentId.startsWith("West")) {
            // Find East intersection
            for (Intersection i : Intersections) {
                if (i.getId().startsWith("East")) {
                    return i;
                }
            }
        }
        return null;
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
        if (intersectionId.equalsIgnoreCase("West2")) return x1; // westernmost
        return (x1 + x2) / 2.0; // Default fallback
    }

    // Check if vehicle can move without colliding with vehicles ahead
    private boolean canMoveWithoutCollision(Vehicle movingVehicle, Intersection intersection, boolean westbound) {
        if (movingVehicle == null || movingVehicle.getPosition() == null) return false;
        
        double minSafeDistance = SimulationConfig.VEHICLE_LENGTH + 15;
        Point2D movingPos = movingVehicle.getPosition();
        
        // Check against all vehicles in all queues including UTurn
        List<PriorityBlockingQueue<Vehicle>> allQueues = List.of(
            intersection.getMidVQueue(),
            intersection.getRightVQueue(), 
            intersection.getLeftVQueue(),
            intersection.getUTurnVQueue()
        );
        
        for (PriorityBlockingQueue<Vehicle> queue : allQueues) {
            for (Vehicle other : queue) {
                if (other != movingVehicle && other.getPosition() != null) {
                    if (isTooClose(movingPos, other.getPosition(), minSafeDistance, westbound)) {
                        return false;
                    }
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
