package index;

import java.util.*;

import static index.IndexSP.*;


public class GradientDescent {

//    private static int modelErr = 1;
//    private static List<GraModel> modelList = new ArrayList<>();
//    private static List<double[]> lower;
//    private static List<double[]> upper;
//    private static int upperStart = 0;
//    private static int lowerStart = 0;
//    private static double firstX;
//    private static int dataSize = 0;
//    private static double[][] rectangle = new double[4][2];
//
//    private static double getSlop(double[] p1, double[] p2) {
//        return ((p2[1] - p1[1])) / (p2[0] - p1[0]);
//    }
//    private static List<GraModel> getModelList() {
//        return modelList;
//    }
//    private static double cross(double[] pointO, double[] pointA, double[] pointB) {
//        return getSlop(pointB, pointO) - getSlop(pointA, pointO);
//    }
//    private static void addRegToModels() {
//        double[] slopAndIntercept = getSlopAndIntercept();
//        GraModel model = new GraModel(firstX, slopAndIntercept[0], slopAndIntercept[1]);
//        modelList.add(model);
//        GradientDescent.clear();
//    }
//    private static void clear() {
//        lower.clear();
//        upper.clear();
//        lowerStart = 0;
//        upperStart = 0;
//        rectangle = new double[4][2];
//        dataSize = 0;
//    }
//    private static double[] getSlopAndIntercept() {
//        if (dataSize == 1) {
//            return new double[] {0, (rectangle[0][1] + rectangle[1][1]) / 2.0};
//        }
//        double intercept, slop;
//        double slop1 = getSlop(rectangle[2], rectangle[0]);
//        double slop2 = getSlop(rectangle[3], rectangle[1]);
//
//
//        slop = (slop1 + slop2) / 2;
//        if (slop1 == slop2)
//            intercept = rectangle[0][0] - rectangle[0][1] / slop;
//        else {
//            double tmp = slop2 - slop1;
//            double x0 = (rectangle[0][1] - slop1 * rectangle[0][0] + slop2 * rectangle[1][0] - rectangle[1][1]) / tmp;
//            double y0 = (slop1 * slop2 * (rectangle[1][0] - rectangle[0][0]) + rectangle[0][1] * slop2 - rectangle[1][1] * slop1) / tmp;
//            intercept = x0 - y0 / slop;
//        }
//        return new double[]{slop, intercept};
//    }
//
//    private static void addKey(double x, double y) {
//        double[] p1 = new double[]{x, y + modelErr};
//        double[] p2 = new double[]{x, y - modelErr};
//
//        if (dataSize == 0) {
//            firstX = x;
//            rectangle[0] = p1;
//            rectangle[1] = p2;
//            lower.clear();
//            upper.add(p1);
//            lower.add(p2);
//            dataSize++;
//            return;
//        }
//        if (dataSize == 1) {
//            rectangle[2] = p2;
//            rectangle[3] = p1;
//            upper.add(p1);
//            lower.add(p2);
//            dataSize++;
//            return;
//        }
//
//        double slope1 = getSlop(rectangle[2], rectangle[0]);
//        double slope2 = getSlop(rectangle[3], rectangle[1]);
//        if (getSlop(p1, rectangle[2]) < slope1 || getSlop(p2, rectangle[3]) > slope2) {
//            addRegToModels();
//            addKey(x, y);
//            return;
//        }
//
//        if (getSlop(p1, rectangle[1]) < slope2) {
//            double min = getSlop(lower.get(lowerStart), p1);
//            int min_i = lowerStart;
//            for (int i = lowerStart + 1; i < lower.size(); ++i) {
//                double val = getSlop(lower.get(i), p1);
//                if (val > min) break;
//                min = val;
//                min_i = i;
//            }
//
//            rectangle[1] = lower.get(min_i);
//            rectangle[3] = p1;
//            lowerStart = min_i;
//
//            int end = upper.size();
//            for (; end >= upperStart + 2 && cross(upper.get(end - 2), upper.get(end - 1), p1) <= 0; --end) {
//                upper.remove(end - 1);
//            }
//            upper.add(p1);
//        }
//
//        if (getSlop(p2, rectangle[0]) > slope1) {
//            double max = getSlop(upper.get(upperStart), p2);
//            int max_i = upperStart;
//            for (int i = upperStart + 1; i < upper.size(); ++i) {
//                double val = getSlop(upper.get(i), p2);
//                if (val < max) break;
//                max = val;
//                max_i = i;
//            }
//
//            rectangle[0] = upper.get(max_i);
//            rectangle[2] = p2;
//            upperStart = max_i;
//
//            int end = lower.size();
//            for (; end >= lowerStart + 2 && cross(lower.get(end - 2), lower.get(end - 1), p2) >= 0; --end) {
//                lower.remove(end - 1);
//            }
//            lower.add(p2);
//        }
//
//    }
//
//    public static void init(int modelErr) {
//        GradientDescent.modelErr = modelErr;
//        lower = new ArrayList<>();
//        upper = new ArrayList<>();
//        modelList = new ArrayList<>();
//        GradientDescent.clear();
//    }
//
//    public static List<GraModel> trainModels(int modelErr, List<int[]> dataset) {
//        init(modelErr);
//        for (int[] data : dataset) {
//            addKey(data[0], data[1]);
//        }
//        addRegToModels();
//        return getModelList();
//    }
public static List<Integer> findSplitIndices(List<int[]> data) {
    List<Integer> splitIndices = new ArrayList<>();
    int n = data.size();
    if (n <= 3) return splitIndices;

    double[] gradients = new double[n - 1];
    for (int i = 1; i < n; i++) {
        gradients[i - 1] = (data.get(i)[1] - data.get(i - 1)[1]) / (double) (data.get(i)[0] - data.get(i - 1)[0]);
    }

    double[] smoothedGradient = new double[n - 1];
    int windowSize = Math.max(2, n / 10);
    for (int i = 0; i < n - 1; i++) {
        double sum = 0;
        int count = 0;
        for (int j = Math.max(0, i - windowSize / 2); j < Math.min(n - 1, i + windowSize / 2); j++) {
            sum += gradients[j];
            count++;
        }
        smoothedGradient[i] = sum / count;
    }

    TreeSet<double[]> st = new TreeSet<>(new Comparator<double[]>() {
        @Override
        public int compare(double[] o1, double[] o2) {
            return Double.compare(o2[0], o1[0]);
        }
    });
    for (int i = 1; i < n - 1; i++) {
        st.add(new double[]{Math.abs(smoothedGradient[i] - smoothedGradient[i - 1]), i});
    }

    if (st.size() < 2) {
        splitIndices.add(0);
        splitIndices.add(data.size() - 1);
    } else {
        splitIndices.add((int) st.pollFirst()[1]);
        splitIndices.add((int) st.pollFirst()[1]);
    }
    Collections.sort(splitIndices);
    return splitIndices;
}

