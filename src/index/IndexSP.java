package index;

import dataset.DatasetVisualizer;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;

import java.util.*;

import static dataset.ReadDataset.readDataset;
import static dataset.ReadDataset.readWorkload;
import static index.WBTreeNode.*;

public class IndexSP {

    public static long STO_W = 1;
    public static long COP_W = 1;

    public static String sk = "123";

    static double learningRate = 0.01;
    static int maxIterations = 100;


    public static long T1 = 0;
    public static long T2 = 800;
    public static long T3 = 5;
//    public static long T2 = 1;
//    public static long T3 = 1;

    public static double bitsetSizeRate = 1.3;

    HashSet<DataPoint[]> partitions = new HashSet<>();

    public enum IndexBuildType {
        BPRIndex, WBPRTree, QuadTree, KDTree
    }


    static final String TYPE_LEFT = "LEFT";
    static final String TYPE_RIGHT = "RIGHT";
    static final String PATH_ROOT = "";

    PartNode partTreeRoot;
    public WBTreeNode root;
    PathTreeNode pathTreeRoot;

    Queue<DataPoint[]> workloadQue = new LinkedList<>();
    int workloadQueSize;


    public static void setUpdatePar(double bitsetSizeRate, double errRate) {
        IndexSP.bitsetSizeRate = bitsetSizeRate;
        PathTreeNode.errRate = errRate;
    }


    public IndexSP(RangeDataset dataset, HashSet<DataPoint[]> workload, IndexBuildType type) {
        workloadQueSize = workload.size();
        switch (type) {
            case BPRIndex: oneLevelTreeBuild(dataset, workload); break;
            case WBPRTree:
                List<HashMap<String, PartNode>> splitPathList = new ArrayList<>();
                partTreeRoot = indexBuild(dataset, workload, splitPathList);
                this.splitPathList = splitPathList.get(0);
                break;
            case QuadTree: quadTreeBuild(dataset); break;
            case KDTree: kdTreeBuild(dataset); break;
        }

//        DatasetVisualizer.draw(workload, dataset.dataset, partitions);

        root = encryptIndex(partTreeRoot, PATH_ROOT);
        workloadQue.addAll(workload);
    }



    public static void setCostRate(long storage, long compute) {
        STO_W = storage;
        COP_W = compute;
    }

    HashMap<String, PartNode> splitPathList;
    public void buildPathTree() {
        pathTreeRoot = buildPathTree(partTreeRoot);
    }

    public void getIndexSize() {
        System.setProperty("java.vm.name", "Java HotSpot(TM) ");
        long sz = ObjectSizeCalculator.getObjectSize(root);
        System.out.println("index size:" + sz / 1024.0 / 1024.0 / 1024 + "GB");
    }


    public List<Integer> getDataset(WBTreeNode node) {
        List<Integer> dataset = new ArrayList<>();
        if (node.isLeafNode()) {
            dataset.addAll(node.ids);
        } else {
            if (node.chd != null) {
                for (WBTreeNode child : node.chd) {
                    if (child != null) {
                        dataset.addAll(getDataset(child));
                    }
                }
            }
        }
        return dataset;
    }


    public void workloadQueryTest() {
        ArrayList<DataPoint[]> queries = new ArrayList<>(workloadQue);
        int queryLen = queries.size();
        Token[] tokens = new Token[queryLen];
        int j = 0;
        for (DataPoint[] dataPoints : queries) {
            tokens[j++] = tokenGeneration(dataPoints);
        }

        // warm up
        for (Token token : tokens) {
            this.query(token);
        }

        long s = System.currentTimeMillis();
        for (Token token : tokens) {
            this.query(token);
        }
        long e = System.currentTimeMillis();
        System.out.println("query time: " + (double)(e - s) / queryLen + "ms" );
    }


    private PathTreeNode buildPathTree(PartNode partTreeNode) {
        PathTreeNode pathTreeNode = new PathTreeNode();

        // build path tree by Part Tree
        addPartTreeToPathTree(pathTreeNode, partTreeNode, IndexSP.PATH_ROOT);

        // set path tree info
        setPathTreeCost(pathTreeNode);

        return pathTreeNode;
    }


