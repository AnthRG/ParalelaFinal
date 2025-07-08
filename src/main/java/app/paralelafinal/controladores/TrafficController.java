package app.paralelafinal.controladores;

import app.paralelafinal.entidades.Intersection;
import app.paralelafinal.entidades.Vehicle;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Gestiona el flujo de tráfico para un conjunto de intersecciones controlando los semáforos.
 * La lógica sigue un estricto sistema de prioridades:
 * 1. Vehículos de emergencia: Se le da luz verde al carril completo hasta que el vehículo de emergencia pase.
 * 2. Primer llegado, primer servido (FIFO): En condiciones normales, el vehículo que ha esperado más tiempo
 * en todo el sistema tiene prioridad para pasar.
 * Solo un carril puede tener luz verde a la vez para evitar colisiones.
 */
public class TrafficController {

    private final List<Intersection> intersections;
    private final ScheduledExecutorService scheduler;

    public TrafficController(List<Intersection> intersections) {
        this.intersections = intersections;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Inicia la lógica de control de tráfico, programándola para que se ejecute a intervalos fijos.
     */
    public void startControl() {
        // La lógica principal se ejecuta cada 2 segundos.
        scheduler.scheduleAtFixedRate(this::manageTrafficFlow, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * Detiene el planificador de control de tráfico.
     */
    public void stopControl() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
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
        // 1. Poner todas las luces en rojo como estado seguro por defecto.
        setAllLights(false);

        // 2. Buscar si existe un vehículo de emergencia en CUALQUIER carril.
        Optional<Intersection> laneWithEmergencyVehicle = findLaneWithEmergencyVehicle();

        if (laneWithEmergencyVehicle.isPresent()) {
            // Si hay un vehículo de emergencia, ese carril tiene la máxima prioridad.
            // Se le da luz verde para que todos los vehículos delante de él puedan avanzar.
            laneWithEmergencyVehicle.get().setGreenLight(true);
        } else {
            // 3. Si no hay emergencias, gestionar el tráfico normal según FIFO.
            handleNormalTrafficFlow();
        }
    }

    /**
     * Gestiona el flujo de tráfico en condiciones normales (sin vehículos de emergencia).
     * Da luz verde al vehículo que ha estado esperando más tiempo en todas las intersecciones.
     */
    private void handleNormalTrafficFlow() {
        // Encuentra el vehículo más antiguo en todas las colas.
        findOldestVehicleInSystem().ifPresent(oldestVehicle -> {
            // Encuentra la intersección de ese vehículo y le da luz verde.
            findIntersectionForVehicle(oldestVehicle).ifPresent(intersection -> intersection.setGreenLight(true));
        });
    }

    /**
     * Busca en todas las intersecciones para encontrar un carril que contenga un vehículo de emergencia.
     * A diferencia de la lógica anterior, esta revisa la cola completa, no solo el primer vehículo.
     *
     * @return Un Optional que contiene la intersección si se encuentra un vehículo de emergencia.
     */
    private Optional<Intersection> findLaneWithEmergencyVehicle() {
        return intersections.stream()
                .filter(Intersection::hasEmergencyVehicleInQueue) // Asume que Intersection tiene este método
                .findFirst();
    }

    /**
     * Obtiene todos los vehículos que esperan en todas las intersecciones.
     * @return Un Stream con todos los vehículos.
     */
    private Stream<Vehicle> getAllWaitingVehicles() {
        return intersections.stream()
                .flatMap(intersection -> intersection.getVehicleQueue().stream()); // Asume que Intersection tiene este método
    }

    /**
     * Encuentra el vehículo que llegó primero entre todos los vehículos del sistema.
     * @return Un Optional que contiene el vehículo con el menor tiempo de llegada.
     */
    private Optional<Vehicle> findOldestVehicleInSystem() {
        return getAllWaitingVehicles()
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
}