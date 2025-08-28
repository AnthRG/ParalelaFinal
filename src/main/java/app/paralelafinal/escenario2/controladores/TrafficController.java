
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
            
            String direction = v.getDirection().toLowerCase();
            
            // Check if this is a special turn movement
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

    private void processVehicleMovement(Vehicle v, String sourceLane, Intersection current, 
                                    Intersection next, boolean westbound, PriorityBlockingQueue<Vehicle> queue) {
    
        Point2D pos = v.getPosition();
        String direction = v.getDirection().toLowerCase();
        
        // Check if this is a vehicle that should continue moving vertically
        if (direction.equals("vertical-north") || direction.equals("vertical-south")) {
            // These vehicles should ONLY move vertically, never horizontally
            double verticalSpeed = 5.0;
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
    
    private void processSpecialTurnVehicle(Vehicle v, Intersection current, boolean westbound, 
                                          PriorityBlockingQueue<Vehicle> queue) {
        String direction = v.getDirection().toLowerCase();
        Point2D pos = v.getPosition();
        double speed = 3.0; // Same speed as U-turn for consistency
        
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
                        System.out.println("Vehicle " + v.getId() + " (" + direction + ") continuing to extended position from " + 
                                         current.getId());
                    } else {
                        // Regular variants start turning immediately
                        v.setUTurnPhase(1);
                        System.out.println("Vehicle " + v.getId() + " (" + direction + ") starting turn at " + 
                                         current.getId() + " at X: " + v.getPosition().getX());
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
                            adjustedX -= 3; // Move 3 pixels left (west) for West left-north-first
                        } else if (direction.equals("right-south-first")) {
                            adjustedX -= 3; // Move 3 pixels left (west) for West right-south-first
                        }
                    }
                    // East vehicles
                    else if (current.getId().startsWith("East")) {
                        if (direction.equals("right-north-second")) {
                            adjustedX -= 2; // Move 2 pixels left for East right-north-second
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
                double verticalSpeed = 5.0;
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
                double extendedSpeed = 3.0; // Use same slower speed as u-turn for consistency
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
        double speed = 5.0; // Normal speed to reach next intersection
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
        double speed = 3.0; // Slower speed for U-turn maneuver
        
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
        
        double minSafeDistance = SimulationConfig.VEHICLE_LENGTH + 15;
        Point2D movingPos = movingVehicle.getPosition();
        String movingDirection = movingVehicle.getDirection().toLowerCase();
        
        // Determine if this vehicle is moving vertically
        boolean isMovingVertically = movingDirection.equals("vertical-north") || 
                                    movingDirection.equals("vertical-south");
        
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
                    if (isMovingVertically) {
                        // For vertically moving vehicles, check vertical collisions
                        if (isTooCloseVertically(movingPos, other.getPosition(), minSafeDistance, 
                                               movingDirection.contains("north"))) {
                            return false;
                        }
                    } else {
                        // For horizontally moving vehicles, check horizontal collisions
                        if (isTooClose(movingPos, other.getPosition(), minSafeDistance, westbound)) {
                            return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    // Check if another vehicle is too close ahead in the direction of travel (horizontal)
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
    
    // Check if another vehicle is too close ahead in the direction of travel (vertical)
    private boolean isTooCloseVertically(Point2D movingPos, Point2D otherPos, double minDistance, boolean movingNorth) {
        // Check if vehicles are in roughly the same vertical lane (X position)
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