    public long rebuildTrees(PathTreeNode pathTreeNode, PathTreeNode rebuildParentNode, DataPoint addData, DataPoint[] query, boolean isAdd) {

        long preCost = pathTreeNode.nowCost;

        List<DataPoint> dataPoints = new ArrayList<>();
        HashSet<DataPoint[]> workload = new HashSet<>();
        List<Integer> chdIndex = new ArrayList<>();
        List<DataPoint[]> chdRange = new ArrayList<>();
        pathTreeNode.getSubTreeDataAndQuery(rebuildParentNode, dataPoints, workload, chdIndex, chdRange);

        if (addData != null) dataPoints.add(addData);
        if (query != null) {
            if (isAdd) workload.add(query);
            else workload.remove(query);
        }

        List<HashMap<String, PartNode>> splitPathList = new ArrayList<>();
        PartNode newNode = indexBuild(new RangeDataset(dataPoints), workload, splitPathList);

//        System.out.println("workload size:" + workload.size() + ", data size: " + dataPoints.size());

        long parentDeltaCost = 0; // the cost change of parent node
        if (rebuildParentNode == null) { // rebuild overall tree
            this.partTreeRoot = newNode;
            root = encryptIndex(partTreeRoot, PATH_ROOT);
            buildPathTree();
            return pathTreeRoot.nowCost - preCost;
        }

        // delete chd range
        for (int i = 0; i < chdIndex.size(); i++) {

            parentDeltaCost += rebuildParentNode.WBTreeNode.flipRange(chdRange.get(i), chdIndex.get(i), rebuildParentNode.encPath);
            rebuildParentNode.WBTreeNode.chd.set(chdIndex.get(i), null);
        }

        if (newNode.getChdSize() == 0) {

            rebuildParentNode.WBTreeNode.chd.set(chdIndex.get(0), encryptIndex(newNode, rebuildParentNode.encPath + chdIndex.get(0)));
            parentDeltaCost += rebuildParentNode.WBTreeNode.flipRange(chdRange.get(0), chdIndex.get(0), rebuildParentNode.encPath);
            PathTreeNode chdPathNode = addPartTreeToPathTree(pathTreeNode, newNode, rebuildParentNode.encPath + chdIndex.get(0));
            chdPathNode.parentNode = rebuildParentNode;
        } else {
            int i = 0;
            for (int j = 0; j < newNode.getChdSize(); ++j) {
                if (i < chdIndex.size()) {
                    rebuildParentNode.WBTreeNode.chd.set(chdIndex.get(i), encryptIndex(newNode.getChdNode(j), rebuildParentNode.encPath + chdIndex.get(i)));
                    parentDeltaCost += rebuildParentNode.WBTreeNode.flipRange(newNode.getChdNode(j).range, chdIndex.get(i), rebuildParentNode.encPath);

                    PathTreeNode chdPathNode = addPartTreeToPathTree(pathTreeNode, newNode.getChdNode(j), rebuildParentNode.encPath + chdIndex.get(i));
                    chdPathNode.parentNode = rebuildParentNode;
                    i++;
                } else {
                    int idx = rebuildParentNode.WBTreeNode.chd.size();
                    rebuildParentNode.WBTreeNode.chd.add(encryptIndex(newNode.getChdNode(j), rebuildParentNode.encPath + idx));
                    parentDeltaCost += rebuildParentNode.WBTreeNode.flipRange(newNode.getChdNode(j).range, idx, rebuildParentNode.encPath);

                    PathTreeNode chdPathNode = addPartTreeToPathTree(pathTreeNode, newNode.getChdNode(j), rebuildParentNode.encPath + idx);
                    chdPathNode.parentNode = rebuildParentNode;

                }
            }
        }

        if (rebuildParentNode.WBTreeNode.isFull()) {
            rebuildParentNode.extend();
        } else {
            rebuildParentNode.buffDeltaCost = parentDeltaCost;
        }

        setPathTreeCost(pathTreeNode);

//        if (pathTreeNode.leftChd == null && pathTreeNode.rightChd == null && pathTreeNode.dataList == null) {
//            int a = 1;
//        }


        return pathTreeNode.nowCost - preCost;
    }

    public void addData(DataPoint data) {
        addData(data, pathTreeRoot, null);
    }



    public long addData(DataPoint data, PathTreeNode pathTreeNode, PathTreeNode rebuildParentNode) {

        if (pathTreeNode.needRebuild) {
            pathTreeNode.needRebuild = false;
            // rebuild
            return rebuildTrees(pathTreeNode, rebuildParentNode, data, null, false);
        }

        long deltaCost;
        if (pathTreeNode.leftChd != null && pathTreeNode.rightChd != null) {

            if (pathTreeNode.leftChd.range[0].data[0] <= data.data[0] && pathTreeNode.leftChd.range[0].data[1] <= data.data[1] && pathTreeNode.leftChd.range[1].data[0] >= data.data[0] && pathTreeNode.leftChd.range[1].data[1] >= data.data[1]) {
                deltaCost = addData(data, pathTreeNode.leftChd, pathTreeNode.WBTreeNode != null ? pathTreeNode : rebuildParentNode);
            } else {
                deltaCost = addData(data, pathTreeNode.rightChd, pathTreeNode.WBTreeNode != null ? pathTreeNode : rebuildParentNode);
            }
            pathTreeNode.checkRange();

        } else {
            // leaf node
            deltaCost = pathTreeNode.WBTreeNode.addData(data, pathTreeNode.encPath);
            pathTreeNode.addData(data);
            if (pathTreeNode.WBTreeNode.isFull()) {
                deltaCost = pathTreeNode.extend();
            }
        }

        // update parent range index
        if (pathTreeNode.newRange != null && pathTreeNode.WBTreeNode != null && rebuildParentNode != null) {
            rebuildParentNode.buffDeltaCost = rebuildParentNode.WBTreeNode.updateSubRangeByFlip(pathTreeNode.range, pathTreeNode.newRange, pathTreeNode.getTheBelongChildIndex(), rebuildParentNode.encPath);
            pathTreeNode.range = pathTreeNode.newRange;
        }

        return pathTreeNode.addCost(deltaCost);
    }


//
//    public void addData(DataPoint addData) {
//
//        pathTreeRoot.newRange = null;
//
//
//        PathTreeNode preParentNode = pathTreeRoot;
//        PathTreeNode node = pathTreeRoot;
//        while (!node.isLeafNode()) {
//            if (node.leftChd.range[0].data[0] <= addData.data[0] && node.leftChd.range[0].data[1] <= addData.data[1] && node.leftChd.range[1].data[0] >= addData.data[0] && node.leftChd.range[1].data[1] >= addData.data[1]) {
//
//                if (node.needRebuild()) {
//                    rebuildTrees(node, preParentNode);
//                    return;
//                }
//                if (node.isEncInterNode()) {
//                    preParentNode = node;
//                }
//                node = node.leftChd;
//            } else {
//                if (node.needRebuild()) {
//                    rebuildTrees(node, preParentNode);
//                    return;
//                }
//                if (node.isEncInterNode()) {
//                    preParentNode = node;
//                }
//                node = node.rightChd;
//            }
//        }
//        node.addData(addData);
//        node.spTreeNode.addData(addData, node.encPath);
//
//    }

    public void updateWorkload(DataPoint[] query) {

        int c = SDC.getEncodingSize(query[0].data[0]) + SDC.getEncodingSize(query[0].data[1]) + SDC.getEncodingSize(query[1].data[0]) + SDC.getEncodingSize(query[1].data[1]);

        updateWorkload(query, c, pathTreeRoot, true, null);
        workloadQue.add(query);
        if (workloadQue.size() > workloadQueSize) {
            query = workloadQue.poll();
            c = SDC.getEncodingSize(query[0].data[0]) + SDC.getEncodingSize(query[0].data[1]) + SDC.getEncodingSize(query[1].data[0]) + SDC.getEncodingSize(query[1].data[1]);
            updateWorkload(query, c, pathTreeRoot, false, null);
        }
    }

