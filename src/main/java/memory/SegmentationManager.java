package memory;

import java.util.*;

public class SegmentationManager {
    private int totalMemory;
    private List<MemorySegment> memorySegments;
    private Map<Integer, List<MemorySegment>> processSegments; // process id -> segments
    
    public static class MemorySegment {
        private int startAddress;
        private int size;
        private boolean allocated;
        private int processId;
        private String segmentType;
        
        public MemorySegment(int startAddress, int size) {
            this.startAddress = startAddress;
            this.size = size;
            this.allocated = false;
            this.processId = -1;
            this.segmentType = "FREE";
        }
        
        public MemorySegment(int startAddress, int size, int processId, String segmentType) {
            this.startAddress = startAddress;
            this.size = size;
            this.allocated = true;
            this.processId = processId;
            this.segmentType = segmentType;
        }
        
        // Getters and setters
        public int getStartAddress() { return startAddress; }
        public int getSize() { return size; }
        public boolean isAllocated() { return allocated; }
        public int getProcessId() { return processId; }
        public String getSegmentType() { return segmentType; }
        public int getEndAddress() { return startAddress + size - 1; }
        
        public void setAllocated(boolean allocated) { this.allocated = allocated; }
        public void setProcessId(int processId) { this.processId = processId; }
        public void setSegmentType(String segmentType) { this.segmentType = segmentType; }
    }

    public SegmentationManager(int totalMemory) {
        this.totalMemory = totalMemory;
        this.memorySegments = new ArrayList<>();
        this.processSegments = new HashMap<>();
        
        // Initialize with one large free segment
        memorySegments.add(new MemorySegment(0, totalMemory));
    }

    public boolean allocateSegment(Process process, int segmentSize, String segmentType) {
        // Find best fit segment
        MemorySegment bestFit = findBestFit(segmentSize);
        
        if (bestFit == null) {
            // Try compaction
            compact();
            bestFit = findBestFit(segmentSize);
        }
        
        if (bestFit == null) {
            return false; // Cannot allocate
        }
        
        // Split segment if necessary
        if (bestFit.getSize() > segmentSize) {
            MemorySegment newSegment = new MemorySegment(
                bestFit.getStartAddress() + segmentSize,
                bestFit.getSize() - segmentSize
            );
            memorySegments.add(newSegment);
        }
        
        // Allocate the segment
        bestFit.setAllocated(true);
        bestFit.setProcessId(process.getProcessId());
        bestFit.setSegmentType(segmentType);
        
        // Update process segments
        processSegments.computeIfAbsent(process.getProcessId(), k -> new ArrayList<>())
                      .add(bestFit);
        
        // Update segment size
        if (bestFit.getSize() > segmentSize) {
            MemorySegment allocatedSegment = new MemorySegment(
                bestFit.getStartAddress(), segmentSize, process.getProcessId(), segmentType
            );
            memorySegments.remove(bestFit);
            memorySegments.add(allocatedSegment);
            
            processSegments.get(process.getProcessId()).remove(bestFit);
            processSegments.get(process.getProcessId()).add(allocatedSegment);
        }
        
        sortSegments();
        return true;
    }

    public void deallocateSegments(Process process) {
        List<MemorySegment> segments = processSegments.get(process.getProcessId());
        if (segments == null) return;
        
        for (MemorySegment segment : segments) {
            segment.setAllocated(false);
            segment.setProcessId(-1);
            segment.setSegmentType("FREE");
        }
        
        processSegments.remove(process.getProcessId());
        mergeAdjacent();
    }

    private MemorySegment findBestFit(int size) {
        MemorySegment bestFit = null;
        for (MemorySegment segment : memorySegments) {
            if (!segment.isAllocated() && segment.getSize() >= size) {
                if (bestFit == null || segment.getSize() < bestFit.getSize()) {
                    bestFit = segment;
                }
            }
        }
        return bestFit;
    }

    private void compact() {
        List<MemorySegment> allocatedSegments = new ArrayList<>();
        int totalFreeSpace = 0;
        
        for (MemorySegment segment : memorySegments) {
            if (segment.isAllocated()) {
                allocatedSegments.add(segment);
            } else {
                totalFreeSpace += segment.getSize();
            }
        }
        
        memorySegments.clear();
        
        // Place allocated segments at the beginning
        int currentAddress = 0;
        for (MemorySegment segment : allocatedSegments) {
            MemorySegment compactedSegment = new MemorySegment(
                currentAddress, segment.getSize(), segment.getProcessId(), segment.getSegmentType()
            );
            memorySegments.add(compactedSegment);
            currentAddress += segment.getSize();
            
            // Update process segments reference
            List<MemorySegment> processSegs = processSegments.get(segment.getProcessId());
            if (processSegs != null) {
                processSegs.remove(segment);
                processSegs.add(compactedSegment);
            }
        }
        
        // Add remaining free space as one segment
        if (totalFreeSpace > 0) {
            memorySegments.add(new MemorySegment(currentAddress, totalFreeSpace));
        }
        
        sortSegments();
    }

    private void mergeAdjacent() {
        sortSegments();
        List<MemorySegment> merged = new ArrayList<>();
        
        for (MemorySegment current : memorySegments) {
            if (merged.isEmpty() || merged.get(merged.size() - 1).isAllocated() || 
                current.isAllocated() || 
                merged.get(merged.size() - 1).getEndAddress() + 1 != current.getStartAddress()) {
                merged.add(current);
            } else {
                // Merge with previous free segment
                MemorySegment last = merged.get(merged.size() - 1);
                MemorySegment mergedSegment = new MemorySegment(
                    last.getStartAddress(), 
                    last.getSize() + current.getSize()
                );
                merged.set(merged.size() - 1, mergedSegment);
            }
        }
        
        memorySegments = merged;
    }

    private void sortSegments() {
        memorySegments.sort(Comparator.comparingInt(MemorySegment::getStartAddress));
    }

    public List<MemorySegment> getMemorySegments() {
        return new ArrayList<>(memorySegments);
    }

    public int getTotalMemory() {
        return totalMemory;
    }

    public int getFreeMemory() {
        return memorySegments.stream()
                .filter(s -> !s.isAllocated())
                .mapToInt(MemorySegment::getSize)
                .sum();
    }

    public double getFragmentation() {
        List<MemorySegment> freeSegments = memorySegments.stream()
                .filter(s -> !s.isAllocated())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        if (freeSegments.isEmpty()) return 0.0;
        
        int totalFree = freeSegments.stream().mapToInt(MemorySegment::getSize).sum();
        int largestFree = freeSegments.stream().mapToInt(MemorySegment::getSize).max().orElse(0);
        
        return totalFree == 0 ? 0.0 : (double)(totalFree - largestFree) / totalFree * 100;
    }

    public Map<Integer, List<MemorySegment>> getProcessSegments() {
        return new HashMap<>(processSegments);
    }
}