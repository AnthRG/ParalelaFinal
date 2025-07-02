package app.paralelafinal.config;

public class SimulationConfig {
    // --- SCENE & ROAD DIMENSIONS ---
    public static final double SCENE_WIDTH = 1200;
    public static final double SCENE_HEIGHT = 900;
    public static final double ROAD_WIDTH = 160;

    // --- ROAD MARKING DIMENSIONS ---
    public static final double DASHED_LINE_LENGTH = 25;
    public static final double DASHED_LINE_GAP = 20;
    public static final double LINE_THICKNESS = 4;
    public static final double CENTER_LINE_GAP = 5;

    // --- TRAFFIC LIGHT DIMENSIONS ---
    public static final double LIGHT_RADIUS = 15;
    public static final double LIGHT_SPACING = 10;
    public static final double TRAFFIC_LIGHT_WIDTH = LIGHT_RADIUS * 2 + 10;
    public static final double TRAFFIC_LIGHT_HEIGHT = LIGHT_RADIUS * 4 + LIGHT_SPACING * 3;
    public static final double TRAFFIC_LIGHT_OFFSET = 35; // Offset mejorado

    // --- SIMULATION TIMING ---
    public static final long TRAFFIC_LIGHT_SWITCH_INTERVAL_SECONDS = 10;
    public static final long VEHICLE_UPDATE_INTERVAL_MS = 1000;

    // --- VEHICLE DIMENSIONS ---
    public static final double VEHICLE_LENGTH = 40;
    public static final double VEHICLE_WIDTH = 20;
    public static final double VEHICLE_SPACING = VEHICLE_LENGTH + 10;

    // --- STOP SIGN OFFSET ---
    public static final double STOP_SIGN_OFFSET = 0;
}