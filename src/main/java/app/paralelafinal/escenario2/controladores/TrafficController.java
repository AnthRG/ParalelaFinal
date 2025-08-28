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
 * Gestiona el flujo de tráfico para un conjunto de intersecciones.
 * 
 */
public class TrafficController {

    private final List<Intersection> RightIntersections; 
    private final List<Intersection> LeftIntersections;  
    private final List<Intersection> Intersections;
    private final ScheduledExecutorService scheduler;
    private final ReentrantLock controlLock = new ReentrantLock();
    
    // Performance monitoring
    private long lastStepTime = 0;
    private static final boolean DEBUG_PERFORMANCE = true;

    public TrafficController(List<Intersection> RightIntersections, List<Intersection> LeftIntersections) {
        this.RightIntersections = RightIntersections;
        this.LeftIntersections = LeftIntersections;
        this.Intersections = new ArrayList<>();
        this.Intersections.addAll(RightIntersections);
        this.Intersections.addAll(LeftIntersections);
        // Use multiple threads for better parallelism
        this.scheduler = Executors.newScheduledThreadPool(3);
    }


    /**
     * Inicia la lógica de control de tráfico, programándola para que se ejecute a intervalos fijos.
     */
    public void startControl() {
        InitializeTrafficLights();
        // Changed to 15 seconds for traffic light duration
        scheduler.scheduleAtFixedRate(this::autoLights, 0, 15, TimeUnit.SECONDS);
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

            // Synchronized traffic light control
            // When East/West have green, North/South have red and vice versa
            boolean currentGreenForEastWest = RightIntersections.get(0).getTrafficLight().isGreen();
            
            // Toggle all lights together
            RightIntersections.forEach(intersection -> {
                TrafficLight light = intersection.getTrafficLight();
                light.changeLight();
            });

            LeftIntersections.forEach(intersection -> {
                TrafficLight light = intersection.getTrafficLight();
                light.changeLight();
            });
            
            // Removed traffic light change logs for cleaner output
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
            
            // Find which intersection and lane has the emergency vehicle
            Intersection targetIntersection = null;
            String emergencyLane = null;
            
            for (Intersection intersection : Intersections) {
                if (intersection.getRightVQueue().contains(emergency)) {
                    targetIntersection = intersection;
                    emergencyLane = "right";
                    break;
                } else if (intersection.getMidVQueue().contains(emergency)) {
                    targetIntersection = intersection;
                    emergencyLane = "straight";
                    break;
                } else if (intersection.getLeftVQueue().contains(emergency)) {
                    targetIntersection = intersection;
                    emergencyLane = "left";
                    break;
                } else if (intersection.getUTurnVQueue().contains(emergency)) {
                    targetIntersection = intersection;
                    emergencyLane = "uturn";
                    break;
                }
            }
            
            if (targetIntersection == null) {
                return;
            }
            
            // Only give green light to the intersection with the emergency vehicle
            // NOT all intersections - this was causing the freeze
            TrafficLight targetLight = targetIntersection.getTrafficLight();
            if (targetLight != null && !targetLight.isGreen()) {
                targetLight.getGreen().set(true);
                
                // If East has emergency, turn off West lights and vice versa
                for (Intersection other : Intersections) {
                    if (other != targetIntersection) {
                        // Only turn off if it's a conflicting direction
                        boolean isConflicting = (targetIntersection.getId().startsWith("East") && other.getId().startsWith("West")) ||
                                              (targetIntersection.getId().startsWith("West") && other.getId().startsWith("East"));
                        if (isConflicting) {
                            TrafficLight otherLight = other.getTrafficLight();
                            if (otherLight != null) {
                                otherLight.getGreen().set(false);
                            }
                        }
                    }
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
     * Detiene el planificador de control de trafico
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
        // Debug: Track processing time
        long startTime = System.currentTimeMillis();
        
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
        // LeftIntersections are East intersections, moving westbound (left)
        for (int idx = 0; idx < leftCopy.size(); idx++) {
            Intersection current = leftCopy.get(idx);
            Intersection next = (idx + 1 < leftCopy.size()) ? leftCopy.get(idx + 1) : null;
            boolean isWestbound = current.getId().startsWith("East"); // East vehicles move west
            processLane(current, next, isWestbound);
        }
        
        // RightIntersections are West intersections, moving eastbound (right)
        for (int idx = 0; idx < rightCopy.size(); idx++) {
            Intersection current = rightCopy.get(idx);
            Intersection next = (idx + 1 < rightCopy.size()) ? rightCopy.get(idx + 1) : null;
            boolean isWestbound = current.getId().startsWith("East"); // East vehicles move west
            processLane(current, next, isWestbound);
        }
        
        // Debug: Log if processing takes too long
        long processingTime = System.currentTimeMillis() - startTime;
        if (processingTime > 100) {
            System.out.println("[DEBUG] Step vehicles took " + processingTime + "ms - SLOW!");
        }
    }

    private void processLane(Intersection current, Intersection next, boolean westbound) {
        if (current == null) return;
        TrafficLight light = current.getTrafficLight();
        
        
        
        // Procesar todos los carriles siempre
        processAllVehiclesInQueue(current.getMidVQueue(), "straight", current, next, westbound, true);
        processAllVehiclesInQueue(current.getRightVQueue(), "right", current, next, westbound, true);
        processAllVehiclesInQueue(current.getLeftVQueue(), "left", current, next, westbound, true);
        
        // U-turns también pueden avanzar siempre
        processUTurnVehicles(current, westbound);
    }

    private void processAllVehiclesInQueue(PriorityBlockingQueue<Vehicle> queue, String sourceLane, 
                                     Intersection current, Intersection next, boolean westbound, boolean canStartNew) {
        if (queue.isEmpty()) return;
        
        // SIMPLIFICACIÓN: Procesar múltiples vehículos por ciclo
        int maxVehiclesPerCycle = 5; // Aumentado aún más para mejor fluidez
        int processed = 0;
        
        // Use iterator to avoid copying the entire queue
        for (Vehicle v : queue) {
            if (v == null || processed >= maxVehiclesPerCycle) break;
            
            String direction = v.getDirection().toLowerCase();
            
            
            boolean canProceed = true;
            
            // Para movimientos norte-sur, verificar prioridad por tiempo de llegada
            if (direction.startsWith("left-north") || direction.startsWith("right-south") ||
                direction.startsWith("left-south") || direction.startsWith("right-north")) {
                // Verificar si hay conflicto con otro vehículo que va al mismo destino
                canProceed = checkNorthSouthPriority(v, current, direction);
            }
            
            // Si no puede proceder por prioridad, esperar
            if (!canProceed) {
                break; // Mantener orden de cola
            }
            
            // Check for collisions with vehicles ahead
            if (!canMoveWithoutCollision(v, current, westbound)) {
                break; // Stop if this vehicle can't move - maintains queue order
            }
            
            // Process the vehicle based on its type
            if (direction.startsWith("left-north") || direction.startsWith("right-south") ||
                direction.startsWith("left-south") || direction.startsWith("right-north")) {
                processSpecialTurnVehicle(v, current, westbound, queue);
            } else {
                // For vehicles that completed U-turn, ensure they stay in left lane
                String actualLane = sourceLane;
                if ("left".equalsIgnoreCase(sourceLane) && "left".equalsIgnoreCase(v.getDirection())) {
                    actualLane = "left"; // Ensure left lane vehicles stay in left lane
                }
                
                processVehicleMovement(v, actualLane, current, next, westbound, queue);
            }
            processed++;
        }
    }
    
    // Nuevo método para verificar prioridad en intersecciones norte-sur
    private boolean checkNorthSouthPriority(Vehicle vehicle, Intersection current, String direction) {
        // Determinar el destino del vehículo (norte o sur)
        boolean goingNorth = direction.contains("north");
        
        // Buscar en todas las colas de la intersección actual si hay otro vehículo
        // que vaya al mismo destino vertical y haya llegado antes
        List<PriorityBlockingQueue<Vehicle>> allQueues = List.of(
            current.getRightVQueue(),
            current.getMidVQueue(),
            current.getLeftVQueue()
        );
        
        for (PriorityBlockingQueue<Vehicle> queue : allQueues) {
            for (Vehicle other : queue) {
                if (other != vehicle && other.getDirection() != null) {
                    String otherDir = other.getDirection().toLowerCase();
                    
                    // Verificar si el otro vehículo va al mismo destino vertical
                    boolean otherGoingNorth = otherDir.contains("north");
                    boolean sameDestination = (goingNorth && otherGoingNorth) || (!goingNorth && !otherGoingNorth);
                    
                    // Si van al mismo destino y el otro llegó primero, este vehículo debe esperar
                    if (sameDestination && otherDir.startsWith("left-") || otherDir.startsWith("right-")) {
                        if (other.getArrivalTime() < vehicle.getArrivalTime()) {
                            // El otro vehículo tiene prioridad
                            return false;
                        }
                    }
                }
            }
        }
        
        return true; // Este vehículo tiene prioridad o no hay conflicto
    }

    private void processVehicleMovement(Vehicle v, String sourceLane, Intersection current, 
                                    Intersection next, boolean westbound, PriorityBlockingQueue<Vehicle> queue) {
    
        Point2D pos = v.getPosition();
        String direction = v.getDirection().toLowerCase();
        
        // Check if this is a vehicle that should continue moving vertically
        if (direction.equals("vertical-north") || direction.equals("vertical-south")) {
            // These vehicles should ONLY move vertically, never horizontally 
            double verticalSpeed = 8.0;
            double newY;
            
            if (direction.equals("vertical-north")) {
                newY = pos.getY() - verticalSpeed; // Move up
                if (newY < -20) {
                    queue.remove(v); // Remove when off screen
                } else {
                    // Keep same X position, only change Y
                    v.setPosition(new Point2D(pos.getX(), newY));
                }
            } else if (direction.equals("vertical-south")) {
                newY = pos.getY() + verticalSpeed; // Move down
                if (newY > SimulationConfig.SCENE_HEIGHT + 20) {
                    queue.remove(v); // Remove when off screen
                } else {
                    // Keep same X position, only change Y
                    v.setPosition(new Point2D(pos.getX(), newY));
                }
            }
            return; // Exit early - no horizontal movement for vertical vehicles
        }
        
        // Normal horizontal movement for other vehicles
        double speed = 8.0; 
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
    
    private void processSpecialTurnVehicle(Vehicle v, Intersection current, boolean westbound, 
                                          PriorityBlockingQueue<Vehicle> queue) {
        String direction = v.getDirection().toLowerCase();
        Point2D pos = v.getPosition();
        double speed = 7.0; // INCREASED SPEED for special turns (north/south)
        
        // Get intersection center X position
        double[] centers = verticalCenters();
        double intersectionCenterX;
        
        // Determine if this is a "second" variant (uses extended position like u-turn-second)
        boolean isSecondVariant = direction.contains("second");
        
        // Check if vehicle needs to advance to next intersection first (for "second" variants)
        if (isSecondVariant && v.getUTurnPhase() == 0 && !v.getId().contains("_advancing")) {
            // Mark as advancing and move to next intersection
            Intersection nextIntersection = findNextIntersection(current, westbound);
            if (nextIntersection != null) {
                v.setId(v.getId() + "_advancing");
                // Use similar logic as u-turn-2nd advance
                double moveSpeed = 5.0;
                double dx = westbound ? -moveSpeed : moveSpeed;
                v.setPosition(new Point2D(pos.getX() + dx, pos.getY()));
                
                double targetX = intersectionX(nextIntersection.getId());
                boolean arrived = westbound ? pos.getX() <= targetX : pos.getX() >= targetX;
                
                if (arrived) {
                    queue.remove(v);
                    v.setInIntersection(nextIntersection.getId());
                    v.setPosition(new Point2D(targetX, pos.getY()));
                    // Re-add to the appropriate queue at the new intersection
                    if (direction.startsWith("left-north") || direction.startsWith("left-south")) {
                        nextIntersection.getLeftVQueue().add(v);
                    } else {
                        nextIntersection.getRightVQueue().add(v);
                    }
                }
                return;
            }
        }
        
        // Determine intersection center based on variant
        if (v.getId().contains("_advancing")) {
            intersectionCenterX = intersectionX(current.getId());
        } else {
            intersectionCenterX = current.getId().startsWith("East") ? centers[1] : centers[0];
        }
        
        switch (v.getUTurnPhase()) {
            case 0: // Approaching intersection center
                double dx = westbound ? -speed : speed;
                v.setPosition(new Point2D(pos.getX() + dx, pos.getY()));
                
                // Check if reached turning point
                boolean reachedCenter = Math.abs(pos.getX() - intersectionCenterX) < 15;
                
                if (reachedCenter) {
                    // For "second" variants that have advanced, need to continue to extended position
                    if (v.getId().contains("_advancing")) {
                        // Mark for extended movement
                        v.setUTurnPhase(3); // New phase for extended movement
                        // Commented out for performance
                        // System.out.println("Vehicle " + v.getId() + " (" + direction + ") continuing to extended position from " + 
                        //                  current.getId());
                    } else {
                        // Regular variants start turning immediately
                        v.setUTurnPhase(1);
                        // System.out.println("Vehicle " + v.getId() + " (" + direction + ") starting turn at " + 
                        //                  current.getId() + " at X: " + v.getPosition().getX());
                    }
                }
                break;
                
            case 1: // Making the 90-degree turn
                double horizRoadY = (SimulationConfig.SCENE_HEIGHT - SimulationConfig.ROAD_WIDTH) / 2;
                double verticalRoadWidth = SimulationConfig.ROAD_WIDTH;
                double targetY;
                String finalDirection;
                
                // Determine target Y position and final direction based on vehicle type
                if (direction.startsWith("left-north") || direction.startsWith("right-north")) {
                    // Turn north (upward) - go to top of screen
                    targetY = 50; // Target position at top of vertical road
                    finalDirection = "north";
                } else if (direction.startsWith("right-south") || direction.startsWith("left-south")) {
                    // Turn south (downward) - go to bottom of screen
                    targetY = SimulationConfig.SCENE_HEIGHT - 50; // Target position at bottom of vertical road
                    finalDirection = "south";
                } else {
                    targetY = pos.getY(); // Shouldn't happen
                    finalDirection = "unknown";
                }
                
                // Move toward target Y position
                double dy = targetY - pos.getY();
                if (Math.abs(dy) > speed) {
                    double moveY = dy > 0 ? speed : -speed;
                    v.setPosition(new Point2D(pos.getX(), pos.getY() + moveY));
                } else {
                    // Reached target position, complete the turn
                    // Adjust X position for better lane alignment based on specific vehicle type
                    double adjustedX = pos.getX();
                    
                    // West vehicles need specific adjustments
                    if (current.getId().startsWith("West")) {
                        if (direction.equals("left-north-first")) {
                            adjustedX -= 3; 
                        } else if (direction.equals("right-south-first")) {
                            adjustedX -= 3; 
                        }
                    }
                    // East vehicles
                    else if (current.getId().startsWith("East")) {
                        if (direction.equals("right-north-second")) {
                            adjustedX -= 2;
                        }
                    }
                    
                    v.setPosition(new Point2D(adjustedX, targetY));
                    v.setUTurnPhase(2);
                    
                    // Continue moving vertically after turn
                    v.setDirection("vertical-" + finalDirection);
                    
                    System.out.println("Vehicle " + v.getId() + " completed 90-degree turn to " + 
                                     finalDirection + " at position (" + adjustedX + ", " + targetY + ")");
                }
                break;
                
            case 2: // Continue moving vertically after turn
                // Move north or south - ONLY VERTICAL MOVEMENT
                double verticalSpeed = 8.0; // Más del doble de velocidad
                double newY;
                if (v.getDirection().contains("north")) {
                    newY = pos.getY() - verticalSpeed; // Move up
                    if (newY < -20) {
                        queue.remove(v); // Remove when off screen
                    } else {
                        // Keep same X position, only change Y
                        v.setPosition(new Point2D(pos.getX(), newY));
                    }
                } else if (v.getDirection().contains("south")) {
                    newY = pos.getY() + verticalSpeed; // Move down
                    if (newY > SimulationConfig.SCENE_HEIGHT + 20) {
                        queue.remove(v); // Remove when off screen
                    } else {
                        // Keep same X position, only change Y
                        v.setPosition(new Point2D(pos.getX(), newY));
                    }
                }
                break;
                
            case 3: // Extended movement for "second" variants
                // Continue moving horizontally to extended position before turning
                // This matches the u-turn-second behavior
                double extendedSpeed = 6.0; // INCREASED SPEED for extended movement
                double extraDistance = 400; // Same distance as u-turn-second
                double targetExtendedX = westbound ? 
                    intersectionCenterX - extraDistance : 
                    intersectionCenterX + extraDistance;
                
                double dxExtended = westbound ? -extendedSpeed : extendedSpeed;
                
                // Move gradually toward the extended position
                v.setPosition(new Point2D(pos.getX() + dxExtended, pos.getY()));
                
                // Check if reached extended position
                double distanceToExtended = Math.abs(pos.getX() - targetExtendedX);
                if (distanceToExtended <= 5) {
                    // Reached extended position, now start turning
                    v.setPosition(new Point2D(targetExtendedX, pos.getY()));
                    v.setUTurnPhase(1);
                    System.out.println("Vehicle " + v.getId() + " (" + direction + ") starting turn at extended position " + 
                                     current.getId() + " at X: " + targetExtendedX);
                }
                break;
        }
    }
    
    private void processUTurnVehicles(Intersection current, boolean westbound) {
        // U-turns now come from the dedicated UTurnVQueue
        PriorityBlockingQueue<Vehicle> uTurnQueue = current.getUTurnVQueue();
        
        if (uTurnQueue.isEmpty()) return;
        
        List<Vehicle> uTurnVehicles = new ArrayList<>();
        for (Vehicle v : uTurnQueue) {
            if ("u-turn".equalsIgnoreCase(v.getDirection()) || "u-turn-second".equalsIgnoreCase(v.getDirection())) {
                uTurnVehicles.add(v);
            }
        }
        
        for (Vehicle v : uTurnVehicles) {
            if (!canMoveWithoutCollision(v, current, westbound)) {
                continue;
            }
            
            // For u-turn-second vehicles that haven't been marked as "advancing"
            if ("u-turn-second".equalsIgnoreCase(v.getDirection()) && v.getUTurnPhase() == 0 && 
                !v.getId().contains("_advancing")) {
                // Find next intersection in the same direction
                Intersection nextIntersection = findNextIntersection(current, westbound);
                if (nextIntersection != null) {
                    // Mark vehicle as advancing to prevent re-processing
                    v.setId(v.getId() + "_advancing");
                    processUTurn2ndAdvance(v, current, nextIntersection, westbound, uTurnQueue);
                } else {
                    // If no next intersection, treat as regular u-turn
                    processUTurnMovement(v, current, westbound, uTurnQueue);
                }
            } else if (!"u-turn-second".equalsIgnoreCase(v.getDirection()) || 
                      v.getId().contains("_advancing")) {
                // Process regular u-turn or u-turn-2nd that has arrived at target
                processUTurnMovement(v, current, westbound, uTurnQueue);
            }
        }
    }
    
    private void processUTurn2ndAdvance(Vehicle v, Intersection current, Intersection next, 
                                        boolean westbound, PriorityBlockingQueue<Vehicle> queue) {
        Point2D pos = v.getPosition();
        double speed = 8.0; // INCREASED SPEED to reach next intersection faster
        double dx = westbound ? -speed : speed;
        
        // Move toward next intersection
        v.setPosition(new Point2D(pos.getX() + dx, pos.getY()));
        
        // Check if reached next intersection
        double targetX = intersectionX(next.getId());
        boolean arrived = westbound ? v.getPosition().getX() <= targetX : v.getPosition().getX() >= targetX;
        
        System.out.println("Vehicle " + v.getId() + " (u-turn-2nd) moving from " + current.getId() + 
                         " to " + next.getId() + " at position (" + v.getPosition().getX() + 
                         ", " + v.getPosition().getY() + ") target X: " + targetX);
        
        if (arrived) {
            // Remove from current queue
            queue.remove(v);
            
            // Update vehicle's intersection
            v.setInIntersection(next.getId());
            v.setPosition(new Point2D(targetX, pos.getY()));
            
            // Add to next intersection's U-turn queue to perform the turn there
            next.getUTurnVQueue().add(v);
            
            System.out.println("Vehicle " + v.getId() + " (u-turn-2nd) arrived at " + 
                             next.getId() + " for U-turn execution at position (" + 
                             targetX + ", " + pos.getY() + ")");
        }
    }
    
    private Intersection findNextIntersection(Intersection current, boolean westbound) {
        String currentId = current.getId();
        
        if (westbound) {
            // East vehicles moving west
            if (currentId.equals("East1")) return findIntersectionById("East2");
            if (currentId.equals("East2")) return findIntersectionById("East3");
            // East3 has no next intersection westbound
        } else {
            // West vehicles moving east  
            if (currentId.equals("West1")) return findIntersectionById("West2");
            if (currentId.equals("West2")) return findIntersectionById("West3");
            // West3 has no next intersection eastbound
        }
        
        return null;
    }
    
    private Intersection findIntersectionById(String id) {
        for (Intersection i : Intersections) {
            if (i.getId().equals(id)) {
                return i;
            }
        }
        return null;
    }
    
    private void processUTurnMovement(Vehicle v, Intersection current, boolean westbound, PriorityBlockingQueue<Vehicle> queue) {
        Point2D pos = v.getPosition();
        double speed = 7.0; 
        
        // Get intersection center X position
        double[] centers = verticalCenters();
        double intersectionCenterX;
        
        // For u-turn-2nd vehicles, use the actual intersection position
        if (v.getId().contains("_advancing")) {
            // This is a u-turn-2nd that should turn at its current intersection
            intersectionCenterX = intersectionX(current.getId());
            // Debug removed - was printing too often
        } else {
            // Regular u-turn uses the vertical center
            intersectionCenterX = current.getId().startsWith("East") ? centers[1] : centers[0];
        }
        
        switch (v.getUTurnPhase()) {
            case 0: // Approaching intersection center
                double dx = westbound ? -speed : speed;
                
                // For u-turn-2nd vehicles, check if we need to continue to extended position
                if (v.getId().contains("_advancing")) {
                    double extraDistance = 400; // Distance needed for proper u-turn-2nd positioning
                    double targetX = westbound ? intersectionCenterX - extraDistance : intersectionCenterX + extraDistance;
                    
                    // Move gradually toward the extended position
                    double distanceToTarget = Math.abs(pos.getX() - targetX);
                    if (distanceToTarget > 5) {
                        // Continue moving toward the extended position
                        v.setPosition(new Point2D(pos.getX() + dx, pos.getY()));
                    } else {
                        // Reached extended position, start turning
                        v.setPosition(new Point2D(targetX, pos.getY()));
                        v.setUTurnPhase(1);
                        System.out.println("Vehicle " + v.getId() + " starting U-turn at extended position " + 
                                         current.getId() + " at X: " + targetX);
                    }
                } else {
                    // Regular u-turn - move to intersection center
                    v.setPosition(new Point2D(pos.getX() + dx, pos.getY()));
                    
                    // Check if reached intersection center for turning
                    boolean reachedCenter = Math.abs(pos.getX() - intersectionCenterX) < 15;
                    
                    if (reachedCenter) {
                        v.setUTurnPhase(1);
                        System.out.println("Vehicle " + v.getId() + " starting U-turn at intersection " + 
                                         current.getId() + " at X: " + v.getPosition().getX());
                    }
                }
                break;
                
            case 1: // Making the U-turn (turning around)
                // Calculate target lane Y position - LEFT LANE of opposite direction
                double horizRoadY = (SimulationConfig.SCENE_HEIGHT - SimulationConfig.ROAD_WIDTH) / 2;
                double laneWidth = SimulationConfig.ROAD_WIDTH / 3;
                double targetY;
                
                if (current.getId().startsWith("East")) {
                    // East vehicles turning to West: enter West's LEFT lane (top lane for West)
                    targetY = horizRoadY + laneWidth * 0.5 + app.paralelafinal.config.LanePositionAdjustment.WEST_LEFT_OFFSET;
                } else {
                    // West vehicles turning to East: enter East's LEFT lane (bottom lane for East)
                    targetY = horizRoadY + laneWidth * 2.5 + app.paralelafinal.config.LanePositionAdjustment.EAST_LEFT_OFFSET;
                    
                    // For West1, adjust 2 pixels to the left (which means moving down when horizontal)
                    if (current.getId().equals("West1") && !v.getId().contains("_advancing")) {
                        targetY = targetY + 2; // Move 2 pixels down (appears as left when vehicle is horizontal pointing south)
                    }
                }
                
                // Move toward target Y position
                double dy = targetY - pos.getY();
                if (Math.abs(dy) > speed) {
                    double moveY = dy > 0 ? speed : -speed;
                    v.setPosition(new Point2D(pos.getX(), pos.getY() + moveY));
                } else {
                    // Reached target lane, complete the turn and transfer to opposite intersection
                    v.setUTurnPhase(2);
                    
                    // Transfer to opposite intersection's LEFT queue
                    Intersection oppositeIntersection = findOppositeIntersection(current);
                    if (oppositeIntersection != null) {
                        // Remove from current U-turn queue
                        queue.remove(v);
                        
                        // Update vehicle properties for the new direction - keep as same vehicle
                        v.setDirection("left");
                        v.setGoal(oppositeIntersection.getId());
                        v.setInIntersection(oppositeIntersection.getId());
                        v.setUTurnPhase(0); // Reset phase
                        
                        // Keep current X position but update Y to the correct left lane
                        v.setPosition(new Point2D(pos.getX(), targetY));
                        
                        // Add the SAME vehicle to the left queue of the opposite intersection
                        oppositeIntersection.getLeftVQueue().add(v);
                        
                        System.out.println("Vehicle " + v.getId() + " completed U-turn and transferred to " + 
                                         oppositeIntersection.getId() + " left queue at position (" + 
                                         pos.getX() + ", " + targetY + ")");
                        System.out.println("Vehicle direction is now: " + v.getDirection());
                        System.out.println("Left queue size: " + oppositeIntersection.getLeftVQueue().size());
                        System.out.println("Mid queue size: " + oppositeIntersection.getMidVQueue().size());
                    }
                }
                break;
        }
    }
    
    private Intersection findOppositeIntersection(Intersection current) {
        String currentId = current.getId();
        String targetId = null;
        
        if (currentId.startsWith("East")) {
            // Extract the number and map to corresponding West intersection
            String number = currentId.substring(4); // Get the number part (1, 2, 3)
            targetId = "West" + number;
        } else if (currentId.startsWith("West")) {
            // Extract the number and map to corresponding East intersection
            String number = currentId.substring(4); // Get the number part (1, 2, 3)
            targetId = "East" + number;
        }
        
        if (targetId != null) {
            for (Intersection i : Intersections) {
                if (i.getId().equals(targetId)) {
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
        double x1 = centers[0]; // First vertical road center
        double x2 = centers[1]; // Second vertical road center
        
        // Map each intersection to its corresponding vertical road
        if (intersectionId.equalsIgnoreCase("East1")) return x2; // East1 at second vertical road
        if (intersectionId.equalsIgnoreCase("East2")) return x1; // East2 at first vertical road
        if (intersectionId.equalsIgnoreCase("East3")) return x1 - 200; // East3 further west
        
        if (intersectionId.equalsIgnoreCase("West1")) return x1; // West1 at first vertical road
        if (intersectionId.equalsIgnoreCase("West2")) return x2; // West2 at second vertical road
        if (intersectionId.equalsIgnoreCase("West3")) return x2 + 200; // West3 further east
        
        return (x1 + x2) / 2.0; // Default fallback
    }

    // Check if vehicle can move without colliding with vehicles ahead
    private boolean canMoveWithoutCollision(Vehicle movingVehicle, Intersection intersection, boolean westbound) {
        if (movingVehicle == null || movingVehicle.getPosition() == null) return false;
        
        // IMPORTANTE: Las emergencias NO pueden pasar por encima de otros vehículos
        // Deben esperar si hay un vehículo adelante, pero los vehículos normales
        // en el mismo carril que la emergencia también tienen prioridad
        
        // Primero verificar si este vehículo está en el mismo carril que una emergencia
        boolean inEmergencyLane = isInSameLaneAsEmergency(movingVehicle, intersection);
        
        if (movingVehicle.isEmergency()) {
            // Poner semáforo en verde para el carril de emergencias
            TrafficLight light = intersection.getTrafficLight();
            if (light != null && !light.isGreen()) {
                light.getGreen().set(true);
            }
            // PERO la emergencia debe verificar colisiones también
        }
        
        double minSafeDistance = SimulationConfig.VEHICLE_LENGTH + 25; // Aumentado para más seguridad
        Point2D movingPos = movingVehicle.getPosition();
        String movingDirection = movingVehicle.getDirection().toLowerCase();
        
        // Determine if this vehicle is moving vertically
        boolean isMovingVertically = movingDirection.equals("vertical-north") || 
                                    movingDirection.equals("vertical-south");
        
        // Verificar colisiones con TODAS las intersecciones para mayor seguridad
        for (Intersection checkIntersection : Intersections) {
            List<PriorityBlockingQueue<Vehicle>> allQueues = List.of(
                checkIntersection.getMidVQueue(),
                checkIntersection.getRightVQueue(), 
                checkIntersection.getLeftVQueue(),
                checkIntersection.getUTurnVQueue()
            );
            
            for (PriorityBlockingQueue<Vehicle> queue : allQueues) {
                for (Vehicle other : queue) {
                    if (other != movingVehicle && other.getPosition() != null) {
                        boolean potentialCollision = false;
                        
                        if (isMovingVertically) {
                            // For vertically moving vehicles, check vertical collisions
                            potentialCollision = isTooCloseVertically(movingPos, other.getPosition(), 
                                                                     minSafeDistance, 
                                                                     movingDirection.contains("north"));
                        } else {
                            // For horizontally moving vehicles, check horizontal collisions
                            potentialCollision = isTooClose(movingPos, other.getPosition(), 
                                                           minSafeDistance, westbound);
                        }
                        
                        // Si hay potencial colisión, verificar quién tiene prioridad
                        if (potentialCollision) {
                            // IMPORTANTE: Solo detener si realmente el otro tiene prioridad
                            // Para evitar deadlocks, ser más específico sobre quién debe detenerse
                            
                            // Entre emergencias, prioridad a la más antigua
                            if (movingVehicle.isEmergency() && other.isEmergency()) {
                                // Ambas son emergencias, prioridad a la que llegó primero
                                if (other.getArrivalTime() < movingVehicle.getArrivalTime()) {
                                    System.out.println("[EMERGENCY PRIORITY] Emergency " + movingVehicle.getId() + 
                                                     " waiting for older emergency " + other.getId());
                                    return false; // La otra emergencia tiene prioridad
                                }
                                // Si esta emergencia es más antigua o igual, puede continuar
                                System.out.println("[EMERGENCY PRIORITY] Emergency " + movingVehicle.getId() + 
                                                 " has priority over " + other.getId());
                            }
                            // Si el otro es emergencia y este no, el otro tiene prioridad
                            else if (other.isEmergency() && !movingVehicle.isEmergency()) {
                                System.out.println("[YIELD TO EMERGENCY] Vehicle " + movingVehicle.getId() + 
                                                 " yielding to emergency " + other.getId());
                                return false; // Ceder paso a la emergencia
                            }
                            // Si este es emergencia y el otro no, este tiene prioridad absoluta
                            else if (movingVehicle.isEmergency() && !other.isEmergency()) {
                                // Esta emergencia tiene prioridad absoluta
                                System.out.println("[EMERGENCY OVERRIDE] Emergency " + movingVehicle.getId() + 
                                                 " has priority over normal vehicle " + other.getId());
                                // La emergencia puede continuar, el otro vehículo debe ceder
                                continue; // Verificar siguiente vehículo
                            }
                            // Si ninguno es emergencia, prioridad al más antiguo
                            else if (other.getArrivalTime() < movingVehicle.getArrivalTime()) {
                                // El otro vehículo llegó primero, tiene prioridad
                                System.out.println("[COLLISION AVOIDED] Vehicle " + movingVehicle.getId() + 
                                                 " (arrival: " + movingVehicle.getArrivalTime() + 
                                                 ") waiting for older vehicle " + other.getId() + 
                                                 " (arrival: " + other.getArrivalTime() + ")");
                                return false;
                            }
                            else if (other.getArrivalTime() == movingVehicle.getArrivalTime()) {
                                // Si llegaron al mismo tiempo, usar ID como desempate
                                if (other.getId().compareTo(movingVehicle.getId()) < 0) {
                                    System.out.println("[TIE BREAKER] Vehicle " + movingVehicle.getId() + 
                                                     " waiting for " + other.getId() + " (same arrival time)");
                                    return false;
                                }
                            }
                            // Si este vehículo es más antiguo, puede continuar
                            System.out.println("[PRIORITY] Vehicle " + movingVehicle.getId() + 
                                             " has priority over " + other.getId());
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
        
    // Verificar si un vehículo está en el mismo carril que una emergencia con mayor prioridad
    private boolean isInSameLaneAsEmergency(Vehicle vehicle, Intersection intersection) {
        // Si este vehículo ES una emergencia, no necesita verificar
        if (vehicle.isEmergency()) {
            return false;
        }
        
        // Buscar si hay una emergencia en la misma cola que este vehículo
        PriorityBlockingQueue<Vehicle> vehicleQueue = null;
        
        if (intersection.getMidVQueue().contains(vehicle)) {
            vehicleQueue = intersection.getMidVQueue();
        } else if (intersection.getRightVQueue().contains(vehicle)) {
            vehicleQueue = intersection.getRightVQueue();
        } else if (intersection.getLeftVQueue().contains(vehicle)) {
            vehicleQueue = intersection.getLeftVQueue();
        } else if (intersection.getUTurnVQueue().contains(vehicle)) {
            vehicleQueue = intersection.getUTurnVQueue();
        }
        
        // Si encontramos la cola del vehículo, verificar si hay emergencias
        if (vehicleQueue != null) {
            for (Vehicle v : vehicleQueue) {
                // No comparar consigo mismo y solo buscar emergencias
                if (v != vehicle && v.isEmergency()) {
                    return true; // Hay una emergencia en el mismo carril
                }
            }
        }
        
        return false;
    }
    
    // Check if another vehicle is too close ahead in the direction of travel (horizontal)
    private boolean isTooClose(Point2D movingPos, Point2D otherPos, double minDistance, boolean westbound) {
        // Verificación más estricta de proximidad
        double xDist = Math.abs(movingPos.getX() - otherPos.getX());
        double yDist = Math.abs(movingPos.getY() - otherPos.getY());
        
        // Si están muy cerca en ambas dimensiones, hay riesgo de colisión
        if (xDist < minDistance && yDist < SimulationConfig.VEHICLE_WIDTH * 2.5) {
            // Verificar si el otro vehículo está en la trayectoria
            if (westbound) {
                // Si vamos hacia el oeste y el otro está a la izquierda
                if (otherPos.getX() < movingPos.getX()) {
                    return true; // Colisión potencial
                }
            } else {
                // Si vamos hacia el este y el otro está a la derecha
                if (otherPos.getX() > movingPos.getX()) {
                    return true; // Colisión potencial
                }
            }
            
            // También verificar si están en el mismo punto (intersección)
            if (xDist < SimulationConfig.VEHICLE_LENGTH && yDist < SimulationConfig.VEHICLE_WIDTH) {
                return true; // Están ocupando el mismo espacio
            }
        }
        
        // Verificación original para vehículos en el mismo carril
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
    
    // Check if another vehicle is too close ahead in the direction of travel (vertical)
    private boolean isTooCloseVertically(Point2D movingPos, Point2D otherPos, double minDistance, boolean movingNorth) {
        // Verificación más estricta de proximidad vertical
        double xDist = Math.abs(movingPos.getX() - otherPos.getX());
        double yDist = Math.abs(movingPos.getY() - otherPos.getY());
        
        // Si están muy cerca en ambas dimensiones, hay riesgo de colisión
        if (yDist < minDistance && xDist < SimulationConfig.VEHICLE_WIDTH * 2.5) {
            // Verificar si el otro vehículo está en la trayectoria
            if (movingNorth) {
                // Si vamos hacia el norte y el otro está arriba
                if (otherPos.getY() < movingPos.getY()) {
                    return true; // Colisión potencial
                }
            } else {
                // Si vamos hacia el sur y el otro está abajo
                if (otherPos.getY() > movingPos.getY()) {
                    return true; // Colisión potencial
                }
            }
            
            // También verificar si están en el mismo punto (intersección)
            if (xDist < SimulationConfig.VEHICLE_WIDTH && yDist < SimulationConfig.VEHICLE_LENGTH) {
                return true; // Están ocupando el mismo espacio
            }
        }
        
        // Verificación original
        if (Math.abs(movingPos.getX() - otherPos.getX()) > SimulationConfig.VEHICLE_WIDTH * 2) {
            return false; // Different vertical lanes, no collision risk
        }
        
        // Check distance in direction of travel
        if (movingNorth) {
            // Moving north (upward): other vehicle should be above
            double distance = movingPos.getY() - otherPos.getY();
            return distance > 0 && distance < minDistance;
        } else {
            // Moving south (downward): other vehicle should be below
            double distance = otherPos.getY() - movingPos.getY();
            return distance > 0 && distance < minDistance;
        }
    }

}
