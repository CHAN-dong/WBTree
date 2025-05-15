package index;

import java.util.*;

import static index.GradientDescent.gradientDescent;
import static index.GradientDescent.objectiveFunction;
import static index.IndexSP.*;

public class PartNode {

    DataPoint[] range;

//    DataPoint[] splitRange;

    HashSet<DataPoint[]> workload;

    List<DataPoint> dataList;
    private List<PartNode> chd = new ArrayList<PartNode>();
    long c; // data size
    long s; // encrypted prf size
    long cost;
    int level;
    List<Integer>[] possibleSplitPosition = new List[2];
    long[] dim_B_cost;

    WBTreeNode WBTreeNode;

    String splitPath;
    boolean needFiner = false;

    public void setFiner(boolean needFiner) {
        this.needFiner = needFiner;
    }



    public PartNode(RangeDataset dataset) {
        this.range = dataset.range;
        this.dataList = dataset.dataset;
    }

    public PartNode(DataPoint[] range, PartNode chd1, PartNode chd2) {
        this.range = range;
        chd.add(chd1);
        chd.add(chd2);
    }

    public PartNode(DataPoint[] range, PartNode chd1, PartNode chd2, PartNode chd3, PartNode chd4) {
        this.range = range;
        chd.add(chd1);
        chd.add(chd2);
        chd.add(chd3);
        chd.add(chd4);
    }

    public boolean isNeedFinerFiner() {
        return needFiner;
    }

    public long[] getDim_B_cost() {
        return dim_B_cost;
    }

    public void setDim_B_cost(long[] dim_B_cost) {
        this.dim_B_cost = dim_B_cost;
    }

    public List<PartNode> getChd() {
        return chd;
    }

    public int getChdSize() {
        return chd.size();
    }

    public boolean isLeafNode() {
        return this.chd.isEmpty();
    }

    public PartNode getChdNode(int index) {
        return chd.get(index);
    }

    public int getM() {
        return isLeafNode() ? dataList.size() : chd.size();
    }

    public void setChdNode(int index, PartNode chdNode) {
        chd.set(index, chdNode);

        this.needFiner |= chdNode.needFiner;
    }

    public void addNode(PartNode chdNode) {
        cost += addNodeCost();
        chd.add(chdNode);

        this.needFiner |= chdNode.needFiner;
    }

    public PartNode(List<DataPoint> dataList, HashSet<DataPoint[]> workload, DataPoint[] range, int level) {
        this.range = range;
        this.dataList = dataList;
        this.workload = workload;
        cost = getNodeCost();
        this.level = level;
    }

    public PartNode(List<DataPoint> dataList, DataPoint[] range) {
        this.range = range;
        this.dataList = dataList;
//        this.workload = workload;
//        cost = getNodeCost();
//        this.level = level;
    }

//    public PartNode(List<DataPoint[]> workload, DataPoint[] range, List<PartNode> chdNodes, int level, boolean needFiner) {
//        this.chd = chdNodes;
//        this.workload = workload;
//        this.range = range;
//        this.level = level;
//        this.needFiner = needFiner;
//    }

    public PartNode(PartNode refNode, List<PartNode> chdNodes, int level, boolean needFiner) {
        this.chd = chdNodes;
        this.workload = refNode.workload;
        this.range = refNode.range;
        this.splitPath = refNode.splitPath;
        this.needFiner = needFiner;
        this.c = refNode.c;
        this.cost = getNodeCost();
        this.level = level;
    }


    // create parentNode by chdNode
    public PartNode(PartNode originalChdNode, PartNode leftNode, PartNode rightNode, int level) {
        workload = originalChdNode.workload;
        this.chd = new ArrayList<>();
        this.chd.add(leftNode);
        this.chd.add(rightNode);
        this.range = originalChdNode.range;
        this.c = originalChdNode.c;
//        cost = this.getNodeCost();
        this.level = level;
        this.needFiner = true;
    }


