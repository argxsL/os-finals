package memory;

import java.util.*;

public class PagingManager {
    private int totalPages;
    private int pageSize;
    private boolean[] pageTable;
    private Map<Integer, Integer> pageOwners; // page number -> process id
    private Queue<Integer> freePages;
    private Queue<Integer> fifoQueue;
    private Map<Integer, Integer> lruCounter;
    private int accessCounter;
    
    public enum ReplacementAlgorithm {
        FIFO, LRU, OPTIMAL
    }
    
    private ReplacementAlgorithm currentAlgorithm;

    public PagingManager(int totalMemory, int pageSize) {
        this.totalPages = totalMemory / pageSize;
        this.pageSize = pageSize;
        this.pageTable = new boolean[totalPages];
        this.pageOwners = new HashMap<>();
        this.freePages = new LinkedList<>();
        this.fifoQueue = new LinkedList<>();
        this.lruCounter = new HashMap<>();
        this.accessCounter = 0;
        this.currentAlgorithm = ReplacementAlgorithm.FIFO;
        
        // Initialize free pages
        for (int i = 0; i < totalPages; i++) {
            freePages.offer(i);
        }
    }

    public boolean allocatePages(Process process) {
        int pagesNeeded = process.getPagesNeeded(pageSize);
        
        if (freePages.size() >= pagesNeeded) {
            // Allocate from free pages
            for (int i = 0; i < pagesNeeded; i++) {
                int pageNumber = freePages.poll();
                pageTable[pageNumber] = true;
                pageOwners.put(pageNumber, process.getProcessId());
                process.addAllocatedPage(pageNumber);
                fifoQueue.offer(pageNumber);
                lruCounter.put(pageNumber, accessCounter++);
            }
            return true;
        } else {
            // Try page replacement if no free pages
            return handlePageFault(process, pagesNeeded);
        }
    }

    public void deallocatePages(Process process) {
        for (int pageNumber : process.getAllocatedPages()) {
            pageTable[pageNumber] = false;
            pageOwners.remove(pageNumber);
            freePages.offer(pageNumber);
            fifoQueue.remove(pageNumber);
            lruCounter.remove(pageNumber);
        }
        process.getAllocatedPages().clear();
    }

    private boolean handlePageFault(Process process, int pagesNeeded) {
        if (pagesNeeded > totalPages) {
            return false; // Process too large
        }

        List<Integer> pagesToReplace = new ArrayList<>();
        
        // Find pages to replace based on algorithm
        switch (currentAlgorithm) {
            case FIFO:
                pagesToReplace = findFIFOPages(pagesNeeded);
                break;
            case LRU:
                pagesToReplace = findLRUPages(pagesNeeded);
                break;
            case OPTIMAL:
                pagesToReplace = findOptimalPages(pagesNeeded);
                break;
        }

        if (pagesToReplace.size() < pagesNeeded) {
            return false;
        }

        // Replace pages
        for (int pageToReplace : pagesToReplace) {
            Integer oldOwnerId = pageOwners.get(pageToReplace);
            if (oldOwnerId != null) {
                // Remove from old process (simplified - in real OS this would cause page fault)
                pageOwners.put(pageToReplace, process.getProcessId());
                process.addAllocatedPage(pageToReplace);
                fifoQueue.offer(pageToReplace);
                lruCounter.put(pageToReplace, accessCounter++);
            }
        }

        return true;
    }

    private List<Integer> findFIFOPages(int count) {
        List<Integer> pages = new ArrayList<>();
        Iterator<Integer> iterator = fifoQueue.iterator();
        while (iterator.hasNext() && pages.size() < count) {
            pages.add(iterator.next());
        }
        return pages;
    }

    private List<Integer> findLRUPages(int count) {
        return lruCounter.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private List<Integer> findOptimalPages(int count) {
        // Simplified optimal - just use LRU for this simulation
        return findLRUPages(count);
    }

    public void accessPage(int pageNumber) {
        if (pageNumber < totalPages && pageTable[pageNumber]) {
            lruCounter.put(pageNumber, accessCounter++);
        }
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean[] getPageTable() {
        return pageTable.clone();
    }

    public Map<Integer, Integer> getPageOwners() {
        return new HashMap<>(pageOwners);
    }

    public int getFreePages() {
        return freePages.size();
    }

    public void setReplacementAlgorithm(ReplacementAlgorithm algorithm) {
        this.currentAlgorithm = algorithm;
    }

    public ReplacementAlgorithm getCurrentAlgorithm() {
        return currentAlgorithm;
    }

    public double getFragmentation() {
        int usedPages = totalPages - freePages.size();
        return usedPages == 0 ? 0.0 : (double) freePages.size() / totalPages * 100;
    }
}