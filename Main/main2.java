public class main2 {

    // Constants
    static final int PAGE_SIZE = 1024; // bytes per page/frame

    // ----- Data Structures -----
    // A segment entry contains: base (page table start), limit (#pages), and a page table mapping
    static class Segment {
        int base;
        int limit;
        int[] pageTable; // page -> frame mapping

        Segment(int base, int limit, int[] pageTable) {
            this.base = base;
            this.limit = limit;
            this.pageTable = pageTable;
        }
    }

    // ----- Segment Table -----
    static Segment[] segmentTable = new Segment[3];

    // Initialize the segment table
    static {
        segmentTable[0] = new Segment(0, 2, new int[]{4, 7});     // Segment 0 → Pages 0–1
        segmentTable[1] = new Segment(100, 3, new int[]{2, 5, 9}); // Segment 1 → Pages 0–2
        segmentTable[2] = new Segment(200, 1, new int[]{13});      // Segment 2 → Page 0 only
    }

    // ----- Address Translation Method -----
    public static int findAddress(int segmentNum, int pageNum, int offset) throws Exception {
        // Step 1: Check segment validity
        if (segmentNum < 0 || segmentNum >= segmentTable.length) {
            throw new Exception("Segmentation Fault: Invalid Segment Number");
        }

        Segment seg = segmentTable[segmentNum];

        // Step 2: Check page validity
        if (pageNum >= seg.limit) {
            throw new Exception("Segmentation Fault: Page exceeds segment limit");
        }

        // Step 3: Get frame number from page table
        int frameNum = seg.pageTable[pageNum];

        // Step 4: Check offset validity
        if (offset >= PAGE_SIZE) {
            throw new Exception("Invalid Offset: exceeds page size");
        }

        // Step 5: Compute physical address
        int physicalAddress = frameNum * PAGE_SIZE + offset;
        return physicalAddress;
    }

    // ----- Main -----
    public static void main(String[] args) {
        System.out.println("Hello OS - Paging + Segmentation Demo\n");

        try {
            // Example 1
            int segment = 1, page = 2, offset = 300;
            int physAddr = findAddress(segment, page, offset);
            System.out.println("Logical Address (S=" + segment + ", P=" + page + ", O=" + offset + ") → Physical Address = " + physAddr);

            // Example 2
            segment = 0; page = 1; offset = 512;
            physAddr = findAddress(segment, page, offset);
            System.out.println("Logical Address (S=" + segment + ", P=" + page + ", O=" + offset + ") → Physical Address = " + physAddr);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Display segment table for reference
        System.out.println("\n--- Segment Table ---");
        for (int i = 0; i < segmentTable.length; i++) {
            Segment seg = segmentTable[i];
            System.out.print("Segment " + i + " (limit=" + seg.limit + "): ");
            for (int p = 0; p < seg.pageTable.length; p++) {
                System.out.print("Page " + p + "→Frame " + seg.pageTable[p] + "  ");
            }
            System.out.println();
        }
    }
}
