package index;

import java.util.*;

public class PathTreeNode {
//    DataPoint[] splitRange;
    DataPoint[] range;
    PathTreeNode leftChd;
    PathTreeNode rightChd;

    HashSet<DataPoint[]> workload;
    List<DataPoint> dataList;

    String encPath;
    WBTreeNode WBTreeNode;
    PathTreeNode parentNode;

    long constructCost;


    long nowCost; // current cost after update
    long buffDeltaCost; // record cost that has not add to nowCost

    long c;

    boolean needRebuild = false;
    DataPoint[] newRange = null;

    public int getTheBelongChildIndex() {
        String str = encPath.substring(parentNode.encPath.length());
        return Integer.valueOf(str, 10);
    }

    static double errRate = 0.1;

    public long updC(long deltC, boolean isAdd) { // update the cost, and return the updated cost
        long preCost = nowCost;
        if (isAdd) this.c += deltC;
        else this.c -= deltC;
        this.c += deltC;
        if (this.WBTreeNode != null) {
            nowCost = WBTreeNode.getCost(c);
        }

        if (notBalance()) needRebuild = true;

        return nowCost - preCost;
    }


    public void setCost(long cost) {
        this.nowCost = this.constructCost = cost;
    }

    public long extend() {
        long preCost = this.nowCost;
        if (this.dataList == null) { // internal node
            List<Integer> chdIndex = new ArrayList<>();
            List<DataPoint[]> chdRange = new ArrayList<>();
            getSubRange(this, chdRange, chdIndex);
            WBTreeNode.extend(encPath, chdRange, chdIndex);
            this.buffDeltaCost = WBTreeNode.getCost(c) - preCost;
            return this.buffDeltaCost;
        } else {
            WBTreeNode.extend(encPath, dataList);
            return WBTreeNode.getCost(c) - preCost;
        }
    }


    public boolean notBalance() {
        if (leftChd == null || rightChd == null) {
            return false;
        }

        double PD = Math.abs((double)leftChd.nowCost / (leftChd.nowCost + rightChd.nowCost) - (double)leftChd.constructCost / (leftChd.constructCost + rightChd.constructCost));


//        if (PD >= errRate) {
//            System.out.println("PD: " + PD + ", preCostLeft: " + leftChd.constructCost + ", preCostRight: " + rightChd.constructCost + ", nowCostLeft: " + leftChd.nowCost + ", nowCostRight: " + rightChd.nowCost);
//        }

        return PD >= errRate;

    }

//    public boolean notBalance() {
//        if (!this.isLeafNode()) {
////            double nowCost = getCostRate();
//            boolean x= getCostRate() - costRate >= errRate;
////            if (x) {
////                System.out.println("not balance!!");
////            }
//            return x;
//        }
//        return false;
//    }

    public void setRange() {
        if (leftChd != null && rightChd != null) {
            range = new DataPoint[2];
            range[0] = new DataPoint(new int[]{Math.min(leftChd.range[0].data[0], rightChd.range[0].data[0]), Math.min(leftChd.range[0].data[1], rightChd.range[0].data[1])});
            range[1] = new DataPoint(new int[]{Math.max(leftChd.range[1].data[0], rightChd.range[1].data[0]), Math.max(leftChd.range[1].data[1], rightChd.range[1].data[1])});
        }
    }

    public void checkRange() {

        DataPoint[] range_ = new DataPoint[] {new DataPoint(new int[] {range[0].data[0], range[0].data[1]}), new DataPoint(new int[] {range[1].data[0], range[1].data[1]})};
//        this.dataSize++;
        boolean isChange = false;

        if (leftChd.newRange != null) {
            if (leftChd.range[0].data[0] < range[0].data[0]){
                range_[0].data[0] = leftChd.range[0].data[0];
                isChange = true;
            }
            if (leftChd.range[0].data[1] < range[0].data[1]){
                range_[0].data[1] = leftChd.range[0].data[1];
                isChange = true;
            }
            if (leftChd.range[1].data[0] > range[1].data[0]) {
                range_[1].data[0] = leftChd.range[1].data[0];
                isChange = true;
            }
            if (leftChd.range[1].data[1] > range[1].data[1]) {
                range_[1].data[1] = leftChd.range[1].data[1];
                isChange = true;
            }
            leftChd.newRange = null;
        }

        if (rightChd.newRange != null) {
            if (rightChd.range[0].data[0] < range[0].data[0]){
                range_[0].data[0] = leftChd.range[0].data[0];
                isChange = true;
            }
            if (rightChd.range[0].data[1] < range[0].data[1]){
                range_[0].data[1] = leftChd.range[0].data[1];
                isChange = true;
            }
            if (rightChd.range[1].data[0] > range[1].data[0]) {
                range_[1].data[0] = rightChd.range[1].data[0];
                isChange = true;
            }
            if (rightChd.range[1].data[1] > range[1].data[1]) {
                range_[1].data[1] = rightChd.range[1].data[1];
                isChange = true;
            }
            rightChd.newRange = null;
        }

        if (isChange) {
            newRange = range_;
            if (WBTreeNode == null) {
                range = newRange;
            }// else pre range used for update the parent node info
        }
    }


//    public boolean errExceed() {
////        System.out.println(T1 + T2 * c + T3 * c * m * bitsetSizeRate - cost);
//        return Math.abs(T1 + T2 * c + T3 * c * m * bitsetSizeRate - cost) > cost * errRate || spTreeNode != null && spTreeNode.bitsetLen <= m;
//    }

//    int querySize;
//    int dataSize;
//
//    double querySizeRate;
//    double dataSizeRate;


//    public DataPoint[] mergeRange(DataPoint[] leftRange, DataPoint[] rightRange) {
//        DataPoint[] range_ = new DataPoint[] {new DataPoint(new int[] {range[0].data[0], range[0].data[1]}), new DataPoint(new int[] {range[1].data[0], range[1].data[1]})};
//    }