    private long updateWorkload(DataPoint[] query, int c, PathTreeNode pathTreeNode, boolean isAdd, PathTreeNode rebuildParentNode) {
        long deltaCost = 0;

        if (pathTreeNode == null || pathTreeNode.range == null) {
            return 0;
        }

        if (pathTreeNode.range[0].data[0] <= query[1].data[0] && query[0].data[0] <= pathTreeNode.range[1].data[0] && pathTreeNode.range[0].data[1] <= query[1].data[1] && query[0].data[1] <= pathTreeNode.range[1].data[1]) {

            if (pathTreeNode.needRebuild) {
                pathTreeNode.needRebuild = false;
                return rebuildTrees(pathTreeNode, rebuildParentNode, null, query, isAdd);
            }

            if (pathTreeNode.isLeafNode()) {
                if (isAdd) {
                    if (pathTreeNode.workload == null) pathTreeNode.workload = new HashSet<>();
                    pathTreeNode.workload.add(query);
                } else {
                    pathTreeNode.workload.remove(query);
                }
            } else {
                deltaCost += updateWorkload(query, c, pathTreeNode.leftChd, isAdd, pathTreeNode.WBTreeNode != null ? pathTreeNode : rebuildParentNode);
                deltaCost += updateWorkload(query, c, pathTreeNode.rightChd, isAdd,  pathTreeNode.WBTreeNode != null ? pathTreeNode : rebuildParentNode);
            }
            deltaCost += pathTreeNode.updC(c, isAdd);

            return deltaCost;

        }
        return deltaCost;
    }

    private long setPathTreeCost(PathTreeNode pathTreeNode) {

        long cst = 0;
        //child cost
        if (pathTreeNode.leftChd != null) {
            cst += setPathTreeCost(pathTreeNode.leftChd);
        }
        if (pathTreeNode.rightChd != null) {
            cst += setPathTreeCost(pathTreeNode.rightChd);
        }
        if (pathTreeNode.WBTreeNode != null) {
            // add current node cost
            cst += pathTreeNode.WBTreeNode.getCost(pathTreeNode.c);
        }
        pathTreeNode.setCost(cst);
        pathTreeNode.setRange();
        return cst;
    }



//    private void setPathTreeCost(PathTreeNode pathTreeRoot, HashMap<String, PartNode> splitPathList) {
//
//        for (Map.Entry<String, PartNode> entry : splitPathList.entrySet()) {
//            String path = entry.getKey();
//            PartNode partNode = entry.getValue();
//
//            PathTreeNode node = pathTreeRoot;
//            for (int i = 0; i < path.length(); i++) {
//                if (path.charAt(i) == '0') {
//                    node = node.leftChd;
//                } else {
//                    node = node.rightChd;
//                }
//            }
//            node.initCost(partNode.cost, partNode.c, partNode.getM());
//            node.setRange(partNode.range);
//        }
//
//    }


//    private void travelPathTree(PathTreeNode pathTreeNode) {
//
//        if (pathTreeNode.isLeafNode()) {
//            pathTreeNode.dataSize = pathTreeNode.dataList.size();
//            pathTreeNode.querySize = pathTreeNode.workload.size();
//        } else {
//            travelPathTree(pathTreeNode.leftChd);
//            travelPathTree(pathTreeNode.rightChd);
//
//            pathTreeNode.dataSize = pathTreeNode.leftChd.dataSize + pathTreeNode.rightChd.dataSize;
//            pathTreeNode.querySize = pathTreeNode.leftChd.querySize + pathTreeNode.rightChd.querySize;
//            pathTreeNode.querySizeRate = (double) pathTreeNode.leftChd.querySize / pathTreeNode.rightChd.querySize;
//            pathTreeNode.dataSizeRate = (double) pathTreeNode.leftChd.dataSize / pathTreeNode.rightChd.dataSize;
//        }
//    }

    private PathTreeNode addPartTreeToPathTree(PathTreeNode pathTreeNode, PartNode partNode, String encPath) {
        if (partNode == null) {return null;}
        PathTreeNode buildPathNode = addPartNodeToPathTree(partNode, pathTreeNode, 0, encPath);

        for (int i = 0; i < partNode.getChdSize(); ++i) {
            PathTreeNode chdPathNode = addPartTreeToPathTree(pathTreeNode, partNode.getChdNode(i), encPath + i);
            chdPathNode.parentNode = buildPathNode;

            if (chdPathNode.getTheBelongChildIndex() >= chdPathNode.parentNode.WBTreeNode.chd.size()) {
                int a = 1;
            }

        }

        return buildPathNode;
    }

    private PathTreeNode addPartNodeToPathTree(PartNode partNode, PathTreeNode pathTreeNode, int index, String encPath) {

//        if (partNode.splitPath == null) return null;

        if (partNode.splitPath.length() == index) {
            pathTreeNode.init(partNode, encPath);
            return pathTreeNode;
        } else {
            if (partNode.splitPath.charAt(index) == '0') {
                if (pathTreeNode.leftChd == null) {
                    pathTreeNode.leftChd = new PathTreeNode();
                }
                return addPartNodeToPathTree(partNode, pathTreeNode.leftChd, index + 1, encPath);
            } else if (partNode.splitPath.charAt(index) == '1') {
                if (pathTreeNode.rightChd == null) {
                    pathTreeNode.rightChd = new PathTreeNode();
                }
                return addPartNodeToPathTree(partNode, pathTreeNode.rightChd, index + 1, encPath);
            }
        }
        return pathTreeNode;
    }


    public void quadTreeBuild(RangeDataset dataset) {
        partTreeRoot = new PartNode(dataset);
    }

    public PartNode quadRecursiveTreeBuild(RangeDataset dataset) {
        List<RangeDataset> subDatasets = quadPartition(dataset.dataset, dataset.range);
        if (subDatasets == null) {
            return new PartNode(dataset);
        } else {

            PartNode partNode0 = quadRecursiveTreeBuild(subDatasets.get(0));
            PartNode partNode1 = quadRecursiveTreeBuild(subDatasets.get(1));
            PartNode partNode2 = quadRecursiveTreeBuild(subDatasets.get(2));
            PartNode partNode3 = quadRecursiveTreeBuild(subDatasets.get(3));

            return new PartNode(dataset.range, partNode0, partNode1, partNode2, partNode3);
        }
    }

