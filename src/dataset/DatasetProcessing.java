package dataset;

import java.io.*;
import java.util.*;

public class DatasetProcessing {

    public static void saveDatasetToFile(int[][] data, String filename, int n) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < n; i++) {
//                writer.write(String.valueOf(i));
                writer.write(data[i][0] + " " + data[i][1]);
//                for (int value : data[i]) {
//                    writer.write(" " + value);
//                }
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("failed!" + e.getMessage());
        }
    }

    public static void saveQueryWorkloadToFile(int[][][] queries, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int[][] query : queries) {
                int d = query[0].length;
                for (int i = 0; i < d; i++) {
                    writer.write(String.valueOf(query[0][i]));
                    if (i != d - 1) {
                        writer.write(" ");
                    } else {
                        writer.write(",");
                    }
                }
                for (int i = 0; i < d; i++) {
                    writer.write(String.valueOf(query[1][i]));
                    if (i != d - 1) {
                        writer.write(" ");
                    }
                }
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("failed!" + e.getMessage());
        }
    }


    public static int[][] generateSkewedSpaceData(int n, int d, int[] minValues, int[] maxValues, double r) {
        int[][] data = new int[n][];
        Random random = new Random();
        HashSet<String> st = new HashSet<>();
        int i = 0;
        while (st.size() < n) {
            int[] point = new int[d];
            for (int j = 0; j < d; j++) {
                double u = random.nextDouble();
                double skewedU = 1 - Math.pow(1 - u, r); // 应用倾斜变换
                point[j] = (int) (minValues[j] + skewedU * (maxValues[j] - minValues[j]));
            }
            String str = Arrays.toString(point);
            if (!st.contains(str)) {
                data[i] = point;
                st.add(str);
                ++i;
            }
        }
        return data;
    }


    public static int[][][] generateRectangles_uniform(int n, int[] minValues, int[] maxValues, double range) {
        int[][][] rectangles = new int[n][2][2];
        int len = (int) Math.sqrt((maxValues[0] - minValues[0])* range * (maxValues[1] - minValues[1]) ) / 2;
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            int[][] query = new int[2][2];
            for (int j = 0; j < 2; ++j) {
                double u = random.nextDouble();
                int center = (int) (minValues[j] + u * (maxValues[j] - minValues[j]));
                query[0][j] = Math.max(minValues[j], center - len);
                query[1][j] = Math.min(maxValues[j], center + len);
            }
            rectangles[i] = query;
        }
        return rectangles;
    }

    public static int[][][] generateRectangles_gaussian(int n, int[] minValues, int[] maxValues, double range, int dataSize) {
        int[][][] rectangles = new int[n][2][2];
        int len = (int) Math.sqrt((maxValues[0] - minValues[0]) * range * (maxValues[1] - minValues[1]));
        Random random = new Random();

        // 计算数据空间中心
        double[] mu = new double[2];
        for (int j = 0; j < 2; j++) {
            mu[j] = (minValues[j] + maxValues[j]) / 2.0;
        }
        double sigma = dataSize / 1000; // 高斯分布的标准差

        for (int i = 0; i < n; i++) {
            int[][] query = new int[2][2];
            for (int j = 0; j < 2; j++) {
                double u1 = random.nextDouble();
                double u2 = random.nextDouble();
                double z0 = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2); // 标准正态分布
                double offset = z0 * sigma; // 调整标准差

                // 计算中心点并限制范围
                int center = (int) Math.round(mu[j] + offset);
                center = Math.max(minValues[j], Math.min(maxValues[j], center));

                // 生成矩形边界
                query[0][j] = Math.max(minValues[j], center - len);
                query[1][j] = Math.min(maxValues[j], center + len);
            }
            rectangles[i] = query;
        }
        return rectangles;
    }

    public static int[][][] generateRectangles_laplace(int n, int[] minValues, int[] maxValues, double range, int dataSize) {
        int[][][] rectangles = new int[n][2][2];
        int len = (int) Math.sqrt((maxValues[0] - minValues[0]) * range * (maxValues[1] - minValues[1]));
        Random random = new Random();

        // 计算数据空间中心
        double[] mu = new double[2];
        for (int j = 0; j < 2; j++) {
            mu[j] = (minValues[j] + maxValues[j]) / 2.0;
        }

        //dataset size / 10
        double b = dataSize / 1000; // Laplace的尺度参数

        for (int i = 0; i < n; i++) {
            int[][] query = new int[2][2];
            for (int j = 0; j < 2; j++) {
                // 生成Laplace分布的随机数
                double u = random.nextDouble();
                double offset = -b * Math.signum(u - 0.5) * Math.log(1 - 2 * Math.abs(u - 0.5));

                // 计算中心点并限制范围
                int center = (int) Math.round(mu[j] + offset);
                center = Math.max(minValues[j], Math.min(maxValues[j], center));

                // 生成矩形边界
                query[0][j] = Math.max(minValues[j], center - len);
                query[1][j] = Math.min(maxValues[j], center + len);
            }
            rectangles[i] = query;
        }
        return rectangles;
    }



    public static int[][][] generateRectangles(int n, int d, int[] minValues, int[] maxValues, double minRate, double maxRate, double concentration) {
        int[][][] rectangles = new int[n][2][d];
        Random random = new Random();

        // 计算整个空间的体积
        double totalVolume = 1.0;
        for (int i = 0; i < d; i++) {
            totalVolume *= (maxValues[i] - minValues[i]);
        }

        // 空间中心
        int[] spaceCenter = new int[d];
        for (int i = 0; i < d; i++) {
            spaceCenter[i] = (minValues[i] + maxValues[i]) / 2;
        }

        // 标准差（concentration 控制分布的集中度）
        double[] stdDevs = new double[d];
        for (int i = 0; i < d; i++) {
            // Concentration 越小，分布越集中；越大，分布越分散
            stdDevs[i] = (maxValues[i] - minValues[i]) * concentration;
        }

        for (int i = 0; i < n; i++) {
            int[] lowerBound = new int[d];
            int[] upperBound = new int[d];
            double[] lengths = new double[d];

            // 生成符合正态分布的中心点
            int[] centerPoint = new int[d];
            for (int j = 0; j < d; j++) {
                centerPoint[j] = (int) Math.round(spaceCenter[j] + random.nextGaussian() * stdDevs[j]);
                centerPoint[j] = Math.max(minValues[j], Math.min(centerPoint[j], maxValues[j]));
            }

            // 随机确定当前矩形的目标体积（在 minRate 和 maxRate 之间）
            double targetRate = minRate + random.nextDouble() * (maxRate - minRate);
            double targetVolume = totalVolume * targetRate;

            // 计算边长，确保总体积接近目标体积，并且各维度边长近似
            double volume = 1.0;
            for (int j = 0; j < d; j++) {
                if (j == 0) {
                    // 第一个维度，根据目标体积和维度数量估算初始边长
                    lengths[j] = Math.pow(targetVolume, 1.0 / d);
                } else {
                    // 保持边长比例（不超过±50%变化）
                    double minAllowed = lengths[j - 1] * 0.5;
                    double maxAllowed = lengths[j - 1] * 1.5;
                    int maxPossible = maxValues[j] - minValues[j];
                    lengths[j] = Math.min(maxAllowed, maxPossible);
                    lengths[j] = Math.max(minAllowed, lengths[j]);
                }

                // 确保边长合理
                lengths[j] = Math.min(maxValues[j] - minValues[j], Math.max(1, (int) lengths[j]));
                volume *= lengths[j];
            }

            // 调整所有边长，确保总体积接近目标体积
            if (volume != 0) {
                double scaleFactor = Math.pow(targetVolume / volume, 1.0 / d);
                for (int j = 0; j < d; j++) {
                    lengths[j] *= scaleFactor;
                    lengths[j] = Math.min(maxValues[j] - minValues[j], Math.max(1, (int) lengths[j]));
                }
            }

            // 确定每个维度的左下角和右上角坐标
            for (int j = 0; j < d; j++) {
                // 计算中心点附近的边长，确保不超出空间范围
                int halfLength = (int) (lengths[j] / 2);
                lowerBound[j] = centerPoint[j] - halfLength;
                upperBound[j] = centerPoint[j] + halfLength;

                // 确保边界在空间范围内
                if (lowerBound[j] < minValues[j]) {
                    lowerBound[j] = minValues[j];
                    upperBound[j] = Math.min(maxValues[j], lowerBound[j] + (int) lengths[j]);
                } else if (upperBound[j] > maxValues[j]) {
                    upperBound[j] = maxValues[j];
                    lowerBound[j] = Math.max(minValues[j], upperBound[j] - (int) lengths[j]);
                }
            }

            rectangles[i][0] = lowerBound;
            rectangles[i][1] = upperBound;
        }

        return rectangles;
    }


    public static int[][] readBDPDataset(String filePath, int length, int[][] range) {
        HashSet<int[]> st = new HashSet<>();
        range[0][0] = range[0][1] = Integer.MAX_VALUE;
        range[1][0] = range[1][1] = Integer.MIN_VALUE;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while (st.size() < length && (line = br.readLine()) != null) {
                String[] values = line.split(",");
                int[] data = new int[2];
                for (int i = 0; i < 2; i++) {
                    data[i] = (int) (Double.parseDouble(values[i]) * 800);
                    range[1][i] = Math.max(range[1][i], data[i]);
                    range[0][i] = Math.min(range[0][i], data[i]);
                }
                st.add(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int id = 0;
        int[][] data = new int[length][];
        for (int[] p : st) {
            p[0] -= range[0][0];
            p[1] -= range[0][1];
            data[id++] = p;
        }

        return data;
    }

    public static void generateDatasetQueryWorkload(String datasetPath, String workloadPath, double rangeRate, String type, int dataSize) {
        int[][] range = new int[2][2];
        readDataset(datasetPath, dataSize, range);
        int[][][] workloads = null;
        switch (type) {
            case "UNI":
                workloads = generateRectangles_uniform(800, range[0], range[1], rangeRate);
                saveQueryWorkloadToFile(workloads, workloadPath);
                break;
            case "GAU":
                workloads = generateRectangles_gaussian(800, range[0], range[1], rangeRate, dataSize);
                saveQueryWorkloadToFile(workloads, workloadPath);
                break;
            case "LAP":
                workloads = generateRectangles_laplace(800, range[0], range[1], rangeRate, dataSize);
                saveQueryWorkloadToFile(workloads, workloadPath);
                break;
            case "MIX":
                int[][][] workloads1 = generateRectangles_uniform(400, range[0], range[1], rangeRate);
                int[][][] workloads2 = generateRectangles_laplace(400, range[0], range[1], rangeRate, dataSize);
                workloads = Arrays.copyOf(workloads1, workloads1.length + workloads2.length);
                System.arraycopy(workloads2, 0, workloads, workloads2.length, workloads2.length);
                saveQueryWorkloadToFile(workloads, workloadPath);
        }
    }

    public static int[][] readDataset(String filePath, int n, int[][] range) {
        int[][] dataset = new int[n][2];
        range[0][0] = range[0][1] = Integer.MAX_VALUE;
        range[1][0] = range[1][1] = Integer.MIN_VALUE;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");
                int[] data = new int[values.length];
                for (int i = 0; i < values.length; i++) {
                    data[i] = Integer.parseInt(values[i]);
                    range[1][i] = Math.max(range[1][i], data[i]);
                    range[0][i] = Math.min(range[0][i], data[i]);
                }
                dataset[id] = data;
                id++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataset;
    }



    public static void main(String[] args) {

        int n = 100000, upd_n = 100000;
        int[] minValues = new int[] {0,0};
        int[] maxValues = new int[] {20000,20000};
//
////        当 r = 2 时，数据点更集中在各维度最大值附近。当 r = 0.5 时，数据点更集中在各维度最小值附近。当 r = 1 时，生成的数据与原均匀分布代码一致。
////        int[][] dataset = generateSkewedSpaceData(n, 2, minValues, maxValues,2);
////
        int[][] dataset = readBDPDataset("D:\\paper_source\\work_6\\dataset\\OSM.csv", n, new int[2][2]);
        saveDatasetToFile(dataset, "./src/dataset/OSM_test", n);


//        generateDatasetQueryWorkload("D:\\paper_source\\work_6\\dataset\\" + "OSM" + "_1M", "D:\\paper_source\\work_6\\dataset\\" + "GAU" + "_Workload_" + "OSM" + "_1M_R" + 0.01 + "%", 0.01 * 0.01, "GAU");


        String[] datasetNames = new String[] {"OSM"};
        String[] typeNames = new String[] {"UNI", "GAU", "LAP", "MIX"};
        for (String datasetName : datasetNames) {
            for (String type : typeNames) {

                for (int i = 1; i <= 5; ++i) {
                    double rangeRate;
                    if (i == 3) {
                        rangeRate = 0.6;
                    } else {
                        rangeRate = 0.2 * i;
                    }
                    String datasetPath = "./src/dataset/" + datasetName + "_test";
                    String workloadPath = "./src/dataset/" + type + "_Workload_" + datasetName + "_test_R" + rangeRate + "%";
                    generateDatasetQueryWorkload(datasetPath, workloadPath, rangeRate * 0.01, type, n);
                }
            }
        }



    }



}
