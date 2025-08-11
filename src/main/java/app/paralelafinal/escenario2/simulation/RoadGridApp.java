package app.paralelafinal.escenario2.simulation;

import app.paralelafinal.config.SimulationConfig;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * JavaFX application that draws a road grid with two horizontal and two vertical roads.
 * Horizontal roads have 3 lanes per direction (6 lanes total).
 * Vertical roads have 1 lane per direction (2 lanes total).
 * The roads have dashed lane dividers that are interrupted at intersections.
 */
public class RoadGridApp extends Application {
    
    // Canvas dimensions - using SimulationConfig
    private static final double CANVAS_WIDTH = SimulationConfig.SCENE_WIDTH;
    private static final double CANVAS_HEIGHT = SimulationConfig.SCENE_HEIGHT;
    
    // Road dimensions
    private static final double HORIZONTAL_ROAD_WIDTH = SimulationConfig.ROAD_WIDTH * 1.875; // 3 lanes per direction (6 lanes total)
    private static final double VERTICAL_ROAD_WIDTH = SimulationConfig.ROAD_WIDTH; // 2 lanes total
    private static final double LANE_WIDTH = SimulationConfig.ROAD_WIDTH / 2; // Width of a single lane
    
    // Calculate block sizes to fit the canvas
    private static final double HORIZONTAL_BLOCK_SIZE = (CANVAS_WIDTH - 2 * VERTICAL_ROAD_WIDTH) / 3;
    private static final double VERTICAL_BLOCK_SIZE = (CANVAS_HEIGHT - 2 * HORIZONTAL_ROAD_WIDTH) / 3;
    
    // Line properties for dashed lane dividers - using SimulationConfig
    private static final double LINE_WIDTH = SimulationConfig.LINE_THICKNESS;
    private static final double DASH_LENGTH = SimulationConfig.DASHED_LINE_LENGTH;
    private static final double GAP_LENGTH = SimulationConfig.DASHED_LINE_GAP;
    
    @Override
    public void start(Stage primaryStage) {
        // Create the canvas
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Draw the road grid
        drawRoadGrid(gc);
        
        // Create the scene
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, CANVAS_WIDTH, CANVAS_HEIGHT);
        scene.setFill(Color.WHITE); // Set white background
        
        // Configure and show the stage
        primaryStage.setTitle("Road Grid - Scenario 2");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    
    /**
     * Draws the complete road grid including roads and lane dividers
     */
    private void drawRoadGrid(GraphicsContext gc) {
        // Clear canvas with white background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        
        // Draw the roads (black rectangles)
        drawRoads(gc);
        
        // Draw the lane dividers (white dashed lines)
        drawLaneDividers(gc);
    }
    
    /**
     * Draws the four roads (2 horizontal and 2 vertical) as black rectangles
     */
    private void drawRoads(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        
        // Vertical roads (2 lanes each)
        // First vertical road: starts after first horizontal block
        double x1 = HORIZONTAL_BLOCK_SIZE;
        gc.fillRect(x1, 0, VERTICAL_ROAD_WIDTH, CANVAS_HEIGHT);
        
        // Second vertical road: starts after first block + road + second block
        double x2 = HORIZONTAL_BLOCK_SIZE + VERTICAL_ROAD_WIDTH + HORIZONTAL_BLOCK_SIZE;
        gc.fillRect(x2, 0, VERTICAL_ROAD_WIDTH, CANVAS_HEIGHT);
        
        // Horizontal roads (6 lanes each - 3 per direction)
        // First horizontal road: starts after first vertical block
        double y1 = VERTICAL_BLOCK_SIZE;
        gc.fillRect(0, y1, CANVAS_WIDTH, HORIZONTAL_ROAD_WIDTH);
        
        // Second horizontal road: starts after first block + road + second block
        double y2 = VERTICAL_BLOCK_SIZE + HORIZONTAL_ROAD_WIDTH + VERTICAL_BLOCK_SIZE;
        gc.fillRect(0, y2, CANVAS_WIDTH, HORIZONTAL_ROAD_WIDTH);
    }
    