    public static List<RangeDataset> quadPartition(List<DataPoint> dataset, DataPoint[] range) {
        if (dataset == null || dataset.size() <= QUAD_M) {
            return null;
        }
        // 确定原始范围的边界
        int minX = range[0].data[0];
        int maxX = range[1].data[0];
        int minY = range[0].data[1];
        int maxY = range[1].data[1];

        // 计算中间点
        int midX = (minX + maxX) / 2;
        int midY = (minY + maxY) / 2;

        // 初始化四个子数据集和子范围
        List<DataPoint> lowerLeftDataset = new ArrayList<>();
        List<DataPoint> lowerRightDataset = new ArrayList<>();
        List<DataPoint> upperLeftDataset = new ArrayList<>();
        List<DataPoint> upperRightDataset = new ArrayList<>();

        DataPoint[] lowerLeftRange = new DataPoint[]{new DataPoint(new int[]{minX, minY}), new DataPoint(new int[]{midX, midY})};
        DataPoint[] lowerRightRange = new DataPoint[]{new DataPoint(new int[]{midX + 1, minY}), new DataPoint(new int[]{maxX, midY})};
        DataPoint[] upperLeftRange = new DataPoint[]{new DataPoint(new int[]{minX, midY + 1}), new DataPoint(new int[]{midX, maxY})};
        DataPoint[] upperRightRange = new DataPoint[]{new DataPoint(new int[]{midX + 1, midY + 1}), new DataPoint(new int[]{maxX, maxY})};

        // 分配数据点到相应的子数据集
        for (DataPoint point : dataset) {
            int x = point.data[0];
            int y = point.data[1];

            if (x <= midX) {
                if (y <= midY) {
                    lowerLeftDataset.add(point);
                } else {
                    upperLeftDataset.add(point);
                }
            } else {
                if (y <= midY) {
                    lowerRightDataset.add(point);
                } else {
                    upperRightDataset.add(point);
                }
            }
        }

        // 构建结果列表
        List<RangeDataset> results = new ArrayList<>();
        results.add(new RangeDataset(lowerLeftDataset, lowerLeftRange));
        results.add(new RangeDataset(lowerRightDataset, lowerRightRange));
        results.add(new RangeDataset(upperLeftDataset, upperLeftRange));
        results.add(new RangeDataset(upperRightDataset, upperRightRange));

        return results;
    }



    static int QUAD_M = 1000;
    public void oneLevelTreeBuild(RangeDataset dataset, HashSet<DataPoint[]> workloads) {
        partTreeRoot = new PartNode(dataset.dataset, dataset.range);
    }


    public WBTreeNode encryptIndex(PartNode partNode, String path) {

        WBTreeNode psfTreeNode = new WBTreeNode();

        if (partNode.isLeafNode()) {
            psfTreeNode.setIds(partNode);
        } else {
            psfTreeNode.chd = new ArrayList<>(partNode.getChdSize());
            for (int i = 0; i < partNode.getChdSize(); ++i) {
                psfTreeNode.chd.add(encryptIndex(partNode.getChdNode(i), path + i));
            }
        }

        psfTreeNode.encryptToBitset(partNode, path);

        return psfTreeNode;
    }


    public List<Integer> query(DataPoint[] query) {
        ArrayList<Integer> res = new ArrayList<>();
        query(partTreeRoot, query, res);
        return res;
    }

    private void query(PartNode node, DataPoint[] query, List<Integer> res) {
        if (node.isLeafNode()) {

            for (DataPoint point : node.dataList) {
                if (point.data[0] >= query[0].data[0] && point.data[0] <= query[1].data[0] && point.data[1] >= query[0].data[1] && point.data[1] <= query[1].data[1]) {
                    res.add(point.id);
                }
            }

        } else {

            for (PartNode chd : node.getChd()) {
                if (chd.range[0].data[0] <= query[1].data[0] && query[0].data[0] <= chd.range[1].data[0] && chd.range[0].data[1] <= query[1].data[1] && query[0].data[1] <= chd.range[1].data[1]) {
                    query(chd, query, res);
                }
            }

        }
    }


    public List<Integer> query(Token token) {
        List<Integer> res = new ArrayList<>();

        long s = System.nanoTime();
        query(token, res);
        long e = System.nanoTime();
        all_time += e - s;
        return res;
    }


    public void printAvgParameter() {

        System.out.println("Every node time list info: ");
        System.out.println("T1List:");
        for (int i = 0; i < tokensizeList.size(); ++i) {
            System.out.printf(lenSizeList.get(i) + "-" + tokensizeList.get(i) + "-" + T1List.get(i) + ", ");
        }
        System.out.println();
        System.out.println("T2List:");
        long t2 = 0;
        for (int i = 0; i < tokensizeList.size(); ++i) {
            t2 += T2List.get(i);
            System.out.printf(lenSizeList.get(i) + "-" + tokensizeList.get(i) + "-" + T2List.get(i) + ", ");
        }
        System.out.println();

        System.out.printf("Avg T2: " + (t2) / tokensizeList.size() + "ns");

        System.out.println("all_time: " + all_time + "ns, part1_time: " + part1_time + "ns, part2_time: " + part2_time + "ns, node_count: " + node_count + ", c_sum: " + c_sum + ", m_sum: " + m_sum);
        System.out.println("avg T0: " + (all_time - part1_time - part2_time) / node_count + ", avg T1: " +  part1_time / c_sum);

//        System.out.println("T3List:");
//        for (int i = 0; i < tokensizeList.size(); ++i) {
//            System.out.printf(lenSizeList.get(i) + "-" + T3List.get(i) + ", ");
//        }
//        System.out.println();

        part1_time = 0;
        part2_time = 0;
        c_sum = 0;
        m_sum = 0;
        node_count = 0;
        T1List = new ArrayList<>();
        T2List = new ArrayList<>();
        tokensizeList = new ArrayList<>();
        lenSizeList = new ArrayList<>();
    }


