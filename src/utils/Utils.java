package utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Utils {

    //get fit query
    public static long[] generateQuery(int queryKeywords, long low, long high) {
        long[] query = new long[queryKeywords];
        long tmp = (long) ((high - low + 1) / (queryKeywords + 1));
        for (int i = 0; i < queryKeywords; ++i) {
            query[i] = low + tmp * (i + 1);
        }
        return query;
    }

    public static long[][] generateQuery(int count, int queryKeywords, long low, long high) {
        long[][] queries = new long[count][queryKeywords];
        for (int i = 0; i < count; ++i) {
            int j = 0;
            HashSet<Long> st = new HashSet<>();
            while (st.size() < queryKeywords) {
                long keyword = (long) (Math.random() * (high - low)) + low;
                if (st.contains(keyword)) continue;
                st.add(keyword);
                queries[i][j++] = keyword;
            }
        }
        return queries;
    }

    //get count queries that per query must have one result
    public static long[][] generateQuery(HashMap<Long, long[]> forwardIndex, int count, int queryKeywords) {
        long high = forwardIndex.size(), low = 1;

        long[][] queries = new long[count][queryKeywords];
        for (int i = 0; i < count; ++i) {
            long id = (long) (Math.random() * (high - low)) + low;
            while (forwardIndex.get(id).length < queryKeywords) {
                id = (long) (Math.random() * (high - low)) + low;
            }
            long[] idKeywords = forwardIndex.get(id);

            Set<Long> chosenElements = new HashSet<>();
            Random random = new Random();
            while (chosenElements.size() < queryKeywords) {
                int randomIndex = random.nextInt(idKeywords.length);
                long randomElement = idKeywords[randomIndex];
                chosenElements.add(randomElement);
            }

            long[] keywords = new long[chosenElements.size()];
            int index = 0;
            for (long element : chosenElements) {
                keywords[index++] = element;
            }

            queries[i] = keywords;
        }

        return queries;
    }



    //find the tar left bound
    public static int findLeftBound(long[] arr, long tar, int l, int r) {
        while (l <= r) {
            int mid = (l + r) / 2;
            if (arr[mid] <= tar) l = mid + 1;
            else r = mid - 1;
        }
        return l - 1;
    }

    //find the tar right bound
    public static int findRightBound(long[] arr, long tar, int left, int right) {
        int l = left, r = right;
        while (l <= r) {
            int mid = (l + r) / 2;
            if (arr[mid] < tar) l = mid + 1;
            else r = mid - 1;
        }
        return r + 1;
    }

    public static List<Long> sortMerge(long[] arr, List<Long> list) {
        List<Long> mergedList = new ArrayList<>(arr.length + list.size());
        int i = 0, j = 0;
        while (i < arr.length || j < list.size()) {
            if (j >= list.size() || i < arr.length && arr[i] < list.get(j)) {
                mergedList.add(arr[i++]);
            } else {
                mergedList.add(list.get(j++));
            }
        }
        return mergedList;
    }

    public static long[] sortMerge(List<Long> list1, List<Long> list2) {
        long[] mergedList = new long[list1.size() + list2.size()];
        int pos = 0;
        int i = 0, j = 0;
        while (i < list1.size() || j < list2.size()) {
            if (j >= list2.size() || i < list1.size() && list1.get(i) < list2.get(j)) {
                mergedList[pos++] = list1.get(i++);
            } else {
                mergedList[pos++] = list2.get(j++);
            }
        }
        return mergedList;
    }

    public static long[] sortMerge(long[] arr1, long[] arr2) {
        long[] mergedList = new long[arr1.length + arr2.length];
        int pos = 0;
        int i = 0, j = 0;
        while (i < arr1.length || j < arr2.length) {
            if (j >= arr2.length || i < arr1.length && arr1[i] < arr2[j]) {
                mergedList[pos++] = arr1[i++];
            } else {
                mergedList[pos++] = arr2[j++];
            }
        }
        return mergedList;
    }

    public static long[] buildRandArr(int len, long low, long high, long[] dataset) {
        long[] arr = new long[len];
        HashSet<Long> st = new HashSet<>();
        if (dataset != null) for (long data: dataset) st.add(data);
        for (int i = 0; i < len; ++i) {
            long num = (long) (Math.random() * (high - low)) + low;
            while (st.contains(num)) {
                num = (int) (Math.random() * (high - low)) + low;
            }
            st.add(num);
            arr[i] = num;
        }
        return arr;
    }

    public static byte[] encPosHash(String sk, BigInteger r, byte[] hash, int pos) {
        return SHA.bytesXor(hash, SHA.hashToBytes(sk + r + pos));
    }

    public static void shuffleArray(long[] array) {
        Random rand = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            // 交换位置
            long temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    //Unit: byte
    public static long getStrSize(String str) throws UnsupportedEncodingException {
        int byteSize = str.getBytes(StandardCharsets.UTF_8).length;
        return byteSize;
    }


    public static void main(String[] args) {

        long[] arr = new long[]{51, 53};
        int leftBound = findLeftBound(arr, 60, 0, 1);
    }
}
