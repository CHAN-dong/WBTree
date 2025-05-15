package index;

import java.util.HashSet;
import java.util.List;

import static dataset.ReadDataset.readDataset;
import static dataset.ReadDataset.readWorkload;

public class Test {


    public static void main(String[] args) {
        long s,e;
        List<DataPoint> dataset = readDataset("./src/dataset/OSM_test");

        RangeDataset rangeDataset = new RangeDataset(dataset);
        HashSet<DataPoint[]> workload = readWorkload("./src/dataset/LAP_Workload_OSM_test_R0.2%", 800);

        IndexSP.setCostRate(32,1);


//        IndexSP.T3 = t3;
        s = System.currentTimeMillis();
        IndexSP indexSP = new IndexSP(rangeDataset, workload, IndexSP.IndexBuildType.WBPRTree);
        e = System.currentTimeMillis();
        System.out.println("build time:" + (e - s) / 1000.0 + "s");
        indexSP.getIndexSize();
        indexSP.buildPathTree();

        indexSP.workloadQueryTest();

    }


}
