package dataset;

import index.DataPoint;
import index.SDC;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.io.BufferedReader;

public class ReadDataset {

    static int maxId = 0;

    public static List<DataPoint> readDataset(String filePath) {
        List<DataPoint> dataPoints = new ArrayList<DataPoint>();
        int maxValue = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");
                int id = maxId++;
                int[] data = new int[values.length];
                for (int i = 0; i < values.length; i++) {
                    data[i] = Integer.parseInt(values[i]);
                    maxValue = Math.max(maxValue, data[i]);
                }
                dataPoints.add(new DataPoint(id, data));
            }
            SDC.setMaxBitLen(32 - Integer.numberOfLeadingZeros(maxValue + 1));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataPoints;
    }

    public static HashSet<DataPoint[]> readWorkload(String filePath, int size) {
        HashSet<DataPoint[]> dataPoints = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null && dataPoints.size() < size) {
                String[] rangeStr = line.split(",");
                DataPoint[] range = new DataPoint[2];
                for (int i = 0; i < 2; ++i) {
                    String[] pointStr = rangeStr[i].split(" ");
                    int[] point = new int[pointStr.length];
                    for (int j = 0; j < pointStr.length; j++) {
                        point[j] = Integer.parseInt(pointStr[j]);
                    }
                    range[i] = new DataPoint(point);
                }
                dataPoints.add(range);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataPoints;
    }


    public static void main(String[] args) {

    }

}
