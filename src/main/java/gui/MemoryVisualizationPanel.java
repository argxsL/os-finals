package gui;

import memory.MemoryManager;
import memory.PagingManager;
import memory.SegmentationManager;
import memory.Process;
import utils.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class MemoryVisualizationPanel extends JPanel {
    private MemoryManager memoryManager;
    private Timer refreshTimer;
    
    public MemoryVisualizationPanel(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        setPreferredSize(new Dimension(Constants.MEMORY_PANEL_WIDTH, Constants.MEMORY_PANEL_HEIGHT));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("Memory Visualization"));
        
        // Refresh timer for animations
        refreshTimer = new Timer(Constants.ANIMATION_DELAY, e -> repaint());
        refreshTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        switch (memoryManager.getCurrentType()) {
            case PAGING:
                drawPagingVisualization(g2d);
                break;
            case SEGMENTATION:
                drawSegmentationVisualization(g2d);
                break;
        }
        
        drawLegend(g2d);
    }
    
    private void drawPagingVisualization(Graphics2D g2d) {
        PagingManager pagingManager = memoryManager.getPagingManager();
        boolean[] pageTable = pagingManager.getPageTable();
        java.util.Map<Integer, Integer> pageOwners = pagingManager.getPageOwners();
        
        int totalPages = pageTable.length;
        int cols = (int) Math.ceil(Math.sqrt(totalPages));
        int rows = (int) Math.ceil((double) totalPages / cols);
        
        int pageWidth = (getWidth() - 220) / cols; // Leave space for legend
        int pageHeight = Math.min(20, (getHeight() - 150) / rows);
        
        int startX = 50;
        int startY = 50;
        
        g2d.setFont(new Font("Arial", Font.BOLD, Constants.TITLE_FONT_SIZE));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Paging Memory Layout", startX, 30);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, Constants.SMALL_FONT_SIZE));
        
        for (int i = 0; i < totalPages; i++) {
            int row = i / cols;
            int col = i % cols;
            int x = startX + col * pageWidth;
            int y = startY + row * pageHeight;
            
            // Draw page
            if (pageTable[i]) {
                // Allocated page
                Integer ownerId = pageOwners.get(i);
                Color pageColor = ownerId != null ? MemoryUtils.getProcessColor(ownerId) : 
                                 Constants.ALLOCATED_MEMORY_COLOR;
                g2d.setColor(pageColor);
            } else {
                // Free page
                g2d.setColor(Constants.FREE_MEMORY_COLOR);
            }
            
            g2d.fillRect(x, y, pageWidth - 1, pageHeight - 1);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, pageWidth - 1, pageHeight - 1);
            
            // Draw page number
            if (pageWidth > 20) {
                String pageNum = String.valueOf(i);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(pageNum);
                int textHeight = fm.getHeight();
                g2d.drawString(pageNum, 
                    x + (pageWidth - textWidth) / 2, 
                    y + (pageHeight + textHeight) / 2 - 2);
            }
        }
        
        // Draw page info
        g2d.setFont(new Font("Arial", Font.PLAIN, Constants.LABEL_FONT_SIZE));
        int infoY = startY + rows * pageHeight + 30;
        g2d.drawString("Page Size: " + MemoryUtils.formatMemorySize(pagingManager.getPageSize()), 
                      startX, infoY);
        g2d.drawString("Free Pages: " + pagingManager.getFreePages() + "/" + totalPages, 
                      startX + 200, infoY);
        g2d.drawString("Algorithm: " + pagingManager.getCurrentAlgorithm(), 
                      startX + 400, infoY);
    }
    
    private void drawSegmentationVisualization(Graphics2D g2d) {
        SegmentationManager segManager = memoryManager.getSegmentationManager();
        List<SegmentationManager.MemorySegment> segments = segManager.getMemorySegments();
        
        int totalMemory = segManager.getTotalMemory();
        int memoryWidth = getWidth() - 220; // Leave more space for legend
        int memoryHeight = 300;
        int startX = 50;
        int startY = 80;
        
        g2d.setFont(new Font("Arial", Font.BOLD, Constants.TITLE_FONT_SIZE));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Segmentation Memory Layout", startX, 30);
        
        // Draw memory bar
        g2d.setColor(Color.BLACK);
        g2d.drawRect(startX - 1, startY - 1, memoryWidth + 2, Constants.MEMORY_BLOCK_HEIGHT + 2);
        
        int currentX = startX;
        for (SegmentationManager.MemorySegment segment : segments) {
            int segmentWidth = (int) ((double) segment.getSize() / totalMemory * memoryWidth);
            
            Color segmentColor;
            if (segment.isAllocated()) {
                segmentColor = MemoryUtils.getSegmentColor(segment.getSegmentType());
                // Blend with process color
                Color processColor = MemoryUtils.getProcessColor(segment.getProcessId());
                segmentColor = blendColors(segmentColor, processColor, 0.5f);
            } else {
                segmentColor = Constants.FREE_MEMORY_COLOR;
            }
            
            g2d.setColor(segmentColor);
            g2d.fillRect(currentX, startY, segmentWidth, Constants.MEMORY_BLOCK_HEIGHT);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(currentX, startY, segmentWidth, Constants.MEMORY_BLOCK_HEIGHT);
            
            // Draw segment info if wide enough
            if (segmentWidth > 60) {
                g2d.setFont(new Font("Arial", Font.PLAIN, Constants.SMALL_FONT_SIZE));
                String segmentInfo = segment.isAllocated() ? 
                    "P" + segment.getProcessId() + ":" + segment.getSegmentType() : "FREE";
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(segmentInfo);
                g2d.drawString(segmentInfo, 
                    currentX + (segmentWidth - textWidth) / 2, 
                    startY + 15);
            }
            
            currentX += segmentWidth;
        }
        
        // Draw detailed segment information
        g2d.setFont(new Font("Arial", Font.PLAIN, Constants.LABEL_FONT_SIZE));
        int detailY = startY + Constants.MEMORY_BLOCK_HEIGHT + 40;
        int detailX = startX;
        
        g2d.setColor(Color.BLACK);
        g2d.drawString("Segment Details:", detailX, detailY);
        detailY += 20;
        
        for (int i = 0; i < segments.size() && i < 10; i++) {
            SegmentationManager.MemorySegment segment = segments.get(i);
            String segmentDetail = String.format("Seg %d: %s - %s (%s) %s", 
                i,
                MemoryUtils.getMemoryAddressString(segment.getStartAddress()),
                MemoryUtils.getMemoryAddressString(segment.getEndAddress()),
                MemoryUtils.formatMemorySize(segment.getSize()),
                segment.isAllocated() ? 
                    "P" + segment.getProcessId() + ":" + segment.getSegmentType() : "FREE"
            );
            
            g2d.setColor(segment.isAllocated() ? 
                MemoryUtils.getProcessColor(segment.getProcessId()) : Color.GRAY);
            g2d.drawString(segmentDetail, detailX, detailY);
            detailY += 18;
        }
        
        if (segments.size() > 10) {
            g2d.setColor(Color.GRAY);
            g2d.drawString("... and " + (segments.size() - 10) + " more segments", 
                          detailX, detailY);
        }
    }
    
    private void drawLegend(Graphics2D g2d) {
        int legendX = getWidth() - 180;
        int legendY;
        
        // Position legend based on memory management type
        if (memoryManager.getCurrentType() == MemoryManager.MemoryType.SEGMENTATION) {
            legendY = getHeight() - 200; // Bottom area for segmentation
            
            // Draw background for better visibility in segmentation mode
            g2d.setColor(new Color(250, 250, 250, 200));
            g2d.fillRect(legendX - 5, legendY - 20, 175, 180);
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawRect(legendX - 5, legendY - 20, 175, 180);
        } else {
            legendY = 50; // Top area for paging
        }
        
        g2d.setFont(new Font("Arial", Font.BOLD, Constants.LABEL_FONT_SIZE));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Legend:", legendX, legendY);
        
        legendY += 25;
        g2d.setFont(new Font("Arial", Font.PLAIN, Constants.SMALL_FONT_SIZE));
        
        // Free memory
        g2d.setColor(Constants.FREE_MEMORY_COLOR);
        g2d.fillRect(legendX, legendY, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(legendX, legendY, 15, 15);
        g2d.drawString("Free Memory", legendX + 20, legendY + 12);
        legendY += 20;
        
        if (memoryManager.getCurrentType() == MemoryManager.MemoryType.SEGMENTATION) {
            // Segment types
            g2d.setColor(Constants.CODE_SEGMENT_COLOR);
            g2d.fillRect(legendX, legendY, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(legendX, legendY, 15, 15);
            g2d.drawString("Code Segment", legendX + 20, legendY + 12);
            legendY += 20;
            
            g2d.setColor(Constants.DATA_SEGMENT_COLOR);
            g2d.fillRect(legendX, legendY, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(legendX, legendY, 15, 15);
            g2d.drawString("Data Segment", legendX + 20, legendY + 12);
            legendY += 20;
            
            g2d.setColor(Constants.STACK_SEGMENT_COLOR);
            g2d.fillRect(legendX, legendY, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(legendX, legendY, 15, 15);
            g2d.drawString("Stack Segment", legendX + 20, legendY + 12);
            legendY += 20;
        }
        
        // Process colors
        List<Process> activeProcesses = memoryManager.getActiveProcesses();
        for (int i = 0; i < Math.min(activeProcesses.size(), 5); i++) {
            Process process = activeProcesses.get(i);
            g2d.setColor(MemoryUtils.getProcessColor(process.getProcessId()));
            g2d.fillRect(legendX, legendY, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(legendX, legendY, 15, 15);
            g2d.drawString("P" + process.getProcessId() + ":" + process.getName(), 
                          legendX + 20, legendY + 12);
            legendY += 20;
        }
    }
    
    private Color blendColors(Color color1, Color color2, float ratio) {
        float inverseRatio = 1f - ratio;
        return new Color(
            (int) (color1.getRed() * ratio + color2.getRed() * inverseRatio),
            (int) (color1.getGreen() * ratio + color2.getGreen() * inverseRatio),
            (int) (color1.getBlue() * ratio + color2.getBlue() * inverseRatio)
        );
    }
    
    public void stopAnimation() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}