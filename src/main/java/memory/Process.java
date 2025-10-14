package memory;

import java.util.ArrayList;
import java.util.List;

public class Process {
    private int processId;
    private String name;
    private int size;
    private List<Integer> allocatedPages;
    private List<Integer> allocatedSegments;
    private boolean isActive;
    private int priority;

    public Process(int processId, String name, int size, int priority) {
        this.processId = processId;
        this.name = name;
        this.size = size;
        this.priority = priority;
        this.allocatedPages = new ArrayList<>();
        this.allocatedSegments = new ArrayList<>();
        this.isActive = true;
    }

    public int getProcessId() {
        return processId;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public List<Integer> getAllocatedPages() {
        return allocatedPages;
    }

    public List<Integer> getAllocatedSegments() {
        return allocatedSegments;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getPriority() {
        return priority;
    }

    public void addAllocatedPage(int pageNumber) {
        allocatedPages.add(pageNumber);
    }

    public void removeAllocatedPage(int pageNumber) {
        allocatedPages.remove(Integer.valueOf(pageNumber));
    }

    public void addAllocatedSegment(int segmentNumber) {
        allocatedSegments.add(segmentNumber);
    }

    public void removeAllocatedSegment(int segmentNumber) {
        allocatedSegments.remove(Integer.valueOf(segmentNumber));
    }

    public int getPagesNeeded(int pageSize) {
        return (int) Math.ceil((double) size / pageSize);
    }

    @Override
    public String toString() {
        return String.format("Process[ID=%d, Name=%s, Size=%d, Active=%b]", 
                           processId, name, size, isActive);
    }
}