    public long getNodeCost() {

        if (this.c == 0) {
            HashSet<Integer> stX = new HashSet<>();
            HashSet<Integer> stY = new HashSet<>();
            //get query random number size
            int c = 0;
            for (DataPoint[] range : workload) {

                c += SDC.keyGen(sk, 0, TYPE_RIGHT, range[0].data[0]).size();
                c += SDC.keyGen(sk, 0, TYPE_LEFT, range[1].data[0] + 1).size();
                c += SDC.keyGen(sk, 1, TYPE_RIGHT, range[0].data[1]).size();
                c += SDC.keyGen(sk, 1, TYPE_LEFT, range[1].data[1] + 1).size();

                stX.add(range[0].data[0] - 1);
                stX.add(range[1].data[0]);
                stY.add(range[0].data[1] - 1);
                stY.add(range[1].data[1]);

            }
            this.c = c;
            possibleSplitPosition  = new List[2];
            possibleSplitPosition[0] = new ArrayList(stX);
            possibleSplitPosition[1] = new ArrayList(stY);
            Collections.sort(possibleSplitPosition[0]);
            Collections.sort(possibleSplitPosition[1]);
        }

        if (this.s == 0) {
            int s = 0;
            if (isLeafNode()) {
                for (DataPoint point : dataList) {
                    s += SDC.encrypt(sk ,0, TYPE_LEFT, point.data[0]).size();
                    s += SDC.encrypt(sk ,0, TYPE_RIGHT, point.data[0]).size();
                    s += SDC.encrypt(sk ,1, TYPE_LEFT, point.data[1]).size();
                    s += SDC.encrypt(sk ,1, TYPE_RIGHT, point.data[1]).size();
                }
            } else {
                for (PartNode chdNode : chd) {
                    s += SDC.encrypt(sk, 0, TYPE_LEFT, chdNode.range[0].data[0]).size();
                    s += SDC.encrypt(sk, 0, TYPE_RIGHT, chdNode.range[1].data[0]).size();
                    s += SDC.encrypt(sk, 1, TYPE_LEFT, chdNode.range[0].data[1]).size();
                    s += SDC.encrypt(sk, 1, TYPE_RIGHT, chdNode.range[1].data[1]).size();
                }
            }
            this.s = s;
        }

        int m = isLeafNode() ? dataList.size() : chd.size();

        // record possible split position

        long computeCost = T1 + T2 * c + T3 * c * (int) (m * bitsetSizeRate);
        long storageCost = s * (64 + (m + 63) / 64 * 64) + 8L * m;

        this.cost = COP_W * computeCost + STO_W * storageCost;
        return this.cost;
    }


    public double addNodeCost() {
        return  c * T3 * COP_W;
    }

    public long[] getOptPoint(HashMap<Integer, Integer> M_m_l, HashMap<Integer, Integer> M_c_l, HashMap<Integer, Integer> M_c_r, int dimension, HashMap<Integer, Long> storageCostMap, int storagePos) {
        long minB = Long.MAX_VALUE, minV = Long.MAX_VALUE;
        long refComputeCost = 0;
        long disb = Long.MAX_VALUE;
        for (int b : possibleSplitPosition[dimension]) {
            long storageCost = storageCostMap.getOrDefault(b, (long)-1);
            if (storageCost == -1) {continue;};
            long McL = M_c_l.get(b);
            long McR = M_c_r.get(b);
            long MmL = M_m_l.get(b);
            long MmR = dataList.size() - MmL;

            if (McL == 0 || MmR == 0) continue;

            long computeCost = (2 * T1 + T2 * (McL + McR) + (T3) * (McL * (long)(MmL * bitsetSizeRate) + McR * (long)(MmR * bitsetSizeRate)));

            if (Math.abs(b - storagePos) < disb) {
                disb = Math.abs(b - storagePos);
                refComputeCost = computeCost;
            }

            long v = computeCost * COP_W + storageCost * STO_W;
            if (v < minV) {
                minV = v;
                minB = b;
            }
        }

        long stoPosCost = storageCostMap.get(storagePos) * STO_W + refComputeCost * COP_W;
        if (stoPosCost < minV) {
            minB = storagePos;
            minV = stoPosCost;
        }
        return new long[]{dimension, minB, minV};
    }