    long part1_time = 0;
    long part2_time = 0;
    int c_sum = 0;
    int m_sum = 0;
    long all_time = 0;
    int node_count = 0;
    List<Long> T2List = new ArrayList<>();
    List<Long> T1List = new ArrayList<>();

    List<Integer> tokensizeList = new ArrayList<>();
    List<Integer> lenSizeList = new ArrayList<>();

    public void query(Token token, List<Integer> res) {

        Queue<WBTreeNode> queue = new LinkedList<>();
        Queue<String> pathQueue = new LinkedList<>();
        queue.add(root);
        pathQueue.add(PATH_ROOT);
        
        while (!queue.isEmpty()) {
            WBTreeNode queryNode = queue.poll();
            String path = pathQueue.poll();

            long s0 = System.nanoTime();
            // processing of token attach path
            int[][] leftToken = new int[2][];
            int[][] rightToken = new int[2][];
            byte[][][] leftKey = new byte[2][][];
            byte[][][] rightKey = new byte[2][][];

            for (int i = 0; i < 2; i++) {
                leftToken[i] = new int[token.left[i].size()];
                rightToken[i] = new int[token.right[i].size()];

                leftKey[i] = new byte[token.left[i].size()][];
                rightKey[i] = new byte[token.right[i].size()][];

                for (int j = 0; j < token.left[i].size(); ++j) {
                    leftToken[i][j] = attachPathToKey(token.left[i].get(j), path);
                    leftKey[i][j] = getDecKey(token.leftKey[i].get(j), path);
                }
                for (int j = 0; j < token.right[i].size(); ++j) {
                    rightToken[i][j] = attachPathToKey(token.right[i].get(j), path);
                    rightKey[i][j] = getDecKey(token.rightKey[i].get(j), path);
                }
            }
            long e0 = System.nanoTime();

            long s1 = System.nanoTime();
            //find in node
            int len;

            boolean isLeafNode = queryNode.isLeafNode();
            if (isLeafNode) len = queryNode.ids.size();
            else len = queryNode.chd.size();

            BitSet left0 = new BitSet(len);
            BitSet left1 = new BitSet(len);
            BitSet right0 = new BitSet(len);
            BitSet right1 = new BitSet(len);
            for (int j = 0; j < token.left[0].size(); ++j) {
                left0.or(queryNode.decToken(leftToken[0][j], leftKey[0][j]));
            }
            for (int j = 0; j < token.left[1].size(); ++j) {
                left1.or(queryNode.decToken(leftToken[1][j], leftKey[1][j]));
            }
            for (int j = 0; j < token.right[0].size(); ++j) {
                right0.or(queryNode.decToken(rightToken[0][j], rightKey[0][j]));
            }
            for (int j = 0; j < token.right[1].size(); ++j) {
                right1.or(queryNode.decToken(rightToken[1][j], rightKey[1][j]));
            }

            BitSet resBitset = right0;
            resBitset.and(right1);
            resBitset.andNot(left0);
            resBitset.andNot(left1);

            if (isLeafNode) {
                for (int i = 0; i < len; ++i) {
                    if (resBitset.get(i)) {
                        res.add(queryNode.ids.get(i));
                    }
                }
            } else {
                for (int i = 0; i < len; ++i) {
                    if (resBitset.get(i)) {
                        queue.add(queryNode.chd.get(i));
                        pathQueue.add(path + i);
                    }
                }
            }
            long e1 = System.nanoTime();

//            int tokensize = (leftToken[0].length + leftToken[1].length + rightToken[0].length + rightToken[1].length);
//            tokensizeList.add(tokensize);
//            lenSizeList.add(len);
//            part1_time += e0 - s0;
//            part2_time += (e1 - s1);
//            c_sum += tokensize;
//            m_sum += len;
//            node_count++;
//            T1List.add((e0 - s0) / tokensize);
//            T2List.add((e1 - s1) / len / tokensize);
        }

    }


    private PartNode indexBuild(RangeDataset dataset, HashSet<DataPoint[]> workload, List<HashMap<String, PartNode>> splitPathList) {

        PartNode node = buildPartTree(dataset, workload, splitPathList);

        while (node.needFiner) {
            node = finerSplit(node, splitPathList);
        }

        return node;
    }


    public PartNode buildPartTree(RangeDataset dataset, HashSet<DataPoint[]> workload, List<HashMap<String, PartNode>> splitPathList) {

        // tree construct
        PartNode initialNode = new PartNode(dataset.dataset, workload, dataset.range,0);
        splitPathList.add(new HashMap<String, PartNode>());
        PartNode node = leafNodeRecursiveSplit(null, -1, initialNode, splitPathList.get(0));
        if (node == null) {
            return initialNode;
        }

        Queue<PartNode> parentNodeQueue = new LinkedList<>();
        while (true) {
            if (splitPathList.size() <= node.level) {
                splitPathList.add(new HashMap());
            }
            node.getNodeCost();
            PartNode newParentNode = nonLeafNodeRecursiveSplit(null, -1, node, splitPathList.get(node.level - 1), splitPathList.get(node.level));
            if (newParentNode == null) {
                return node;
            } else {
                parentNodeQueue.add(newParentNode);
                node = newParentNode;
            }
        }

    }


    private PartNode finerSplit(PartNode node, List<HashMap<String, PartNode>> splitPathList) {
        recursiveFinerSplit(node, splitPathList);

        Queue<PartNode> parentNodeQueue = new LinkedList<>();
        while (true) {
            splitPathList.add(new HashMap());
            PartNode newParentNode = nonLeafNodeRecursiveSplit(null, -1, node, splitPathList.get(node.level - 1), splitPathList.get(node.level));
            if (newParentNode == null) {
                return node;
            } else {
                parentNodeQueue.add(newParentNode);
                node = newParentNode;
            }
        }
    }

