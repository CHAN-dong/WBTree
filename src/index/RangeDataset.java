package index;

import java.util.Arrays;
import java.util.List;

public class RangeDataset {

    public DataPoint[] range;
    public List<DataPoint> dataset;

    public RangeDataset(List<DataPoint> rangeDataset, DataPoint[] range) {
        this.dataset = rangeDataset;
        this.range = range;
    }

    public RangeDataset(List<DataPoint> rangeDataset) {
        dataset = rangeDataset;
        range = new DataPoint[2];
        if (rangeDataset.size() == 1) {
            range[0] = rangeDataset.get(0);
            range[1] = rangeDataset.get(0);
        } else {
            DataPoint min = new DataPoint();
            Arrays.fill(min.data, Integer.MAX_VALUE);
            DataPoint max = new DataPoint();
            Arrays.fill(max.data, Integer.MIN_VALUE);
            for (DataPoint point : dataset) {
                for (int i = 0; i < DataPoint.d; ++i) {
                    min.data[i] = Math.min(min.data[i], point.data[i]);
                    max.data[i] = Math.max(max.data[i], point.data[i]);
                }
            }
            range[0] = min;
            range[1] = max;
        }
    }
}