    public long[] getSplitPoint() {

//        if (possibleSplitPosition[0].isEmpty() && possibleSplitPosition[1].isEmpty()) {
//            return null;
//        }

        HashMap<Integer, Long> storageCost0 = new HashMap<>();

        int pos0 = getStorageCostInfo(0, storageCost0);
        HashMap<Integer, Integer> M_m_l = getDatasetLeftToRight(0);
        HashMap<Integer, Integer> M_c_l = getWorkloadLeftToRight(0);
        HashMap<Integer, Integer> M_c_r = getWorkloadRightToLeft(0);
        long[] optPoint0 = getOptPoint(M_m_l, M_c_l, M_c_r, 0, storageCost0, pos0);


        HashMap<Integer, Long> storageCost1 = new HashMap<>();
        int pos1 = getStorageCostInfo(1, storageCost1);
        M_m_l = getDatasetLeftToRight(1);
        M_c_l = getWorkloadLeftToRight(1);
        M_c_r = getWorkloadRightToLeft(1);
        long[] optPoint1 = getOptPoint(M_m_l, M_c_l, M_c_r, 1, storageCost1, pos1);

        if (optPoint0[2] >= optPoint1[2]) {
            return optPoint0;
        } else {
            return optPoint1;
        }
    }



    public double[] getSplitPointByModel() {

        if (possibleSplitPosition[0].isEmpty() && possibleSplitPosition[1].isEmpty()) {
            return null;
        }

        List<GraModel> M_m_l = getDatasetModelLeftToRight(0);
        List<GraModel> M_m_r = getDatasetModelRightToLeft(0);
        List<GraModel> M_c_l = getWorkloadModelLeftToRight(0);
        List<GraModel> M_c_r = getWorkloadModelRightToLeft(0);
        double b_x = gradientDescent(M_c_l, M_c_r, M_m_l, M_m_r, possibleSplitPosition[0].get(possibleSplitPosition[0].size() / 2), possibleSplitPosition[0]);
        double v_x = objectiveFunction(b_x, M_c_l, M_c_r, M_m_l, M_m_r);

        M_m_l = getDatasetModelLeftToRight(1);
        M_m_r = getDatasetModelRightToLeft(1);
        M_c_l = getWorkloadModelLeftToRight(1);
        M_c_r = getWorkloadModelRightToLeft(1);
        double b_y = gradientDescent(M_c_l, M_c_r, M_m_l, M_m_r, possibleSplitPosition[1].get(possibleSplitPosition[1].size() / 2), possibleSplitPosition[1]);
        double v_y = objectiveFunction(b_y, M_c_l, M_c_r, M_m_l, M_m_r);

        if (v_x < v_y) {
            return new double[]{0, Math.round(b_x), v_x};
        } else {
            return new double[]{1, Math.round(b_y), v_y};
        }
    }

    public int getStorageCostInfo(int dimension, HashMap<Integer, Long> cost) {
        dataList.sort(new Comparator<DataPoint>() {
            @Override
            public int compare(DataPoint o1, DataPoint o2) {
                return o1.data[dimension] - o2.data[dimension];
            }
        });

        int len = dataList.size();
        long[] sLtoR = new long[len];
        long[] sRtoL = new long[len];

        HashSet<String> encLtoR = new HashSet<>();
        HashSet<String> encRtoL = new HashSet<>();
        for (int i = 0; i < len; ++i) {
            DataPoint dataPoint = dataList.get(i);
            encLtoR.addAll(SDC.encrypt(sk, 0, TYPE_LEFT, dataPoint.data[0]));
            encLtoR.addAll(SDC.encrypt(sk, 0, TYPE_RIGHT, dataPoint.data[0]));
            encLtoR.addAll(SDC.encrypt(sk, 1, TYPE_LEFT, dataPoint.data[1]));
            encLtoR.addAll(SDC.encrypt(sk, 1, TYPE_RIGHT, dataPoint.data[1]));
            sLtoR[i] = encLtoR.size();
        }
        for (int i = dataList.size() - 1; i >= 0; i--) {
            DataPoint dataPoint = dataList.get(i);
            encRtoL.addAll(SDC.encrypt(sk, 0, TYPE_LEFT, dataPoint.data[0]));
            encRtoL.addAll(SDC.encrypt(sk, 0, TYPE_RIGHT, dataPoint.data[0]));
            encRtoL.addAll(SDC.encrypt(sk, 1, TYPE_LEFT, dataPoint.data[1]));
            encRtoL.addAll(SDC.encrypt(sk, 1, TYPE_RIGHT, dataPoint.data[1]));
            sRtoL[i] = encRtoL.size();
        }

        int minCostPos = 0;
        long minCost = Long.MAX_VALUE;
        int j = 0, i = 0;

        while (j < possibleSplitPosition[dimension].size() && possibleSplitPosition[dimension].get(j) < dataList.get(0).data[dimension]) {
            j++;
        }

        while (i < dataList.size()) {
            long theCost = (sLtoR[i] * (64 + (i + 64) / 64 * 64) + sRtoL[i] * (64 + (len - i + 62) / 64 * 64) + 8L * len);
            if (j < possibleSplitPosition[dimension].size() && possibleSplitPosition[dimension].get(j) < dataList.get(i).data[dimension]) {
                cost.put(possibleSplitPosition[dimension].get(j), theCost);
                j++;
            } else {
                cost.put(dataList.get(i).data[dimension], theCost);
                if (theCost < minCost) {
                    minCost = theCost;
                    minCostPos = dataList.get(i).data[dimension];
                }
                i++;
            }
        }
        return minCostPos;
    }



