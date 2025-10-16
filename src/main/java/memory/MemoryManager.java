package memory;

import java.util.*;

public class MemoryManager {
    private PagingManager pagingManager;
    private SegmentationManager segmentationManager;
    private List<Process> processes;
    private int nextProcessId;
    
    public enum MemoryType {
        PAGING, SEGMENTATION
    }
    
    private MemoryType currentType;

    public MemoryManager(int totalMemory, int pageSize) {
        this.pagingManager = new PagingManager(totalMemory, pageSize);
        this.segmentationManager = new SegmentationManager(totalMemory);
        this.processes = new ArrayList<>();
        this.nextProcessId = 1;
        this.currentType = MemoryType.PAGING;
    }

    public Process createProcess(String name, int size, int priority) {
        Process process = new Process(nextProcessId++, name, size, priority);
        processes.add(process);
        return process;
    }

    public boolean allocateMemory(Process process) {
        switch (currentType) {
            case PAGING:
                return pagingManager.allocatePages(process);
            case SEGMENTATION:
                // Allocate segments for code, data, and stack
                int codeSize = process.getSize() / 3;
                int dataSize = process.getSize() / 3;
                int stackSize = process.getSize() - codeSize - dataSize;
                
                boolean allocated = true;
                allocated &= segmentationManager.allocateSegment(process, codeSize, "CODE");
                allocated &= segmentationManager.allocateSegment(process, dataSize, "DATA");
                allocated &= segmentationManager.allocateSegment(process, stackSize, "STACK");
                
                if (!allocated) {
                    // If allocation failed, deallocate any already allocated segments
                    segmentationManager.deallocateSegments(process);
                }
                return allocated;
        }
        return false;
    }

    public void deallocateMemory(Process process) {
        switch (currentType) {
            case PAGING:
                pagingManager.deallocatePages(process);
                break;
            case SEGMENTATION:
                segmentationManager.deallocateSegments(process);
                break;
        }
        process.setActive(false);
    }

    public void terminateProcess(int processId) {
        Process process = findProcess(processId);
        if (process != null) {
            deallocateMemory(process);
            processes.remove(process);
        }
    }

    public Process findProcess(int processId) {
        return processes.stream()
                .filter(p -> p.getProcessId() == processId)
                .findFirst()
                .orElse(null);
    }

    public List<Process> getActiveProcesses() {
        return processes.stream()
                .filter(Process::isActive)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<Process> getAllProcesses() {
        return new ArrayList<>(processes);
    }

    public PagingManager getPagingManager() {
        return pagingManager;
    }

    public SegmentationManager getSegmentationManager() {
        return segmentationManager;
    }

    public void setMemoryType(MemoryType type) {
        // Clear current allocations when switching
        for (Process process : getActiveProcesses()) {
            deallocateMemory(process);
        }
        
        this.currentType = type;
        
        // Reallocate with new memory management scheme
        for (Process process : processes) {
            if (!process.isActive()) {
                process.setActive(true);
                allocateMemory(process);
            }
        }
    }

    public MemoryType getCurrentType() {
        return currentType;
    }

    public MemoryStats getMemoryStats() {
        switch (currentType) {
            case PAGING:
                return new MemoryStats(
                    pagingManager.getTotalPages() * pagingManager.getPageSize(),
                    pagingManager.getFreePages() * pagingManager.getPageSize(),
                    pagingManager.getFragmentation(),
                    processes.size(),
                    getActiveProcesses().size()
                );
            case SEGMENTATION:
                return new MemoryStats(
                    segmentationManager.getTotalMemory(),
                    segmentationManager.getFreeMemory(),
                    segmentationManager.getFragmentation(),
                    processes.size(),
                    getActiveProcesses().size()
                );
        }
        return null;
    }

    public static class MemoryStats {
        private int totalMemory;
        private int freeMemory;
        private double fragmentation;
        private int totalProcesses;
        private int activeProcesses;

        public MemoryStats(int totalMemory, int freeMemory, double fragmentation, 
                          int totalProcesses, int activeProcesses) {
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.fragmentation = fragmentation;
            this.totalProcesses = totalProcesses;
            this.activeProcesses = activeProcesses;
        }

        // Getters
        public int getTotalMemory() { return totalMemory; }
        public int getFreeMemory() { return freeMemory; }
        public int getUsedMemory() { return totalMemory - freeMemory; }
        public double getFragmentation() { return fragmentation; }
        public int getTotalProcesses() { return totalProcesses; }
        public int getActiveProcesses() { return activeProcesses; }
        public double getUtilization() { 
            return totalMemory == 0 ? 0.0 : (double) getUsedMemory() / totalMemory * 100; 
        }
    }

    public void reset() {
        processes.clear();
        nextProcessId = 1;
        pagingManager = new PagingManager(
            pagingManager.getTotalPages() * pagingManager.getPageSize(),
            pagingManager.getPageSize()
        );
        segmentationManager = new SegmentationManager(segmentationManager.getTotalMemory());
    }
}