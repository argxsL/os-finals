package gui;

import memory.MemoryManager;
import memory.SegmentationManager;
import memory.Process;
import utils.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class SegmentationPanel extends JPanel {
    private MemoryManager memoryManager;
    private JTable segmentTableView;
    private DefaultTableModel segmentTableModel;
    private JLabel statsLabel;
    private Timer updateTimer;
    
    public SegmentationPanel(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        initializeComponents();
        setupLayout();
        
        // Update timer
        updateTimer = new Timer(Constants.STATS_UPDATE_INTERVAL, e -> updateSegmentTable());
        updateTimer.start();
    }
    
    private void initializeComponents() {
        setBorder(BorderFactory.createTitledBorder("Segmentation Management"));
        
        // Segment table
        String[] columns = {"Seg #", "Start Address", "End Address", "Size", "Type", "Process ID", "Status"};
        segmentTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        segmentTableView = new JTable(segmentTableModel);
        segmentTableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        segmentTableView.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        segmentTableView.getColumnModel().getColumn(0).setPreferredWidth(50);  // Seg #
        segmentTableView.getColumnModel().getColumn(1).setPreferredWidth(80);  // Start
        segmentTableView.getColumnModel().getColumn(2).setPreferredWidth(80);  // End
        segmentTableView.getColumnModel().getColumn(3).setPreferredWidth(60);  // Size
        segmentTableView.getColumnModel().getColumn(4).setPreferredWidth(60);  // Type
        segmentTableView.getColumnModel().getColumn(5).setPreferredWidth(70);  // Process
        segmentTableView.getColumnModel().getColumn(6).setPreferredWidth(70);  // Status
        
        // Stats label
        statsLabel = new JLabel();
        statsLabel.setFont(new Font("Monospaced", Font.PLAIN, Constants.LABEL_FONT_SIZE));
        
        updateSegmentTable();
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(Constants.COMPONENT_SPACING, Constants.COMPONENT_SPACING));
        
        // Top panel with controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton compactBtn = new JButton("Compact Memory");
        compactBtn.addActionListener(this::compactMemory);
        controlPanel.add(compactBtn);
        
        JButton clearSegmentsBtn = new JButton("Clear All Segments");
        clearSegmentsBtn.addActionListener(this::clearAllSegments);
        controlPanel.add(clearSegmentsBtn);
        
        JButton showFragmentationBtn = new JButton("Show Fragmentation");
        showFragmentationBtn.addActionListener(this::showFragmentationInfo);
        controlPanel.add(showFragmentationBtn);
        
        add(controlPanel, BorderLayout.NORTH);
        
        // Center panel with segment table
        JScrollPane scrollPane = new JScrollPane(segmentTableView);
        scrollPane.setPreferredSize(new Dimension(280, 200));
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel with statistics
        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(BorderFactory.createTitledBorder("Segmentation Statistics"));
        statsPanel.add(statsLabel, BorderLayout.CENTER);
        add(statsPanel, BorderLayout.SOUTH);
    }
    
    private void updateSegmentTable() {
        if (memoryManager.getCurrentType() != MemoryManager.MemoryType.SEGMENTATION) {
            return;
        }
        
        SegmentationManager segManager = memoryManager.getSegmentationManager();
        List<SegmentationManager.MemorySegment> segments = segManager.getMemorySegments();
        
        // Clear existing data
        segmentTableModel.setRowCount(0);
        
        // Add segment data
        for (int i = 0; i < segments.size(); i++) {
            SegmentationManager.MemorySegment segment = segments.get(i);
            
            Object[] rowData = {
                i,
                MemoryUtils.getMemoryAddressString(segment.getStartAddress()),
                MemoryUtils.getMemoryAddressString(segment.getEndAddress()),
                MemoryUtils.formatMemorySize(segment.getSize()),
                segment.getSegmentType(),
                segment.isAllocated() ? String.valueOf(segment.getProcessId()) : "-",
                segment.isAllocated() ? "Allocated" : "Free"
            };
            
            segmentTableModel.addRow(rowData);
        }
        
        // Update statistics
        updateStatistics();
    }
    
    private void updateStatistics() {
        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        if (stats == null) return;
        
        SegmentationManager segManager = memoryManager.getSegmentationManager();
        
        StringBuilder statsText = new StringBuilder();
        statsText.append("<html>");
        statsText.append("Total Memory: ").append(MemoryUtils.formatMemorySize(segManager.getTotalMemory())).append("<br>");
        statsText.append("Free Memory: ").append(MemoryUtils.formatMemorySize(segManager.getFreeMemory())).append("<br>");
        statsText.append("Used Memory: ").append(MemoryUtils.formatMemorySize(stats.getUsedMemory())).append("<br>");
        statsText.append("Total Segments: ").append(segManager.getMemorySegments().size()).append("<br>");
        statsText.append("Memory Utilization: ").append(MemoryUtils.formatPercentage(stats.getUtilization())).append("<br>");
        statsText.append("Fragmentation: ").append(MemoryUtils.formatPercentage(stats.getFragmentation())).append("<br>");
        
        // Count free segments
        long freeSegments = segManager.getMemorySegments().stream()
                .filter(s -> !s.isAllocated())
                .count();
        statsText.append("Free Segments: ").append(freeSegments).append("<br>");
        
        // Largest free block
        int largestFree = segManager.getMemorySegments().stream()
                .filter(s -> !s.isAllocated())
                .mapToInt(SegmentationManager.MemorySegment::getSize)
                .max().orElse(0);
        statsText.append("Largest Free Block: ").append(MemoryUtils.formatMemorySize(largestFree)).append("<br>");
        
        statsText.append("</html>");
        
        statsLabel.setText(statsText.toString());
    }
    
    private void compactMemory(ActionEvent e) {
        // Show progress dialog
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                           "Compacting Memory", true);
        progressDialog.add(new JLabel("Compacting memory segments..."), BorderLayout.CENTER);
        progressDialog.add(progressBar, BorderLayout.SOUTH);
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(this);
        
        // Use SwingWorker for background operation
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Simulate compaction time
                Thread.sleep(1000);
                
                // Force compaction by reallocating all processes
                List<Process> activeProcesses = memoryManager.getActiveProcesses();
                for (Process process : activeProcesses) {
                    memoryManager.deallocateMemory(process);
                }
                for (Process process : activeProcesses) {
                    process.setActive(true);
                    memoryManager.allocateMemory(process);
                }
                
                return null;
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                updateSegmentTable();
                JOptionPane.showMessageDialog(SegmentationPanel.this, 
                    "Memory compaction completed!", 
                    "Compaction", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void clearAllSegments(ActionEvent e) {
        int result = JOptionPane.showConfirmDialog(this,
            "This will deallocate all processes. Continue?",
            "Clear All Segments",
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            List<Process> processes = memoryManager.getAllProcesses();
            for (Process process : processes) {
                memoryManager.deallocateMemory(process);
            }
            updateSegmentTable();
        }
    }
    
    private void showFragmentationInfo(ActionEvent e) {
        SegmentationManager segManager = memoryManager.getSegmentationManager();
        List<SegmentationManager.MemorySegment> freeSegments = segManager.getMemorySegments()
                .stream()
                .filter(s -> !s.isAllocated())
                .collect(java.util.stream.Collectors.toList());
        
        StringBuilder info = new StringBuilder();
        info.append("Fragmentation Analysis:\n\n");
        info.append("Total Free Segments: ").append(freeSegments.size()).append("\n");
        
        if (!freeSegments.isEmpty()) {
            int totalFree = freeSegments.stream().mapToInt(SegmentationManager.MemorySegment::getSize).sum();
            int largestFree = freeSegments.stream().mapToInt(SegmentationManager.MemorySegment::getSize).max().orElse(0);
            int smallestFree = freeSegments.stream().mapToInt(SegmentationManager.MemorySegment::getSize).min().orElse(0);
            
            info.append("Total Free Memory: ").append(MemoryUtils.formatMemorySize(totalFree)).append("\n");
            info.append("Largest Free Block: ").append(MemoryUtils.formatMemorySize(largestFree)).append("\n");
            info.append("Smallest Free Block: ").append(MemoryUtils.formatMemorySize(smallestFree)).append("\n");
            info.append("Fragmentation: ").append(MemoryUtils.formatPercentage(segManager.getFragmentation())).append("\n\n");
            
            info.append("Free Segments:\n");
            for (int i = 0; i < Math.min(freeSegments.size(), 10); i++) {
                SegmentationManager.MemorySegment segment = freeSegments.get(i);
                info.append(String.format("  %s - %s (%s)\n",
                    MemoryUtils.getMemoryAddressString(segment.getStartAddress()),
                    MemoryUtils.getMemoryAddressString(segment.getEndAddress()),
                    MemoryUtils.formatMemorySize(segment.getSize())));
            }
            
            if (freeSegments.size() > 10) {
                info.append("  ... and ").append(freeSegments.size() - 10).append(" more");
            }
        } else {
            info.append("No free segments available.");
        }
        
        JTextArea textArea = new JTextArea(info.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Fragmentation Information", 
                                     JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void refresh() {
        updateSegmentTable();
    }
    
    public void stopTimer() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }
}