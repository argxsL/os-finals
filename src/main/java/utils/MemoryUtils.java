package utils;

import memory.Process;
import java.awt.Color;
import java.util.Random;

public class MemoryUtils {
    private static final Random random = new Random();
    
    public static String formatMemorySize(int sizeInKB) {
        if (sizeInKB >= 1024) {
            return String.format("%.1f MB", sizeInKB / 1024.0);
        }
        return sizeInKB + " KB";
    }
    
    public static String formatPercentage(double percentage) {
        return String.format("%.1f%%", percentage);
    }
    
    public static Color getProcessColor(int processId) {
        return Constants.PROCESS_COLORS[processId % Constants.PROCESS_COLORS.length];
    }
    
    public static Color getSegmentColor(String segmentType) {
        switch (segmentType.toUpperCase()) {
            case "CODE":
                return Constants.CODE_SEGMENT_COLOR;
            case "DATA":
                return Constants.DATA_SEGMENT_COLOR;
            case "STACK":
                return Constants.STACK_SEGMENT_COLOR;
            default:
                return Constants.FREE_MEMORY_COLOR;
        }
    }
    
    public static Process generateRandomProcess() {
        String name = Constants.PROCESS_NAMES[random.nextInt(Constants.PROCESS_NAMES.length)];
        int size = Constants.MIN_PROCESS_SIZE + 
                   random.nextInt(Constants.MAX_PROCESS_SIZE - Constants.MIN_PROCESS_SIZE);
        int priority = 1 + random.nextInt(10);
        
        return new Process(0, name, size, priority); // ID will be set by MemoryManager
    }
    
    public static String getMemoryAddressString(int address) {
        return String.format("0x%04X", address);
    }
    
    public static int calculatePages(int memorySize, int pageSize) {
        return (int) Math.ceil((double) memorySize / pageSize);
    }
    
    public static double calculateFragmentation(int totalFree, int largestFree) {
        if (totalFree == 0) return 0.0;
        return ((double)(totalFree - largestFree) / totalFree) * 100;
    }
    
    public static String getProcessStatusString(Process process) {
        return String.format("%s [ID: %d, Size: %s, Priority: %d]",
                           process.getName(),
                           process.getProcessId(),
                           formatMemorySize(process.getSize()),
                           process.getPriority());
    }
    
    public static Color lightenColor(Color color, float factor) {
        int red = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int green = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int blue = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(red, green, blue);
    }
    
    public static Color darkenColor(Color color, float factor) {
        int red = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int green = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int blue = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(red, green, blue);
    }
    
    public static String timeToString(long timeMs) {
        if (timeMs < 1000) {
            return timeMs + " ms";
        } else if (timeMs < 60000) {
            return String.format("%.1f s", timeMs / 1000.0);
        } else {
            long minutes = timeMs / 60000;
            long seconds = (timeMs % 60000) / 1000;
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}