    /**
     * Draws the white dashed lane dividers on the roads, avoiding intersections
     */
    private void drawLaneDividers(GraphicsContext gc) {
        // Configure line properties
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(LINE_WIDTH);
        gc.setLineDashes(DASH_LENGTH, GAP_LENGTH);
        
        // Calculate vertical road positions
        double vertRoad1Start = HORIZONTAL_BLOCK_SIZE;
        double vertRoad1End = HORIZONTAL_BLOCK_SIZE + VERTICAL_ROAD_WIDTH;
        double vertRoad2Start = HORIZONTAL_BLOCK_SIZE + VERTICAL_ROAD_WIDTH + HORIZONTAL_BLOCK_SIZE;
        double vertRoad2End = HORIZONTAL_BLOCK_SIZE + VERTICAL_ROAD_WIDTH + HORIZONTAL_BLOCK_SIZE + VERTICAL_ROAD_WIDTH;
        
        // Calculate horizontal road positions
        double horizRoad1Start = VERTICAL_BLOCK_SIZE;
        double horizRoad1End = VERTICAL_BLOCK_SIZE + HORIZONTAL_ROAD_WIDTH;
        double horizRoad2Start = VERTICAL_BLOCK_SIZE + HORIZONTAL_ROAD_WIDTH + VERTICAL_BLOCK_SIZE;
        double horizRoad2End = VERTICAL_BLOCK_SIZE + HORIZONTAL_ROAD_WIDTH + VERTICAL_BLOCK_SIZE + HORIZONTAL_ROAD_WIDTH;
        
        // Draw horizontal lane dividers (5 lines per road for 6 lanes)
        drawHorizontalLaneDividers(gc, horizRoad1Start, horizRoad1End, horizRoad2Start, horizRoad2End,
                                  vertRoad1Start, vertRoad1End, vertRoad2Start, vertRoad2End);
        
        // Draw vertical lane dividers (1 line per road for 2 lanes)
        drawVerticalLaneDividers(gc, vertRoad1Start, vertRoad1End, vertRoad2Start, vertRoad2End,
                                horizRoad1Start, horizRoad1End, horizRoad2Start, horizRoad2End);
    }
    
    /**
     * Draws horizontal lane dividers with gaps at intersections
     * Horizontal roads have 6 lanes (3 per direction), so we need 5 divider lines
     */
    private void drawHorizontalLaneDividers(GraphicsContext gc, double horizRoad1Start, double horizRoad1End, 
                                           double horizRoad2Start, double horizRoad2End,
                                           double vertRoad1Start, double vertRoad1End,
                                           double vertRoad2Start, double vertRoad2End) {
        
        double laneHeight = HORIZONTAL_ROAD_WIDTH / 3; // 6 lanes total
        
        // Draw lines for first horizontal road
        for (int i = 1; i <= 2; i++) {
            double y = horizRoad1Start + (i * laneHeight);
            
            // Segment 1: From start to first vertical road
            gc.strokeLine(0, y, vertRoad1Start, y);
            
            // Segment 2: Between the two vertical roads
            gc.strokeLine(vertRoad1End, y, vertRoad2Start, y);
            
            // Segment 3: From second vertical road to end
            gc.strokeLine(vertRoad2End, y, CANVAS_WIDTH, y);
        }
        
        // Draw lines for second horizontal road
        for (int i = 1; i <= 2; i++) {
            double y = horizRoad2Start + (i * laneHeight);
            
            // Segment 1: From start to first vertical road
            gc.strokeLine(0, y, vertRoad1Start, y);
            
            // Segment 2: Between the two vertical roads
            gc.strokeLine(vertRoad1End, y, vertRoad2Start, y);
            
            // Segment 3: From second vertical road to end
            gc.strokeLine(vertRoad2End, y, CANVAS_WIDTH, y);
        }
    }
    
    /**
     * Draws vertical lane dividers with gaps at intersections
     * Vertical roads have 2 lanes (1 per direction), so we need 1 divider line
     */
    private void drawVerticalLaneDividers(GraphicsContext gc, double vertRoad1Start, double vertRoad1End, 
                                         double vertRoad2Start, double vertRoad2End,
                                         double horizRoad1Start, double horizRoad1End,
                                         double horizRoad2Start, double horizRoad2End) {
        // First vertical road center line
        double x1 = vertRoad1Start + VERTICAL_ROAD_WIDTH / 2;
        
        // Segment 1: From start to first horizontal road
        gc.strokeLine(x1, 0, x1, horizRoad1Start);
        
        // Segment 2: Between the two horizontal roads
        gc.strokeLine(x1, horizRoad1End, x1, horizRoad2Start);
        
        // Segment 3: From second horizontal road to end
        gc.strokeLine(x1, horizRoad2End, x1, CANVAS_HEIGHT);
        
        // Second vertical road center line
        double x2 = vertRoad2Start + VERTICAL_ROAD_WIDTH / 2;
        
        // Segment 1: From start to first horizontal road
        gc.strokeLine(x2, 0, x2, horizRoad1Start);
        
        // Segment 2: Between the two horizontal roads
        gc.strokeLine(x2, horizRoad1End, x2, horizRoad2Start);
        
        // Segment 3: From second horizontal road to end
        gc.strokeLine(x2, horizRoad2End, x2, CANVAS_HEIGHT);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}