    public long addCost(long cst) {
        cst += buffDeltaCost;
        buffDeltaCost = 0;
        this.nowCost += cst ;

        if (notBalance()) {
            this.needRebuild = true;
        }

        return cst;
    }



    // if data exceed MBR, return true
    public void addData(DataPoint dataPoint) {
        DataPoint[] range_ = new DataPoint[] {new DataPoint(new int[] {range[0].data[0], range[0].data[1]}), new DataPoint(new int[] {range[1].data[0], range[1].data[1]})};
        this.dataList.add(dataPoint);
//        this.dataSize++;
        boolean isUpdate = false;
        if (dataPoint.data[0] < range[0].data[0]){
            range_[0].data[0] = dataPoint.data[0];
            isUpdate = true;
        }
        if (dataPoint.data[1] < range[0].data[1]){
            range_[0].data[1] = dataPoint.data[1];
            isUpdate = true;
        }
        if (dataPoint.data[0] > range[1].data[0]) {
            range_[1].data[0] = dataPoint.data[0];
            isUpdate = true;
        }
        if (dataPoint.data[1] > range[1].data[1]) {
            range_[1].data[1] = dataPoint.data[1];
            isUpdate = true;
        }
        if (isUpdate){
            newRange = range_;
        }
    }



    public void init(PartNode partNode, String encPath) {
        if (partNode.isLeafNode()) {
            this.workload = partNode.workload;
            this.dataList = partNode.dataList;
//            this.querySize = this.workload.size();
//            this.dataSize = this.dataList.size();
        }
        this.WBTreeNode = partNode.WBTreeNode;
        this.encPath = encPath;
        this.c = partNode.c;
        this.range = partNode.range;
    }

    public boolean isLeafNode() {
        return this.leftChd == null && this.rightChd == null;
    }

    public boolean isEncInterNode() {
        return !isLeafNode() && WBTreeNode != null;
    }

    public void deleteQuery(DataPoint[] query) {
        workload.remove(query);
    }


//    public boolean needRebuild() {
//        if (this.leftChd != null && this.leftChd.errExceed() || this.rightChd != null && this.rightChd.errExceed()) {return true;}
//        else return false;
//    }

    public void getSubRange(PathTreeNode rebuildParentNode, List<DataPoint[]> subRanges, List<Integer> chdIndex) {
        Queue<PathTreeNode> queue = new LinkedList<>();
        queue.offer(this);
        while (!queue.isEmpty()) {
            PathTreeNode node = queue.poll();
            if (rebuildParentNode != null &&  node.parentNode != null && node.parentNode == rebuildParentNode) {
                chdIndex.add(node.getTheBelongChildIndex());
                subRanges.add(node.range);
            } else {
                if (node.leftChd != null) {
                    queue.offer(node.leftChd);
                }
                if (node.rightChd != null) {
                    queue.offer(node.rightChd);
                }
            }
        }
    }


    public void getSubTreeDataAndQuery(PathTreeNode rebuildParentNode, List<DataPoint> dataList, HashSet<DataPoint[]> workload, List<Integer> chdIndex, List<DataPoint[]> subRanges) {
        Queue<PathTreeNode> queue = new LinkedList<>();
        queue.offer(this);
        while (!queue.isEmpty()) {
            PathTreeNode node = queue.poll();
            if (rebuildParentNode != null &&  node.parentNode != null && node.parentNode == rebuildParentNode) {
                chdIndex.add(node.getTheBelongChildIndex());
                subRanges.add(node.range);
            }
            node.WBTreeNode = null;
            node.range = null;

            if (node.dataList != null) {
                dataList.addAll(node.dataList);
                workload.addAll(node.workload);
                node.dataList = null;
                node.workload = null;
            }

            if (node.leftChd != null) {
                queue.offer(node.leftChd);
                node.leftChd = null;
            }
            if (node.rightChd != null) {
                queue.offer(node.rightChd);
                node.rightChd = null;
            }
        }
    }

    public void setRange(DataPoint[] range) {
        this.range = range;
    }
}
