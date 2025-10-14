import java.util.Scanner;
public class Main {

     // <-- normally this goes at the very top (outside class)

    // Constants
    static final int PAGE_SIZE = 1024;

    // Segment Table (each segment has its own page table)
    static class Segment {
        int base;
        int limit;
        int[] pageTable;

        Segment(int base, int limit, int[] pageTable) {
            this.base = base;
            this.limit = limit;
            this.pageTable = pageTable;
        }
    }

    // Example: predefined segment table
    static Segment[] segmentTable = {
            new Segment(0, 2, new int[]{4, 7}),        // Segment 0
            new Segment(100, 3, new int[]{2, 5, 9}),   // Segment 1
            new Segment(200, 1, new int[]{13})         // Segment 2
    };

    public int[] FindAddress(int segmentNum, int pageNum, int offset) {
        int[] result = new int[1]; // store physical address

        try {
            if (segmentNum < 0 || segmentNum >= segmentTable.length) {
                throw new Exception("Segmentation Fault: Invalid Segment Number");
            }

            Segment seg = segmentTable[segmentNum];

            if (pageNum >= seg.limit) {
                throw new Exception("Segmentation Fault: Page exceeds segment limit");
            }

            if (offset >= PAGE_SIZE) {
                throw new Exception("Offset exceeds page size");
            }

            int frameNum = seg.pageTable[pageNum];
            int physicalAddress = frameNum * PAGE_SIZE + offset;
            result[0] = physicalAddress;

            System.out.println("Physical Address = " + physicalAddress);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return result;
    }

    public static void main(String[] args) {
        System.out.println("hello os");

        Main mmu = new Main();
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter Segment Number: ");
        int s = sc.nextInt();
        System.out.print("Enter Page Number: ");
        int p = sc.nextInt();
        System.out.print("Enter Offset: ");
        int o = sc.nextInt();

        mmu.FindAddress(s, p, o);
    }
}
