package app.paralelafinal.escenario1.controladores;

import app.paralelafinal.escenario1.entidades.Intersection;
import app.paralelafinal.escenario1.entidades.Vehicle;

import java.util.*;
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

    private final List<Intersection> intersections;
    private final ScheduledExecutorService scheduler;
    private final ReentrantLock controlLock = new ReentrantLock(); 

    public TrafficController(List<Intersection> intersections) {
        this.intersections = intersections;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Inicia la lógica de control de tráfico, programándola para que se ejecute a intervalos fijos.
     */
    public void startControl() {
        scheduler.scheduleAtFixedRate(this::manageTrafficFlow, 3, 4, TimeUnit.SECONDS);
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

    /**
     * El método central que orquesta la gestión del tráfico en cada ciclo.
     * Decide qué carril obtiene la luz verde basándose en un sistema de prioridades.
     */
    private void manageTrafficFlow() {
        if (controlLock.tryLock()) {
            try {
                setAllLights(false);
                Optional<Intersection> emergencyLane = prioritizeEmergencyLane();

                if (emergencyLane.isPresent()) {
                    emergencyLane.get().setGreenLight(true);
                } else {
                    handleNormalTrafficFlow();
                }
            } finally {
                controlLock.unlock();
            }
        }
    }

    /**
     * Gestiona el flujo de tráfico en condiciones normales (sin vehículos de emergencia).
     * Da luz verde al vehículo que ha estado esperando más tiempo en todas las intersecciones.
     */
    private void handleNormalTrafficFlow() {
        findOldestVehicleInSystem().flatMap(this::findIntersectionForVehicle).ifPresent(intersection -> intersection.setGreenLight(true));
    }

    /**
     *  Busca en todas las intersecciones para encontrar el vehículo de emergencia con mayor tiempo de espera.
     * Esto resuelve el conflicto cuando hay múltiples vehículos de emergencia en el sistema a la vez.
     *
     * @return Un Optional que contiene la intersección del vehículo de emergencia con mayor prioridad.
     */
    private Optional<Intersection> prioritizeEmergencyLane() {
        // Encuentra el vehículo de emergencia más antiguo en todo el sistema.
        Optional<Vehicle> oldestEmergencyVehicle = getAllWaitingVehicles()
                .filter(Vehicle::isEmergency) // Asume que Vehicle tiene el método isEmergency()
                .min(Comparator.comparingLong(Vehicle::getArrivalTime));

        // Si se encontró, busca la intersección a la que pertenece.
        return oldestEmergencyVehicle.flatMap(this::findIntersectionForVehicle);
    }

    /**
     * Obtiene todos los vehículos que esperan en todas las intersecciones.
     * @return Un Stream con todos los vehículos.
     */
    private Stream<Vehicle> getAllWaitingVehicles() {
        return intersections.stream()
                .flatMap(intersection -> intersection.getVehicleQueue().stream()); // Asume que Intersection tiene getVehicleQueue()
    }

    /**
     * Encuentra el vehículo que llegó primero entre todos los vehículos del sistema.
     * @return Un Optional que contiene el vehículo con el menor tiempo de llegada.
     */
    private Optional<Vehicle> findOldestVehicleInSystem() {
        return getAllWaitingVehicles()
                .filter(v -> !v.isEmergency())
                .min(Comparator.comparingLong(Vehicle::getArrivalTime));
    }

    /**
     * Encuentra la intersección que contiene un vehículo específico en su cola.
     * @param vehicle El vehículo a buscar.
     * @return Un Optional que contiene la Intersección, o vacío si no se encuentra.
     */
    private Optional<Intersection> findIntersectionForVehicle(Vehicle vehicle) {
        return intersections.stream()
                .filter(i -> i.getVehicleQueue().contains(vehicle)) // Asume que la cola se puede consultar
                .findFirst();
    }

    /**
     * Establece el estado de todos los semáforos a verde o rojo.
     * @param isGreen True para poner todos los semáforos en verde, false para ponerlos en rojo.
     */
    private void setAllLights(boolean isGreen) {
        intersections.forEach(i -> i.setGreenLight(isGreen));
    }

    public boolean isVehicleAuthorizedToMove(Vehicle vehicle) {
        // Emergencia: si el semáforo está en verde y hay emergencia en la cola, todos pueden avanzar
        Optional<Intersection> intersectionOpt = findIntersectionForVehicle(vehicle);
        if (intersectionOpt.isPresent()) {
            Intersection intersection = intersectionOpt.get();
            if (intersection.hasGreenLight() && intersection.getVehicleQueue().stream().anyMatch(Vehicle::isEmergency)) {
                return true;
            }
        }
        
        Optional<Vehicle> oldest = findOldestVehicleInSystem();
        return oldest.isPresent() && oldest.get().equals(vehicle);
    }
}