package gui;

import memory.MemoryManager;
import memory.Process;
import utils.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class MainFrame extends JFrame {
    private MemoryManager memoryManager;
    private MemoryVisualizationPanel memoryVisualizationPanel;
    private PagingPanel pagingPanel;
    private SegmentationPanel segmentationPanel;
    private JTable processTable;
    private DefaultTableModel processTableModel;
    private JLabel statusLabel;
    private JRadioButton pagingRadio;
    private JRadioButton segmentationRadio;
    private Timer statusUpdateTimer;
    
    public MainFrame() {
        initializeMemoryManager();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupMenuBar();
        
        setTitle("Memory Management Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        
        // Status update timer
        statusUpdateTimer = new Timer(Constants.STATS_UPDATE_INTERVAL, e -> updateStatus());
        statusUpdateTimer.start();
        
        // Initial update
        updateProcessTable();
        updateStatus();
    }
    
    private void initializeMemoryManager() {
        memoryManager = new MemoryManager(Constants.DEFAULT_TOTAL_MEMORY, Constants.DEFAULT_PAGE_SIZE);
    }
    
    private void initializeComponents() {
        // Memory type selection
        pagingRadio = new JRadioButton("Paging", true);
        segmentationRadio = new JRadioButton("Segmentation", false);
        ButtonGroup memoryTypeGroup = new ButtonGroup();
        memoryTypeGroup.add(pagingRadio);
        memoryTypeGroup.add(segmentationRadio);
        
        // Memory visualization panel
        memoryVisualizationPanel = new MemoryVisualizationPanel(memoryManager);
        
        // Paging and segmentation panels
        pagingPanel = new PagingPanel(memoryManager);
        segmentationPanel = new SegmentationPanel(memoryManager);
        
        // Process table
        String[] columns = {"ID", "Name", "Size", "Priority", "Status"};
        processTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        processTable = new JTable(processTableModel);
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Top panel with controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createTitledBorder("Memory Management Type"));
        topPanel.add(pagingRadio);
        topPanel.add(segmentationRadio);
        
        JButton addProcessBtn = new JButton("Add Random Process");
        addProcessBtn.addActionListener(this::addRandomProcess);
        topPanel.add(addProcessBtn);
        
        JButton addCustomProcessBtn = new JButton("Add Custom Process");
        addCustomProcessBtn.addActionListener(this::addCustomProcess);
        topPanel.add(addCustomProcessBtn);
        
        JButton removeProcessBtn = new JButton("Remove Selected Process");
        removeProcessBtn.addActionListener(this::removeSelectedProcess);
        topPanel.add(removeProcessBtn);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Center panel with visualization
        add(memoryVisualizationPanel, BorderLayout.CENTER);
        
        // Right panel with controls and information
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(Constants.CONTROL_PANEL_WIDTH, 0));
        
        // Process list panel
        JPanel processPanel = new JPanel(new BorderLayout());
        processPanel.setBorder(BorderFactory.createTitledBorder("Processes"));
        JScrollPane processScrollPane = new JScrollPane(processTable);
        processScrollPane.setPreferredSize(new Dimension(280, 150));
        processPanel.add(processScrollPane, BorderLayout.CENTER);
        rightPanel.add(processPanel, BorderLayout.NORTH);
        
        // Tabbed pane for paging/segmentation details
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Paging", pagingPanel);
        tabbedPane.addTab("Segmentation", segmentationPanel);
        rightPanel.add(tabbedPane, BorderLayout.CENTER);
        
        add(rightPanel, BorderLayout.EAST);
        
        // Status bar
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        pagingRadio.addActionListener(e -> {
            memoryManager.setMemoryType(MemoryManager.MemoryType.PAGING);
            updateProcessTable();
            pagingPanel.refresh();
        });
        
        segmentationRadio.addActionListener(e -> {
            memoryManager.setMemoryType(MemoryManager.MemoryType.SEGMENTATION);
            updateProcessTable();
            segmentationPanel.refresh();
        });
    }
    
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem resetItem = new JMenuItem("Reset Simulation");
        resetItem.addActionListener(e -> resetSimulation());
        fileMenu.add(resetItem);
        
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            cleanup();
            System.exit(0);
        });
        fileMenu.add(exitItem);
        
        menuBar.add(fileMenu);
        
        // Simulation menu
        JMenu simulationMenu = new JMenu("Simulation");
        
        JMenuItem addMultipleItem = new JMenuItem("Add Multiple Processes");
        addMultipleItem.addActionListener(this::addMultipleProcesses);
        simulationMenu.add(addMultipleItem);
        
        JMenuItem memoryStressItem = new JMenuItem("Memory Stress Test");
        memoryStressItem.addActionListener(this::runMemoryStressTest);
        simulationMenu.add(memoryStressItem);
        
        menuBar.add(simulationMenu);
        
        // Help menu
        JMenu helpMenu = new JMenu("Help");
        
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(this::showAbout);
        helpMenu.add(aboutItem);
        
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void addRandomProcess(ActionEvent e) {
        Process process = MemoryUtils.generateRandomProcess();
        Process createdProcess = memoryManager.createProcess(
            process.getName(), 
            process.getSize(), 
            process.getPriority()
        );
        
        boolean allocated = memoryManager.allocateMemory(createdProcess);
        if (!allocated) {
            JOptionPane.showMessageDialog(this, 
                "Failed to allocate memory for process: " + createdProcess.getName() + 
                "\nProcess size: " + MemoryUtils.formatMemorySize(createdProcess.getSize()),
                "Allocation Failed", 
                JOptionPane.WARNING_MESSAGE);
        }
        
        updateProcessTable();
    }
    
    private void addCustomProcess(ActionEvent e) {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        
        JTextField nameField = new JTextField("CustomProcess");
        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(64, 
            Constants.MIN_PROCESS_SIZE, Constants.MAX_PROCESS_SIZE, 16));
        JSpinner prioritySpinner = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
        
        panel.add(new JLabel("Process Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Size (KB):"));
        panel.add(sizeSpinner);
        panel.add(new JLabel("Priority (1-10):"));
        panel.add(prioritySpinner);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Create Custom Process", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "CustomProcess";
            
            int size = (Integer) sizeSpinner.getValue();
            int priority = (Integer) prioritySpinner.getValue();
            
            Process process = memoryManager.createProcess(name, size, priority);
            boolean allocated = memoryManager.allocateMemory(process);
            
            if (!allocated) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to allocate memory for process: " + name,
                    "Allocation Failed", 
                    JOptionPane.WARNING_MESSAGE);
            }
            
            updateProcessTable();
        }
    }
    
    private void removeSelectedProcess(ActionEvent e) {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow >= 0) {
            int processId = (Integer) processTableModel.getValueAt(selectedRow, 0);
            memoryManager.terminateProcess(processId);
            updateProcessTable();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Please select a process to remove.", 
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void addMultipleProcesses(ActionEvent e) {
        String input = JOptionPane.showInputDialog(this, 
            "How many random processes to add?", 
            "Add Multiple Processes", 
            JOptionPane.QUESTION_MESSAGE);
        
        if (input != null) {
            try {
                int count = Integer.parseInt(input);
                count = MemoryUtils.clamp(count, 1, 20);
                
                int successful = 0;
                for (int i = 0; i < count; i++) {
                    Process process = MemoryUtils.generateRandomProcess();
                    Process createdProcess = memoryManager.createProcess(
                        process.getName(), 
                        process.getSize(), 
                        process.getPriority()
                    );
                    
                    if (memoryManager.allocateMemory(createdProcess)) {
                        successful++;
                    }
                }
                
                updateProcessTable();
                JOptionPane.showMessageDialog(this, 
                    String.format("Successfully allocated %d out of %d processes.", successful, count),
                    "Batch Process Creation", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter a valid number.", 
                    "Invalid Input", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void runMemoryStressTest(ActionEvent e) {
        int result = JOptionPane.showConfirmDialog(this,
            "This will create many processes to test memory limits.\nContinue?",
            "Memory Stress Test",
            JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    for (int i = 0; i < 50; i++) {
                        Process process = MemoryUtils.generateRandomProcess();
                        Process createdProcess = memoryManager.createProcess(
                            "StressTest" + i, 
                            process.getSize(), 
                            process.getPriority()
                        );
                        
                        memoryManager.allocateMemory(createdProcess);
                        publish(i + 1);
                        Thread.sleep(100);
                    }
                    return null;
                }
                
                @Override
                protected void process(List<Integer> chunks) {
                    updateProcessTable();
                }
                
                @Override
                protected void done() {
                    updateProcessTable();
                    JOptionPane.showMessageDialog(MainFrame.this, 
                        "Memory stress test completed!", 
                        "Stress Test", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            };
            
            worker.execute();
        }
    }
    
    private void resetSimulation() {
        memoryManager.reset();
        updateProcessTable();
        pagingPanel.refresh();
        segmentationPanel.refresh();
        JOptionPane.showMessageDialog(this, 
            "Simulation reset successfully.", 
            "Reset", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showAbout(ActionEvent e) {
        String aboutText = 
            "Memory Management Simulator\n\n" +
            "This application simulates two memory management techniques:\n" +
            "• Paging: Fixed-size memory blocks with page replacement algorithms\n" +
            "• Segmentation: Variable-size memory segments with compaction\n\n" +
            "Features:\n" +
            "• Visual memory representation\n" +
            "• Multiple page replacement algorithms (FIFO, LRU, Optimal)\n" +
            "• Memory compaction for segmentation\n" +
            "• Real-time statistics and fragmentation analysis\n" +
            "• Process creation and management\n\n" +
            "Developed for Operating Systems Education";
        
        JOptionPane.showMessageDialog(this, aboutText, "About", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void updateProcessTable() {
        processTableModel.setRowCount(0);
        
        List<Process> processes = memoryManager.getAllProcesses();
        for (Process process : processes) {
            String status = process.isActive() ? "Active" : "Terminated";
            processTableModel.addRow(new Object[]{
                process.getProcessId(),
                process.getName(),
                MemoryUtils.formatMemorySize(process.getSize()),
                process.getPriority(),
                status
            });
        }
    }
    
    private void updateStatus() {
        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        if (stats != null) {
            String statusText = String.format(
                "Memory: %s/%s (%.1f%%) | Processes: %d active, %d total | Fragmentation: %.1f%%",
                MemoryUtils.formatMemorySize(stats.getUsedMemory()),
                MemoryUtils.formatMemorySize(stats.getTotalMemory()),
                stats.getUtilization(),
                stats.getActiveProcesses(),
                stats.getTotalProcesses(),
                stats.getFragmentation()
            );
            statusLabel.setText(statusText);
        }
    }
    
    private void cleanup() {
        if (statusUpdateTimer != null) {
            statusUpdateTimer.stop();
        }
        if (memoryVisualizationPanel != null) {
            memoryVisualizationPanel.stopAnimation();
        }
        if (pagingPanel != null) {
            pagingPanel.stopTimer();
        }
        if (segmentationPanel != null) {
            segmentationPanel.stopTimer();
        }
    }
    
    @Override
    public void dispose() {
        cleanup();
        super.dispose();
    }
}