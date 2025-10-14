package utils;

import java.awt.Color;

public class Constants {
    // Memory Configuration
    public static final int DEFAULT_TOTAL_MEMORY = 1024; // KB
    public static final int DEFAULT_PAGE_SIZE = 64; // KB
    public static final int MIN_PROCESS_SIZE = 16; // KB
    public static final int MAX_PROCESS_SIZE = 256; // KB
    
    // GUI Configuration
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    public static final int MEMORY_PANEL_WIDTH = 800;
    public static final int MEMORY_PANEL_HEIGHT = 400;
    public static final int CONTROL_PANEL_WIDTH = 300;
    
    // Colors for Memory Visualization
    public static final Color FREE_MEMORY_COLOR = Color.WHITE;
    public static final Color ALLOCATED_MEMORY_COLOR = Color.LIGHT_GRAY;
    public static final Color PROCESS_COLORS[] = {
        new Color(255, 182, 193), // Light Pink
        new Color(173, 216, 230), // Light Blue
        new Color(144, 238, 144), // Light Green
        new Color(255, 218, 185), // Peach
        new Color(221, 160, 221), // Plum
        new Color(255, 255, 224), // Light Yellow
        new Color(175, 238, 238), // Pale Turquoise
        new Color(255, 192, 203), // Pink
        new Color(230, 230, 250), // Lavender
        new Color(250, 240, 230)  // Linen
    };
    
    // Segment Type Colors
    public static final Color CODE_SEGMENT_COLOR = new Color(255, 200, 200);
    public static final Color DATA_SEGMENT_COLOR = new Color(200, 255, 200);
    public static final Color STACK_SEGMENT_COLOR = new Color(200, 200, 255);
    
    // Animation and Update Settings
    public static final int ANIMATION_DELAY = 100; // milliseconds
    public static final int STATS_UPDATE_INTERVAL = 500; // milliseconds
    
    // Process Generation
    public static final String[] PROCESS_NAMES = {
        "Browser", "TextEditor", "MediaPlayer", "Calculator", "FileManager",
        "Compiler", "Database", "WebServer", "ImageEditor", "GameEngine",
        "Antivirus", "Messenger", "DownloadManager", "VideoEncoder", "BackupTool"
    };
    
    // Memory Block Sizes (for visualization)
    public static final int MEMORY_BLOCK_HEIGHT = 20;
    public static final int MEMORY_BLOCK_MARGIN = 2;
    
    // Font Sizes
    public static final int TITLE_FONT_SIZE = 16;
    public static final int LABEL_FONT_SIZE = 12;
    public static final int SMALL_FONT_SIZE = 10;
    
    // Border and Padding
    public static final int PANEL_PADDING = 10;
    public static final int COMPONENT_SPACING = 5;
}