package index;

import java.util.Arrays;


public class DataPoint {

    public static int d = 2;
    public int[] data;
    public int id = -1;
    public DataPoint() {
        this.data = new int[d];
    }
    public DataPoint(int[] data) {
        this.data = data;
    }

    public DataPoint copy() {
        return new DataPoint(Arrays.copyOf(data, data.length));
    }

    public DataPoint(int id, int[] data) {
        this.id = id;
        this.data = data;
    }

    public void setId(int id) {
        this.id = id;
    }
}