    public static GraModel fitLinearModel(List<int[]> data, int start, int end) {
        int n = end - start;
        if (n < 0) return new GraModel(0, 0 , 0 ,0);
        if (n == 1) return new GraModel(1, 0, data.get(start)[0], data.get(start)[0] + 1);

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int startX = data.get(start)[0];
        int endX = data.get(end - 1)[0];

        for (int i = start; i < end; i++) {
            int x = data.get(i)[0];
            int y = data.get(i)[1];
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (slope * sumX - sumY) / n;
        return new GraModel(slope, intercept, startX, endX);
    }

    public static List<GraModel> trainModels(List<int[]> data) {
        List<Integer> splitIndices = findSplitIndices(data);
        List<GraModel> models = new ArrayList<>();

        int start = 0;
        for (int splitIndex : splitIndices) {
            models.add(fitLinearModel(data, start, splitIndex + 1));
            start = splitIndex;
        }
        models.add(fitLinearModel(data, start, data.size()));
        return models;
    }

    // 目标函数 f(b)
    public static double objectiveFunction(double b, List<GraModel> modelsCL, List<GraModel> modelsCR, List<GraModel> modelsML, List<GraModel> modelsMR) {
        double McL = evaluateModel(b, modelsCL); // M^c_L(b)
        double McR = evaluateModel(b, modelsCR); // M^c_R(b)
        double MmL = evaluateModel(b, modelsML); // M^m_L(b)
        double MmR = evaluateModel(b, modelsMR); // M^m_R(b)

        return T1 * 2 + T2 * (McL + McR) + T3 * (McL * MmL + McR * MmR);
    }

    // 计算梯度 f'(b)
    private static double calculateGradient(double b, List<GraModel> modelsCL, List<GraModel> modelsCR, List<GraModel> modelsML, List<GraModel> modelsMR) {
        double McL = evaluateModel(b, modelsCL); // M^c_L(b)
        double McR = evaluateModel(b, modelsCR); // M^c_R(b)
        double MmL = evaluateModel(b, modelsML); // M^m_L(b)
        double MmR = evaluateModel(b, modelsMR); // M^m_R(b)

        double McLDerivative = evaluateDerivative(b, modelsCL); // M^{c'}_L(b)
        double McRDerivative = evaluateDerivative(b, modelsCR); // M^{c'}_R(b)
        double MmLDerivative = evaluateDerivative(b, modelsML); // M^{m'}_L(b)
        double MmRDerivative = evaluateDerivative(b, modelsMR); // M^{m'}_R(b)

        return T2 * (McLDerivative + McRDerivative) +
                T3 * (McLDerivative * MmL + McL * MmLDerivative + McRDerivative * MmR + McR * MmRDerivative);
    }

    // 计算分段线性函数的值
    private static double evaluateModel(double x, List<GraModel> models) {

        if (x < models.get(0).start) {
            return models.get(0).evaluate(x);
        }

        for (int i = 0; i < models.size(); ++i) {
            if (x >= models.get(i).start && (i + 1 >= models.size() ||  x < models.get(i +  1).start)) {
                return models.get(i).evaluate(x);
            }
        }
        throw new IllegalArgumentException("Point " + x + " is not within any model's range.");
    }

    // 计算分段线性函数的导数
    private static double evaluateDerivative(double x, List<GraModel> models) {

        if (x < models.get(0).start) {
            return models.get(0).derivative();
        }

        for (int i = 0; i < models.size(); ++i) {
            if (x >= models.get(i).start && (i + 1 >= models.size() ||  x < models.get(i +  1).start)) {
                return models.get(i).derivative();
            }
        }
        throw new IllegalArgumentException("Point " + x + " is not within any model's range.");
    }

    // 梯度下降法
    public static int gradientDescent(List<GraModel> modelsCL, List<GraModel> modelsCR, List<GraModel> modelsML, List<GraModel> modelsMR, double initialB, List<Integer> possibleB) {
        double b = initialB;
        for (int i = 0; i < maxIterations; i++) {
            // 计算梯度
            double gradient = calculateGradient(b, modelsCL, modelsCR, modelsML, modelsMR);

            // 更新分割点
            b = b - learningRate * gradient;

//            // 打印当前迭代结果
//            System.out.println("Iteration " + i + ": b = " + b + ", f(b) = " + objectiveFunction(b, modelsCL, modelsCR, modelsML, modelsMR));
        }

        int insertionPoint  = binarySearchInsertionPoint(possibleB, b);
        int leftNeighbor = Math.max(0, insertionPoint);
        int rightNeighbor = Math.min(modelsCL.size() - 1, insertionPoint + 1);

        if (objectiveFunction(possibleB.get(leftNeighbor), modelsCL, modelsCR, modelsML, modelsMR) < objectiveFunction(possibleB.get(rightNeighbor), modelsCL, modelsCR, modelsML, modelsMR)) {
            return possibleB.get(leftNeighbor);
        } else {
            return possibleB.get(rightNeighbor);
        }
    }

    private static int binarySearchInsertionPoint(List<Integer> possibleB, double b) {
        int left = 0, right = possibleB.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (possibleB.get(mid) <= b) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return left - 1; // 返回 b 应该插入的位置
    }


    public static void main(String[] args) {
        // 示例：定义分段线性函数模型
//        List<Model> modelsCL = List.of(new Model(0, 10, 1, 0)); // M^c_L(b) = b
//        List<Model> modelsCR = List.of(new Model(0, 10, -1, 10)); // M^c_R(b) = -b + 10
//        List<Model> modelsML = List.of(new Model(0, 10, 0.5, 0)); // M^m_L(b) = 0.5b
//        List<Model> modelsMR = List.of(new Model(0, 10, -0.5, 5)); // M^m_R(b) = -0.5b + 5
//
//        // 常数
//        double T1 = 1.0;
//        double T2 = 1.0;
//        double T3 = 1.0;
//
//        // 梯度下降参数
//        double initialB = 5.0; // 初始分割点
//        double learningRate = 0.01;
//        int maxIterations = 100;
//
//        // 运行梯度下降
//        double optimalB = gradientDescent(modelsCL, modelsCR, modelsML, modelsMR, initialB, learningRate, maxIterations, T1, T2, T3);
//        System.out.println("Optimal split point b: " + optimalB);
    }
}