    public static Token tokenGeneration(DataPoint[] query) {
        List<String>[] left = new List[2];
        List<String>[] right = new List[2];
        List<String>[] leftKey = new List[2];
        List<String>[] rightKey = new List[2];
        for (int i = 0; i < 2; i++) {
            left[i] = SDC.keyGen(sk, i, TYPE_RIGHT, query[0].data[i]);
            right[i] = SDC.keyGen(sk, i, TYPE_LEFT,query[1].data[i] + 1);
        }

        leftKey[0] = SDC.tokenKeyGen(sk, left[0]);
        rightKey[0] = SDC.tokenKeyGen(sk, right[0]);
        leftKey[1] = SDC.tokenKeyGen(sk, left[1]);
        rightKey[1] = SDC.tokenKeyGen(sk, right[1]);

        return new Token(left, right, leftKey, rightKey);
    }


    private void recursiveFinerSplit(PartNode node, List<HashMap<String, PartNode>> splitPathList) {
        node.needFiner = false;
        int chdSizes = node.getChdSize();
        for (int i = 0; i < chdSizes; ++i) {
            PartNode theChd = node.getChdNode(i);
            if (theChd.isNeedFinerFiner()) {
                recursiveFinerSplit(theChd, splitPathList);
                node.needFiner |= theChd.needFiner;
            }
            if (theChd.isLeafNode()) {
                leafNodeRecursiveSplit(node, i, theChd, splitPathList.get(0));
            } else {
                nonLeafNodeRecursiveSplit(node, i, theChd, splitPathList.get(theChd.level - 1), splitPathList.get(theChd.level));
            }
        }
    }

    private PartNode nonLeafNodeRecursiveSplit(PartNode parentNode, int index, PartNode node, HashMap<String, PartNode> prePath, HashMap<String, PartNode> newPath) {
        long[] dim_B_cost;
        Queue<Integer> que = new ArrayDeque<>();

        if (parentNode == null) {
            dim_B_cost = prePath.get("").getDim_B_cost();
            node.setPath("");

            parentNode = tryNonLeafNodeSplit(null, -1, node, dim_B_cost, prePath.get("0"), prePath.get("1"));
            if (parentNode == null) {
                return null;
            } else {
                node.setDim_B_cost(dim_B_cost);
                newPath.put("", node);
            }

            que.add(0);
            que.add(1);
            parentNode.getChdNode(0).setPath("0");
            parentNode.getChdNode(1).setPath("1");
        } else {
            que.add(index);
        }

        while (!que.isEmpty()) {
            index = que.poll();
            PartNode tryChdNode = parentNode.getChdNode(index);
            String path = tryChdNode.getPath();

            dim_B_cost = prePath.get(path).getDim_B_cost();
            PartNode newParentNode = tryNonLeafNodeSplit(parentNode, index, tryChdNode, dim_B_cost, prePath.get(path + "0"), prePath.get(path + "1"));

            if (newParentNode != null) {
                parentNode = newParentNode;

                tryChdNode.setDim_B_cost(dim_B_cost);
                newPath.put(path, tryChdNode);

                que.add(index);
                que.add(parentNode.getChdSize() - 1);
                parentNode.getChdNode(index).setPath(path + "0");
                parentNode.getChdNode(parentNode.getChdSize() - 1).setPath(path + "1");
            }
        }

        return parentNode;
    }

    private PartNode leafNodeRecursiveSplit(PartNode parentNode, int index, PartNode node, HashMap<String, PartNode> splitPath) {

        long[] dim_B_cost = node.getSplitPoint();
        Queue<Integer> que = new ArrayDeque<>();

        if (parentNode == null) {
            parentNode = tryLeafNodeSplit(null, -1, node, dim_B_cost);
            node.setPath("");
            splitPath.put("", node);
            if (parentNode == null) {
                return null;
            } else {
                node.setDim_B_cost(dim_B_cost);
            }

            que.add(0);
            que.add(1);
            parentNode.getChdNode(0).setPath("0");
            parentNode.getChdNode(1).setPath("1");
        } else {
            que.add(index);
        }

        while (!que.isEmpty()) {
            index = que.poll();
            PartNode tryChdNode = parentNode.getChdNode(index);
            String path = tryChdNode.getPath();

            splitPath.put(path, tryChdNode);

            dim_B_cost = tryChdNode.getSplitPoint(); // partition information

            PartNode newParentNode = tryLeafNodeSplit(parentNode, index, tryChdNode, dim_B_cost);
            if (newParentNode != null) {
                parentNode = newParentNode;
                tryChdNode.setDim_B_cost(dim_B_cost);
                que.add(index);
                que.add(parentNode.getChdSize() - 1);
                parentNode.getChdNode(index).setPath(path + "0");
                parentNode.getChdNode(parentNode.getChdSize() - 1).setPath(path + "1");
            }

            else {
                if (tryChdNode.dataList.size() > 1000) {
                    dim_B_cost = tryChdNode.getSplitPoint(); // partition information
                    newParentNode = tryLeafNodeSplit(parentNode, index, tryChdNode, dim_B_cost);
                }
            }
        }

        return parentNode;
    }

    private PartNode tryNonLeafNodeSplit(PartNode parentNode, int index, PartNode tryChdNode, long[] dim_B_cost, PartNode refLeftNode, PartNode refRightNode) {
        if (refLeftNode == null || refRightNode == null) {return null;}

        PartNode[] subNodes = nonLeafNodeSplit(tryChdNode, (int) dim_B_cost[0], (int) dim_B_cost[1], refLeftNode, refRightNode);

        long deltaT = subNodes[0].cost + subNodes[1].cost - tryChdNode.cost;

        if (parentNode == null) {
            deltaT += (T1 + tryChdNode.c * T2 + 2 * tryChdNode.c * T3) * COP_W;
        } else {
            deltaT += (tryChdNode.c * T3) * COP_W;
        }

        if (deltaT < 0) {
            if (parentNode != null) {

                if (subNodes[0].getChdSize() == 1) {
                    subNodes[0] = subNodes[0].getChdNode(0);
                }
                if (subNodes[1].getChdSize() == 1) {
                    subNodes[1] = subNodes[1].getChdNode(0);
                }

                parentNode.setChdNode(index, subNodes[0]);
                parentNode.addNode(subNodes[1]);
                return parentNode;
            } else {
                return new PartNode(tryChdNode, subNodes[0], subNodes[1], tryChdNode.level + 1);
            }
        }

        return null;
    }


