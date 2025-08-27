package app.paralelafinal.config;

public class LanePositionAdjustment {

    // Vertical offsets for East lanes
    public static final double EAST_LEFT_OFFSET = -2.4 * (SimulationConfig.ROAD_WIDTH / 3);
    public static final double EAST_STRAIGHT_OFFSET = -3.3 * (SimulationConfig.ROAD_WIDTH / 3); 
    public static final double EAST_RIGHT_OFFSET = -4.3* (SimulationConfig.ROAD_WIDTH / 3); 
    public static final double EAST_U_TURN_OFFSET = EAST_LEFT_OFFSET;
    public static final double EAST_U_TURN_2ND_OFFSET = EAST_LEFT_OFFSET + 5; // Slightly offset for visual distinction
    
    // Vertical offsets for West lanes
    public static final double WEST_LEFT_OFFSET = 2 * (SimulationConfig.ROAD_WIDTH / 3); 
    public static final double WEST_STRAIGHT_OFFSET = 3 * (SimulationConfig.ROAD_WIDTH / 3); 
    public static final double WEST_RIGHT_OFFSET = 3.8 * (SimulationConfig.ROAD_WIDTH / 3); 
    public static final double WEST_U_TURN_OFFSET = WEST_LEFT_OFFSET;
    public static final double WEST_U_TURN_2ND_OFFSET = WEST_LEFT_OFFSET - 5; // Slightly offset for visual distinction
}