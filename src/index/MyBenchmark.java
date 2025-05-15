package index;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dataset.ReadDataset.readDataset;
import static dataset.ReadDataset.readWorkload;
import static index.IndexSP.tokenGeneration;
import static index.WBTreeNode.*;

@State(Scope.Thread) // 每个线程独享状态
@BenchmarkMode(Mode.AverageTime) // 测试模式：平均时间
@OutputTimeUnit(TimeUnit.NANOSECONDS) // 输出时间单位
@Warmup(iterations = 3, time = 1) // 预热：3 轮，每轮 1 秒
@Measurement(iterations = 5, time = 1) // 正式测试：5 轮，每轮 1 秒
public class MyBenchmark {
    private IndexSP indexSP; // 索引对象
    private HashSet<DataPoint[]> workload; // 工作负载

    Token token;
    @Setup(Level.Trial) // 初始化方法，整个测试前执行一次
    public void setup() {
        int len = 100000;
        List<DataPoint> dataset = readDataset("D:\\paper_source\\work_6\\dataset\\Uniform_1M");
        dataset = dataset.subList(0, len);
        // 初始化数据集和工作负载
        RangeDataset rangeDataset = new RangeDataset(dataset);
        workload = readWorkload("D:\\paper_source\\work_6\\dataset\\MIX_Workload_Uniform_1M_R0.2%", 800);


        // 构建索引
        IndexSP.setCostRate(1, 1);
        long s = System.currentTimeMillis();
        indexSP = new IndexSP(rangeDataset, workload, IndexSP.IndexBuildType.BPRIndex);
        long e = System.currentTimeMillis();
        System.out.println("build time:" + (e - s) + "ms");

        token = tokenGeneration(workload.iterator().next());
        System.out.println("token size:" + (token.left[0].size() + token.left[1].size() + token.right[0].size() + token.right[1].size()));

        // processing of token attach path
        leftToken = new int[2][];
        rightToken = new int[2][];

        byte[][][] leftKey = new byte[2][][];
        byte[][][] rightKey = new byte[2][][];
        for (int i = 0; i < 2; i++) {
            leftToken[i] = new int[token.left[i].size()];
            rightToken[i] = new int[token.right[i].size()];

            leftKey[i] = new byte[token.left[i].size()][];
            rightKey[i] = new byte[token.right[i].size()][];

            for (int j = 0; j < token.left[i].size(); ++j) {
                leftToken[i][j] = attachPathToKey(token.left[i].get(j), "");
                leftKey[i][j] = getDecKey(token.leftKey[i].get(j), "");
            }
            for (int j = 0; j < token.right[i].size(); ++j) {
                rightToken[i][j] = attachPathToKey(token.right[i].get(j), "");
                rightKey[i][j] = getDecKey(token.rightKey[i].get(j), "");
            }
        }
//        T1Test();
//        T2Test();
    }
    int[][] leftToken = new int[2][];
    int[][] rightToken  = new int[2][];

    byte[][][] leftKey;
    byte[][][] rightKey;

    @Benchmark
    public void T1Test() {

        // processing of token attach path
        leftToken = new int[2][];
        rightToken = new int[2][];

        byte[][][] leftKey = new byte[2][][];
        byte[][][] rightKey = new byte[2][][];
        for (int i = 0; i < 2; i++) {
            leftToken[i] = new int[token.left[i].size()];
            rightToken[i] = new int[token.right[i].size()];

            leftKey[i] = new byte[token.left[i].size()][];
            rightKey[i] = new byte[token.right[i].size()][];

            for (int j = 0; j < token.left[i].size(); ++j) {
                leftToken[i][j] = attachPathToKey(token.left[i].get(j), "");
                leftKey[i][j] = getDecKey(token.leftKey[i].get(j), "");
            }
            for (int j = 0; j < token.right[i].size(); ++j) {
                rightToken[i][j] = attachPathToKey(token.right[i].get(j), "");
                rightKey[i][j] = getDecKey(token.rightKey[i].get(j), "");
            }
        }

    }

    int len;
    List<Integer> res = new ArrayList<>();

    @Benchmark
    public void T2Test() {

        WBTreeNode psfTreeNode = indexSP.root;

        boolean isLeafNode = psfTreeNode.isLeafNode();
        if (isLeafNode) len = psfTreeNode.ids.size();
        else len = psfTreeNode.chd.size();

        BitSet left0 = new BitSet(len);
        BitSet left1 = new BitSet(len);
        BitSet right0 = new BitSet(len);
        BitSet right1 = new BitSet(len);
        for (int j = 0; j < token.left[0].size(); ++j) {
            left0.or(psfTreeNode.decToken(leftToken[0][j], leftKey[0][j]));
        }
        for (int j = 0; j < token.left[1].size(); ++j) {
            left1.or(psfTreeNode.decToken(leftToken[1][j], leftKey[1][j]));
        }
        for (int j = 0; j < token.right[0].size(); ++j) {
            right0.or(psfTreeNode.decToken(rightToken[0][j], rightKey[0][j]));
        }
        for (int j = 0; j < token.right[1].size(); ++j) {
            right1.or(psfTreeNode.decToken(rightToken[1][j], rightKey[1][j]));
        }

        BitSet resBitset = right0;
        resBitset.and(right1);
        resBitset.andNot(left0);
        resBitset.andNot(left1);

        if (isLeafNode) {
            for (int i = 0; i < len; ++i) {
                if (resBitset.get(i)) {
                    res.add(psfTreeNode.ids.get(i));
                }
            }
        } else {
            for (int i = 0; i < len; ++i) {
                if (resBitset.get(i)) {
//                    query(psfTreeNode.chd.get(i), path + i, token, res);
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
        org.openjdk.jmh.Main.main(args);
    }
}