    private PartNode tryLeafNodeSplit(PartNode parentNode, int index, PartNode tryChdNode, long[] dim_B_cost) {

        if (dim_B_cost == null) return null;

        long deltaT = dim_B_cost[2] - tryChdNode.cost;
        if (parentNode == null) {
            deltaT += ((T1 + tryChdNode.c * T2 + 2 * tryChdNode.c * T3)) * COP_W;
        } else {
            deltaT += (tryChdNode.c * T3) * COP_W;
        }


        PartNode[] subNodes = leafNodeSplit(tryChdNode, (int) dim_B_cost[0], (int) dim_B_cost[1]);
        if (deltaT - dim_B_cost[2] + subNodes[0].cost + subNodes[1].cost < 0) {

            partitions.add(subNodes[0].range);
            partitions.add(subNodes[1].range);

            if (parentNode != null) {
                parentNode.setChdNode(index, subNodes[0]);
                parentNode.addNode(subNodes[1]);
                return parentNode;
            } else {
                return new PartNode(tryChdNode, subNodes[0], subNodes[1], 1);
            }
        }

//        if (tryChdNode.isLeafNode() && tryChdNode.dataList.size() > 1000) {
//            int a = 1;
//            dim_B_cost = tryChdNode.getSplitPoint();
//            DatasetVisualizer.draw(tryChdNode.workload, tryChdNode.dataList,  (int)dim_B_cost[0],  (int)dim_B_cost[1]);
//
//            subNodes = leafNodeSplit(tryChdNode, (int) dim_B_cost[0], (int) dim_B_cost[1]);
//            dim_B_cost = tryChdNode.getSplitPoint();
//        }

        // split fails
        return null;
    }

    private PartNode[] nonLeafNodeSplit(PartNode node, int dimension, int b, PartNode refLeftNode, PartNode refRightNode) {
        List<PartNode> leftNode = new ArrayList<>();
        List<PartNode> rightNode = new ArrayList<>();

        for (PartNode p : node.getChd()) {
            if (p.range[0].data[dimension] <= b) {
                leftNode.add(p);
            } else {
                rightNode.add(p);
            }
        }
        return new PartNode[] {new PartNode(refLeftNode, leftNode, node.level, true), new PartNode(refRightNode, rightNode, node.level, true)};
    }


    //    <= b : left  ;   > b : right
    private PartNode[] leafNodeSplit(PartNode node, int dimension, int b) {
        List<DataPoint> leftData = new ArrayList<>();
        List<DataPoint> rightData = new ArrayList<>();
        HashSet<DataPoint[]> leftWorkload = new HashSet<>();
        HashSet<DataPoint[]> rightWorkload = new HashSet<>();
        DataPoint[] leftRange = new DataPoint[] {new DataPoint(new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE}), new DataPoint(new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE})};
        DataPoint[] rightRange = new DataPoint[] {new DataPoint(new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE}), new DataPoint(new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE})};

//        DataPoint[] splitLeftRange = new DataPoint[] {new DataPoint(new int[] {node.range[0].data[0], node.range[0].data[1]}), new DataPoint(new int[] {node.range[1].data[0], node.range[1].data[1]})};
//        DataPoint[] splitRightRange = new DataPoint[] {new DataPoint(new int[] {node.range[0].data[0], node.range[0].data[1]}), new DataPoint(new int[] {node.range[1].data[0], node.range[1].data[1]})};
//        splitLeftRange[1].data[dimension] = b;
//        splitRightRange[0].data[dimension] = b + 1;


        for (DataPoint p : node.dataList) {
            if (p.data[dimension] <= b) {
                leftData.add(p);
                leftRange[0].data[0] = Math.min(leftRange[0].data[0], p.data[0]);
                leftRange[0].data[1] = Math.min(leftRange[0].data[1], p.data[1]);
                leftRange[1].data[0] = Math.max(leftRange[1].data[0], p.data[0]);
                leftRange[1].data[1] = Math.max(leftRange[1].data[1], p.data[1]);
            } else {
                rightData.add(p);
                rightRange[0].data[0] = Math.min(rightRange[0].data[0], p.data[0]);
                rightRange[0].data[1] = Math.min(rightRange[0].data[1], p.data[1]);
                rightRange[1].data[0] = Math.max(rightRange[1].data[0], p.data[0]);
                rightRange[1].data[1] = Math.max(rightRange[1].data[1], p.data[1]);
            }
        }

        for (DataPoint[] range : node.workload) {

            if (range[1].data[0] >= leftRange[0].data[0] && leftRange[1].data[0] >= range[0].data[0] &&
                    range[1].data[1] >= leftRange[0].data[1] && leftRange[1].data[1] >= range[0].data[1]) {
                leftWorkload.add(range);
            }
            if (range[1].data[0] >= rightRange[0].data[0] && rightRange[1].data[0] >= range[0].data[0] &&
                    range[1].data[1] >= rightRange[0].data[1] && rightRange[1].data[1] >= range[0].data[1]) {
                rightWorkload.add(range);
            }
        }