    public HashMap<Integer, Integer> getDatasetLeftToRight(int dimension) {
        if (dataList == null) return null;
        dataList.sort(new Comparator<DataPoint>() {
            @Override
            public int compare(DataPoint o1, DataPoint o2) {
                return o1.data[dimension] - o2.data[dimension];
            }
        });

        HashMap<Integer, Integer> mp = new HashMap<>();
        if (possibleSplitPosition[dimension] == null || possibleSplitPosition[dimension].isEmpty()) return mp;

        int j = 0;

        int p = possibleSplitPosition[dimension].get(j), c = 0;
        for (DataPoint dataPoint : dataList) {
            if (p < dataPoint.data[dimension]) {
                mp.put(p, c);
                j++;
                if (j >= possibleSplitPosition[dimension].size()) {break;}
                p = possibleSplitPosition[dimension].get(j);
            }
            c++;
        }
        mp.put(p, c);

        for (;j < possibleSplitPosition[dimension].size(); ++j) {
            mp.put(possibleSplitPosition[dimension].get(j), c);
        }

        return mp;
    }


    public List<GraModel> getDatasetModelLeftToRight(int dimension) {
        if (dataList == null) return null;
        dataList.sort(new Comparator<DataPoint>() {
            @Override
            public int compare(DataPoint o1, DataPoint o2) {
                return o1.data[dimension] - o2.data[dimension];
            }
        });


        List<int[]> pAndSize = new ArrayList<int[]>();
        int p = dataList.get(0).data[dimension], c = 0;
        for (DataPoint dataPoint : dataList) {
            if (dataPoint.data[dimension] != p) {
                pAndSize.add(new int[]{p, c});
                p = dataPoint.data[dimension];
            }
            c++;
        }
        pAndSize.add(new int[]{p, c});


        List<GraModel> graModels = GradientDescent.trainModels(pAndSize);

        return graModels;
    }

    public HashMap<Integer, Integer> getDatasetRightToLeft(int dimension) {
        if (dataList == null) return null;
        dataList.sort(new Comparator<DataPoint>() {
            @Override
            public int compare(DataPoint o1, DataPoint o2) {
                return o1.data[dimension] - o2.data[dimension];
            }
        });

        HashMap<Integer, Integer> mp = new HashMap<>();

        int p = dataList.get(dataList.size() - 1).data[dimension], c = 0, c_w = 0;
        for (int i = dataList.size() - 1; i >= 0; i--) {
            if (dataList.get(i).data[dimension] != p) {
                mp.put(p, c);
                p = dataList.get(i).data[dimension];
                c += c_w;
                c_w = 0;
            }
            c_w++;
        }
        mp.put(p, c);
        return mp;
    }


