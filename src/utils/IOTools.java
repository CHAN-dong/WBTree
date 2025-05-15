package utils;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.HashMap;

public class IOTools {
    public static long low;
    public static long high;

    public static HashMap<Long, long[]> readData(String filePath) throws IOException {
        HashMap<Long, long[]> invertedIndex = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while((line = reader.readLine()) != null) {
                String[] keywordIds = line.split(" ");
                long[] ids = new long[keywordIds.length - 1];
                for (int j = 1; j < keywordIds.length; ++j) {
                    ids[j - 1] = Long.parseLong(keywordIds[j]);
                }
                Arrays.sort(ids);
                invertedIndex.put(Long.parseLong(keywordIds[0]), ids);
            }
        }
        return invertedIndex;
    }

    public static long[] readData(String filePath, int len) {
        low = Long.MAX_VALUE;
        high = Long.MIN_VALUE;
        long[] data = new long[len];
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            for (int i = 0; i < len && (line = reader.readLine()) != null; ++i) {
                data[i] = Long.parseLong(line);
                low = Math.min(data[i], low);
                high = Math.max(data[i], high);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static long getFileSize(String path) {
        FileInputStream fis  = null;
        try {
            File file = new File(path);
            fis = new FileInputStream(file);
            return fis.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public static void memoryHeapInfoPrint() {
        // 获取 MemoryMXBean
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        // 获取堆内存使用情况
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

        // 打印堆内存使用情况
        System.out.println("Heap Memory Usage:");
        System.out.println("  Initial: " + formatSize(heapMemoryUsage.getInit()));
        System.out.println("  Used: " + formatSize(heapMemoryUsage.getUsed()));
        System.out.println("  Committed: " + formatSize(heapMemoryUsage.getCommitted()));
        System.out.println("  Max: " + formatSize(heapMemoryUsage.getMax()));
    }
    private static String formatSize(long bytes) {
        long kilobytes = bytes / 1024 / 1024;
        return kilobytes + " MB";
    }
}