//        return new PartNode[] {new PartNode(leftData, leftWorkload, leftRange, splitLeftRange, node.level), new PartNode(rightData, rightWorkload, rightRange, splitRightRange, node.level)};
//        return new PartNode[] {new PartNode(leftData, leftWorkload, splitLeftRange, node.level), new PartNode(rightData, rightWorkload, splitRightRange, node.level)};
        return new PartNode[] {new PartNode(leftData, leftWorkload, leftRange, node.level), new PartNode(rightData, rightWorkload, rightRange, node.level)};
    }

    public void kdTreeBuild(RangeDataset dataset) {
        this.partTreeRoot = recursiveKDTreeBuild(dataset);
    }

    public PartNode recursiveKDTreeBuild(RangeDataset dataset) {
        PartNode node;
        if (dataset.dataset.size() <= 2) {

            node = new PartNode(dataset);
        } else {
            List<List<DataPoint>> subDataset = splitByKD(dataset.dataset);
            PartNode left = recursiveKDTreeBuild(new RangeDataset(subDataset.get(0)));
            PartNode right = recursiveKDTreeBuild(new RangeDataset(subDataset.get(1)));
            node = new PartNode(dataset.range, left, right);
        }
        partitions.add(node.range);
        return node;
    }

    private List<List<DataPoint>> splitByKD(List<DataPoint> dataset) {
        int dim = getMaxVarianceDimension(dataset);
        dataset.sort(Comparator.comparingInt(dp -> dp.data[dim]));

        int splitIndex = dataset.size() / 2;
        List<DataPoint> left = new ArrayList<>(dataset.subList(0, splitIndex));
        List<DataPoint> right = new ArrayList<>(dataset.subList(splitIndex, dataset.size()));
        List<List<DataPoint>> list = new ArrayList<>();
        list.add(left); list.add(right);
        return list;
    }

    public static int getMaxVarianceDimension(List<DataPoint> dataset) {

        double[] sum = new double[2];

        int n = dataset.size();
        for (DataPoint dp : dataset) {
            for (int i = 0; i < 2; i++) {
                sum[i] += dp.data[i];
            }
        }
        double[] avg = new double[2];
        for (int i = 0; i < 2; i++) {
            avg[i] = sum[i] / n;
        }
        double[] variance = new double[2];
        for (DataPoint dp : dataset) {
            for (int i = 0; i < 2; i++) {
                variance[i] += Math.pow(dp.data[i] - avg[i], 2);
            }
        }
        if (variance[0] > variance[1]) {return 0;}
        else return 1;
    }

    public static void main(String[] args) {
//        test("BPD", "MIX", 1000000);


        String[] datasetNames = new String[] {"Uniform", "Skew", "BPD", "OSM"};
//        String[] datasetNames = new String[] {"OSM"};
//        String[] typeNames = new String[] {"UNI", "GAU", "LAP", "MIX"};
        String[] rates = new String[] {"0.2%", "0.4%", "0.6%", "0.8%", "1.0%"};
//        int[] dataSize = new int[] {200000, 400000, 600000, 800000, 1000000};

//        long[] cop = new long[]{0,1,1,1,1,1,1,2,4,8,16,32,1};
//        long[] sto = new long[]{1,32,16,8,4,2,1,1,1,1,1,1,0};

//        for (double errRate = 0.01; errRate)

        test("OSM", "GAU");
//        test("OSM", "BPD", "UNI", "GAU");

//        double[] bufferRate = new double[]{1.1, 1.2, 1.3, 1.4,1.5,1.6,1.7,1.8,1.9,2.0};
//        for (String datasetName : datasetNames) {
//
//            for (double rate : bufferRate) {
//                System.out.printf("Rate:" + rate + "--------------------");
//                test("Uniform", "SKew", "MIX", "GAU", rate);
//                System.out.println();
//            }
//
//
//
//
////            for (int i = 0;  i < cop.length; ++i) {
////                System.out.printf("Dataset name:" + datasetName + "--------------------");
////                System.out.println("cop: " + cop[i] + ", sto: " + sto[i]);
////                test(datasetName, "MIX", 1000000, cop[i], sto[i]);
////                System.out.println();
////            }
//        }

    }




    public static void test(String datasetName, String workloadName) {
        long s,e;
        List<DataPoint> dataset = readDataset("D:\\paper_source\\work_6\\dataset\\" + datasetName + "_1M");
        dataset = dataset.subList(0, 1000000);

        RangeDataset rangeDataset = new RangeDataset(dataset);
        HashSet<DataPoint[]> workload = readWorkload("D:\\paper_source\\work_6\\dataset\\" + workloadName + "_Workload_" + datasetName + "_1M_R0.01%", 800);

//        IndexSP.setUpdatePar(1, 0.4);
        IndexSP.setCostRate(32,1);


//        IndexSP.T3 = t3;
        s = System.currentTimeMillis();
        IndexSP indexSP = new IndexSP(rangeDataset, workload, IndexBuildType.WBPRTree);
        e = System.currentTimeMillis();
        System.out.println("build time:" + (e - s) / 1000.0 + "s");
        indexSP.getIndexSize();
        indexSP.buildPathTree();

        indexSP.workloadQueryTest();

//        s = System.currentTimeMillis();
//        int j = 0;
//        for (DataPoint[] query : updatedWorkload) {
//            if (j >= 400) {
//                break;
//            }
//            j++;
//            indexSP.updateWorkload(query);
//        }
//        e = System.currentTimeMillis();
//        System.out.println("update time:" + (e - s) / 400.0 + "ms");

//        List<Integer> dataset1 = indexSP.getDataset(indexSP.root);

//        s = System.currentTimeMillis();
//        for (int i = 0; i < updateDataset.size(); i++) {
//            indexSP.addData(updateDataset.get(i));
//        }
//        e = System.currentTimeMillis();
//        System.out.println("update time:" + (double)(e - s) / updateDataset.size() + "ms");
////
//        indexSP.workloadQueryTest();
//        indexSP.getIndexSize();

//        s = System.nanoTime();
//        Token[] tokens = new Token[queryLen];
//        int j = 0;
//        for (DataPoint[] dataPoints : workload) {
//            tokens[j++] = tokenGeneration(dataPoints);
//        }
//        e = System.nanoTime();
//        System.out.println("avg token generate time:" + (e - s) / workload.size() / 1e6 + "ms");

//        double size = 0;
//        for (Token token : tokens) {
//            size += token.getTokenSize();
//        }
//        System.out.println("avg token size: " + (size / tokens.length) + "kb");
//
//
//        for (int i = 0; i < queryLen; i++) {
//            List<Integer> res_encrypt = indexSP.query(tokens[i]);
////            System.out.println(res_encrypt.size());
//        }
////        indexSP.printAvgParameter();
//
//        s = System.nanoTime();
//        for (int i = 0; i < queryLen; i++) {
//            List<Integer> res_encrypt = indexSP.query(tokens[i]);
//        }
//        e = System.nanoTime();
//        System.out.println("avg query time:" + (e - s) / (double) 1e6 / workload.size() + "ms");
//
////        indexSP.printAvgParameter();

    }

}
