package gui;

import memory.MemoryManager;
import memory.PagingManager;
import memory.Process;
import utils.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class PagingPanel extends JPanel {
    private MemoryManager memoryManager;
    private JComboBox<PagingManager.ReplacementAlgorithm> algorithmCombo;
    private JTable pageTableView;
    private DefaultTableModel pageTableModel;
    private JLabel statsLabel;
    private Timer updateTimer;
    
    public PagingPanel(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        // Update timer
        updateTimer = new Timer(Constants.STATS_UPDATE_INTERVAL, e -> updatePageTable());
        updateTimer.start();
    }
    
    private void initializeComponents() {
        setBorder(BorderFactory.createTitledBorder("Paging Management"));
        
        // Algorithm selection
        algorithmCombo = new JComboBox<>(PagingManager.ReplacementAlgorithm.values());
        algorithmCombo.setSelectedItem(memoryManager.getPagingManager().getCurrentAlgorithm());
        
        // Page table
        String[] columns = {"Page #", "Status", "Process ID", "Access Count"};
        pageTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pageTableView = new JTable(pageTableModel);
        pageTableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pageTableView.getTableHeader().setReorderingAllowed(false);
        
        // Stats label
        statsLabel = new JLabel();
        statsLabel.setFont(new Font("Monospaced", Font.PLAIN, Constants.LABEL_FONT_SIZE));
        
        updatePageTable();
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(Constants.COMPONENT_SPACING, Constants.COMPONENT_SPACING));
        
        // Top panel with controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Replacement Algorithm:"));
        controlPanel.add(algorithmCombo);
        
        JButton accessPageBtn = new JButton("Access Page");
        accessPageBtn.addActionListener(this::accessRandomPage);
        controlPanel.add(accessPageBtn);
        
        JButton clearPagesBtn = new JButton("Clear All Pages");
        clearPagesBtn.addActionListener(this::clearAllPages);
        controlPanel.add(clearPagesBtn);
        
        add(controlPanel, BorderLayout.NORTH);
        
        // Center panel with page table
        JScrollPane scrollPane = new JScrollPane(pageTableView);
        scrollPane.setPreferredSize(new Dimension(280, 200));
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel with statistics
        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(BorderFactory.createTitledBorder("Page Statistics"));
        statsPanel.add(statsLabel, BorderLayout.CENTER);
        add(statsPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        algorithmCombo.addActionListener(e -> {
            PagingManager.ReplacementAlgorithm selected = 
                (PagingManager.ReplacementAlgorithm) algorithmCombo.getSelectedItem();
            memoryManager.getPagingManager().setReplacementAlgorithm(selected);
        });
    }
    
    private void updatePageTable() {
        if (memoryManager.getCurrentType() != MemoryManager.MemoryType.PAGING) {
            return;
        }
        
        PagingManager pagingManager = memoryManager.getPagingManager();
        boolean[] pageTable = pagingManager.getPageTable();
        java.util.Map<Integer, Integer> pageOwners = pagingManager.getPageOwners();
        
        // Clear existing data
        pageTableModel.setRowCount(0);
        
        // Add page data
        for (int i = 0; i < pageTable.length; i++) {
            String status = pageTable[i] ? "Allocated" : "Free";
            String processId = pageTable[i] && pageOwners.containsKey(i) ? 
                String.valueOf(pageOwners.get(i)) : "-";
            String accessCount = pageTable[i] ? "N/A" : "-"; // Simplified for demo
            
            pageTableModel.addRow(new Object[]{i, status, processId, accessCount});
        }
        
        // Update statistics
        updateStatistics();
    }
    
    private void updateStatistics() {
        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        if (stats == null) return;
        
        PagingManager pagingManager = memoryManager.getPagingManager();
        
        StringBuilder statsText = new StringBuilder();
        statsText.append("<html>");
        statsText.append("Total Pages: ").append(pagingManager.getTotalPages()).append("<br>");
        statsText.append("Free Pages: ").append(pagingManager.getFreePages()).append("<br>");
        statsText.append("Used Pages: ").append(pagingManager.getTotalPages() - pagingManager.getFreePages()).append("<br>");
        statsText.append("Page Size: ").append(MemoryUtils.formatMemorySize(pagingManager.getPageSize())).append("<br>");
        statsText.append("Memory Utilization: ").append(MemoryUtils.formatPercentage(stats.getUtilization())).append("<br>");
        statsText.append("Fragmentation: ").append(MemoryUtils.formatPercentage(stats.getFragmentation())).append("<br>");
        statsText.append("Algorithm: ").append(pagingManager.getCurrentAlgorithm()).append("<br>");
        statsText.append("</html>");
        
        statsLabel.setText(statsText.toString());
    }
    
    private void accessRandomPage(ActionEvent e) {
        PagingManager pagingManager = memoryManager.getPagingManager();
        boolean[] pageTable = pagingManager.getPageTable();
        
        // Find allocated pages
        java.util.List<Integer> allocatedPages = new java.util.ArrayList<>();
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i]) {
                allocatedPages.add(i);
            }
        }
        
        if (!allocatedPages.isEmpty()) {
            int randomPage = allocatedPages.get((int)(Math.random() * allocatedPages.size()));
            pagingManager.accessPage(randomPage);
            
            JOptionPane.showMessageDialog(this, 
                "Accessed page " + randomPage, 
                "Page Access", 
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                "No allocated pages to access!", 
                "Error", 
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void clearAllPages(ActionEvent e) {
        int result = JOptionPane.showConfirmDialog(this,
            "This will deallocate all processes. Continue?",
            "Clear All Pages",
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            List<Process> processes = memoryManager.getAllProcesses();
            for (Process process : processes) {
                memoryManager.deallocateMemory(process);
            }
            updatePageTable();
        }
    }
    
    public void refresh() {
        updatePageTable();
    }
    
    public void stopTimer() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }
}