    public List<GraModel> getDatasetModelRightToLeft(int dimension) {
        if (dataList == null) return null;
        dataList.sort(new Comparator<DataPoint>() {
            @Override
            public int compare(DataPoint o1, DataPoint o2) {
                return o1.data[dimension] - o2.data[dimension];
            }
        });


        List<int[]> pAndSize = new ArrayList<int[]>();
        int p = dataList.get(dataList.size() - 1).data[dimension], c = 0, c_w = 0;
        for (int i = dataList.size() - 1; i >= 0; i--) {
            if (dataList.get(i).data[dimension] != p) {
                pAndSize.add(new int[]{p, c});
                p = dataList.get(i).data[dimension];
                c += c_w;
                c_w = 0;
            }
            c_w++;
        }
        pAndSize.add(new int[]{p, c});

        Collections.sort(pAndSize, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return Integer.compare(a[0], b[0]); // 比较数组的第一个元素
            }
        });

        List<GraModel> graModels = GradientDescent.trainModels(pAndSize);

        return graModels;
    }

    public HashMap<Integer, Integer> getWorkloadLeftToRight(int dimension) {
        int anotherDim = Math.abs(1 - dimension);
        List<DataPoint[]> workload = new ArrayList<>(this.workload);
        workload.sort(new Comparator<DataPoint[]>() {
            @Override
            public int compare(DataPoint[] o1, DataPoint[] o2) {
                return o1[0].data[dimension] - o2[0].data[dimension];
            }
        });

        HashMap<Integer, Integer> resMap = new HashMap<>();
        if (workload.isEmpty()) {
            return resMap;
        }

        int p = workload.get(0)[0].data[dimension] - 1;
        int c = 0, c_mp = 0;
        int c_w = 0, pre_c = 0;
        TreeMap<Integer, Integer> mp = new TreeMap<>();
        int i = 0;
        while (i < workload.size() || !mp.isEmpty()) {
            int stKey = mp.isEmpty() ? Integer.MAX_VALUE : mp.firstKey();
            int workKey = i >= workload.size() ? Integer.MAX_VALUE : workload.get(i)[0].data[dimension];
            int theKey;
            theKey = Math.min(workKey - 1, stKey);

            int t = SDC.getEncodingSize(theKey);
            if (theKey != p) {
                resMap.put(p, pre_c);
                c += c_w;
                c_w = 0;
                p = theKey;
                pre_c = c + c_mp + t * mp.size();
            }

            if (theKey == stKey) {
                int v = mp.pollFirstEntry().getValue();
                c_mp -= v;
                c = c + t + v;
                pre_c = c + c_mp + t * mp.size();
            } else {
                int v = SDC.keyGen(sk, anotherDim, TYPE_LEFT, workload.get(i)[1].data[anotherDim] + 1).size();
                mp.put(workload.get(i)[1].data[dimension], v);
                c_mp += v;
                c_w = c_w + SDC.keyGen(sk, dimension, TYPE_RIGHT, theKey + 1).size() + SDC.keyGen(sk, anotherDim, TYPE_RIGHT, workload.get(i)[0].data[anotherDim]).size();
                i++;
            }
        }
        resMap.put(p, pre_c);
        return resMap;
    }

    public List<GraModel> getWorkloadModelLeftToRight(int dimension) {
        int anotherDim = Math.abs(1 - dimension);
        List<DataPoint[]> workload = new ArrayList<>(this.workload);
        workload.sort(new Comparator<DataPoint[]>() {
            @Override
            public int compare(DataPoint[] o1, DataPoint[] o2) {
                return o1[0].data[dimension] - o2[0].data[dimension];
            }
        });

        List<int[]> pAndSize = new ArrayList<int[]>();
        int p = workload.get(0)[0].data[dimension] - 1;
        int c = 0, c_mp = 0;
        int c_w = 0, pre_c = 0;
        TreeMap<Integer, Integer> mp = new TreeMap<>();
        int i = 0;
        while (i < workload.size() || !mp.isEmpty()) {
            int stKey = mp.isEmpty() ? Integer.MAX_VALUE : mp.firstKey();
            int workKey = i >= workload.size() ? Integer.MAX_VALUE : workload.get(i)[0].data[dimension];
            int theKey;
            theKey = Math.min(workKey - 1, stKey);

            int t = SDC.getEncodingSize(theKey);
            if (theKey != p) {
                pAndSize.add(new int[]{p, pre_c});
                c += c_w;
                c_w = 0;
                p = theKey;
                pre_c = c + c_mp + t * mp.size();
            }

            if (theKey == stKey) {
                int v = mp.pollFirstEntry().getValue();
                c_mp -= v;
                c = c + t + v;
                pre_c = c + c_mp + t * mp.size();
            } else {
                int v = SDC.keyGen(sk, anotherDim, TYPE_LEFT, workload.get(i)[1].data[anotherDim]).size();
                mp.put(workload.get(i)[1].data[dimension], v);
                c_mp += v;
                c_w = c_w + SDC.keyGen(sk, dimension, TYPE_RIGHT, theKey + 1).size() + SDC.keyGen(sk, dimension, TYPE_RIGHT, workload.get(i)[0].data[anotherDim]).size();
                i++;
            }
        }
        pAndSize.add(new int[]{p, pre_c});

        List<GraModel> graModels = GradientDescent.trainModels(pAndSize);

        return graModels;
    }

    public HashMap<Integer, Integer> getWorkloadRightToLeft(int dimension) {
        int anotherDim = Math.abs(1 - dimension);
        List<DataPoint[]> workload = new ArrayList<>(this.workload);
        workload.sort(new Comparator<DataPoint[]>() {
            @Override
            public int compare(DataPoint[] o1, DataPoint[] o2) {
                return o1[1].data[dimension] - o2[1].data[dimension];
            }
        });

        HashMap<Integer, Integer> resMap = new HashMap<>();
        if (workload.isEmpty()) return resMap;
        int p = workload.get(workload.size() - 1)[1].data[dimension];
        int c = 0, c_mp = 0;
        int c_w = 0, pre_c = 0;
        TreeMap<Integer, Integer> mp = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });
        int i = workload.size() - 1;
        while (i >= 0 || !mp.isEmpty()) {
            int stKey = mp.isEmpty() ? (Integer.MIN_VALUE + 1) : mp.firstKey();
            int workKey = i < 0 ? Integer.MIN_VALUE : workload.get(i)[1].data[dimension];
            int theKey = Math.max(workKey, stKey - 1);

            int t = SDC.getEncodingSize(theKey);
            int t_ = SDC.getEncodingSize(theKey + 1);
            if (theKey != p) {
                resMap.put(p, pre_c);
                c += c_w;
                c_w = 0;
                p = theKey;
                pre_c = c + c_mp + t_ * mp.size();
            }

            if (theKey == stKey - 1) {
                int v = mp.pollFirstEntry().getValue();
                c_mp -= v;
                c = c + t_ + v;
                pre_c = c + c_mp + t_ * mp.size();
            } else {
                int v = SDC.getEncodingSize(workload.get(i)[0].data[anotherDim]);
                mp.put(workload.get(i)[0].data[dimension], v);
                c_mp += v;
                c_w = c_w + t + SDC.getEncodingSize(workload.get(i)[1].data[anotherDim]);
                i--;
            }
        }
        resMap.put(p, pre_c);
        return resMap;
    }

    public List<GraModel> getWorkloadModelRightToLeft(int dimension) {
        int anotherDim = Math.abs(1 - dimension);
        List<DataPoint[]> workload = new ArrayList<>(this.workload);
        workload.sort(new Comparator<DataPoint[]>() {
            @Override
            public int compare(DataPoint[] o1, DataPoint[] o2) {
                return o1[1].data[dimension] - o2[1].data[dimension];
            }
        });

        List<int[]> pAndSize = new ArrayList<int[]>();
        int p = workload.get(workload.size() - 1)[1].data[dimension];
        int c = 0, c_mp = 0;
        int c_w = 0, pre_c = 0;
        TreeMap<Integer, Integer> mp = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });
        int i = workload.size() - 1;
        while (i >= 0 || !mp.isEmpty()) {
            int stKey = mp.isEmpty() ? (Integer.MIN_VALUE + 1) : mp.firstKey();
            int workKey = i < 0 ? Integer.MIN_VALUE : workload.get(i)[1].data[dimension];
            int theKey = Math.max(workKey, stKey - 1);

            int t = SDC.getEncodingSize(theKey);
            int t_ = SDC.getEncodingSize(theKey + 1);
            if (theKey != p) {
                pAndSize.add(new int[]{p, pre_c});
                c += c_w;
                c_w = 0;
                p = theKey;
                pre_c = c + c_mp + t_ * mp.size();
            }

            if (theKey == stKey - 1) {
                int v = mp.pollFirstEntry().getValue();
                c_mp -= v;
                c = c + t_ + v;
                pre_c = c + c_mp + t_ * mp.size();
            } else {
                int v = SDC.getEncodingSize(workload.get(i)[0].data[anotherDim]);
                mp.put(workload.get(i)[0].data[dimension], v);
                c_mp += v;
                c_w = c_w + t + SDC.getEncodingSize(workload.get(i)[1].data[anotherDim]);
                i--;
            }
        }
        pAndSize.add(new int[]{p, pre_c});


        Collections.sort(pAndSize, new Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return Integer.compare(a[0], b[0]); // 比较数组的第一个元素
            }
        });
        List<GraModel> graModels = GradientDescent.trainModels(pAndSize);

        return graModels;
    }

    public String getPath() {
        return splitPath;
    }

    public void setPath(String path) {
        this.splitPath = path;
    }
}
