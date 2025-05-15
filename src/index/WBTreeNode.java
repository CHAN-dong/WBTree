package index;

import utils.SHA;

import java.io.Serializable;
import java.util.*;

import static index.IndexSP.*;


public class WBTreeNode implements Serializable {

    int bitsetLen;

    HashMap<Integer, BitSet> encryptedBitsets = new HashMap<>();

    List<Integer> ids;
    List<WBTreeNode> chd;

    public boolean isLeafNode() {
        return ids != null;
    }

    public long getCost(long c) {

        int m = bitsetLen;
        int s = encryptedBitsets.size();
        long computeCost = T1 + T2 * c + T3 * c * m;
        long storageCost = s * (64 + (m + 63) / 64 * 64) + 8L * m;

        return COP_W * computeCost + STO_W * storageCost;
    }

    public int getM() {
        return this.chd == null ? ids.size() : this.chd.size();
    }

    public void extend(String encPath, List<DataPoint> dataPoints) {
        bitsetLen = (int) (this.getM() * bitsetSizeRate);
        encryptedBitsets = new HashMap<>();
        // map tokens to filters, and add filters to hash map
        int len = dataPoints.size();

        HashMap<String, List<Integer>> mp = new HashMap<>();
        for (int i = 0; i < len; i++) {
            // get each token
            for (int j = 0; j < 2; ++j) {
                List<String> encryptedLeftData;
                List<String> encryptedRightData;

                encryptedLeftData = SDC.encrypt(sk, j, TYPE_LEFT, dataPoints.get(i).data[j]);
                encryptedRightData = SDC.encrypt(sk, j, TYPE_RIGHT, dataPoints.get(i).data[j]);


                for (String leftToken : encryptedLeftData) {
                    mp.computeIfAbsent(leftToken, k -> new ArrayList<>()).add(i);
                }
                for (String rightToken : encryptedRightData) {
                    mp.computeIfAbsent(rightToken, k -> new ArrayList<>()).add(i);
                }
            }
        }

//        node.dataList = null;

        bitsetLen = (int) (len * bitsetSizeRate);
        // map tokens to filters, and add filters to hash map
        for (Map.Entry<String, List<Integer>> entry : mp.entrySet()) {
            String token = entry.getKey();
            String encToken = SHA.HASHDataToString(sk + token);
            BitSet bitSet = new BitSet(bitsetLen);

            int key0_path = attachPathToKey(token, encPath);
            byte[] key1_path = getDecKey(encToken, encPath);

            for (int v : entry.getValue()) {
                bitSet.set(v, true);
            }

            bitSet = bitsetEncOrDec(bitSet, key1_path);
            encryptedBitsets.put(key0_path, bitSet);
        }

    }

    public void extend(String encPath, List<DataPoint[]> chdRanges, List<Integer> chdIndex) {
        bitsetLen = (int) (this.getM() * bitsetSizeRate);
        encryptedBitsets = new HashMap<>();
        // map tokens to filters, and add filters to hash map
        int len = chdIndex.size();

        HashMap<String, List<Integer>> mp = new HashMap<>();
        for (int i = 0; i < len; i++) {

            if (chdRanges.get(i) == null) {
                continue;
            }

            // get each token
            for (int j = 0; j < 2; ++j) {
                List<String> encryptedLeftData;
                List<String> encryptedRightData;

                encryptedLeftData = SDC.encrypt(sk, j, TYPE_LEFT, chdRanges.get(i)[0].data[j]);
                encryptedRightData = SDC.encrypt(sk, j, TYPE_RIGHT, chdRanges.get(i)[1].data[j]);


                for (String leftToken : encryptedLeftData) {
                    mp.computeIfAbsent(leftToken, k -> new ArrayList<>()).add(chdIndex.get(i));
                }
                for (String rightToken : encryptedRightData) {
                    mp.computeIfAbsent(rightToken, k -> new ArrayList<>()).add(chdIndex.get(i));
                }
            }
        }

//        node.dataList = null;

        bitsetLen = (int) (len * bitsetSizeRate);
        // map tokens to filters, and add filters to hash map
        for (Map.Entry<String, List<Integer>> entry : mp.entrySet()) {
            String token = entry.getKey();
            String encToken = SHA.HASHDataToString(sk + token);
            BitSet bitSet = new BitSet(bitsetLen);

            int key0_path = attachPathToKey(token, encPath);
            byte[] key1_path = getDecKey(encToken, encPath);

            for (int v : entry.getValue()) {
                bitSet.set(v, true);
            }

            bitSet = bitsetEncOrDec(bitSet, key1_path);
            encryptedBitsets.put(key0_path, bitSet);
        }

    }


    public void setIds(PartNode node) {
        int len = node.dataList.size();
        ids = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            ids.add(node.dataList.get(i).id);
        }
    }

    public long flipRange(DataPoint[] chdRange, int index, String encPath) {
        long deltaCost = 0;
        List<String> encode = new ArrayList<>();
        encode.addAll(SDC.encrypt(sk, 0, TYPE_LEFT, chdRange[0].data[0]));
        encode.addAll(SDC.encrypt(sk, 1, TYPE_LEFT, chdRange[0].data[1]));
        encode.addAll(SDC.encrypt(sk, 0, TYPE_RIGHT, chdRange[1].data[0]));
        encode.addAll(SDC.encrypt(sk, 1, TYPE_RIGHT, chdRange[1].data[1]));

        for (String s : encode) {

            String encToken = SHA.HASHDataToString(sk + s);
            int key0_path = attachPathToKey(s, encPath);
            BitSet encBitset = encryptedBitsets.getOrDefault(key0_path, null);
            if (encBitset != null) {
                encBitset.flip(index);
            } else {
                encBitset = new BitSet(bitsetLen);
                encBitset.flip(index);
                byte[] key1_path = getDecKey(encToken, encPath);
                encBitset = bitsetEncOrDec(encBitset, key1_path);
                encryptedBitsets.put(key0_path, encBitset);
                deltaCost += 64 + (bitsetLen + 63) / 64 * 64;
            }
        }

        return deltaCost * STO_W;
    }

    public long updateSubRangeByFlip(DataPoint[] preRange, DataPoint[] updatedRange, int index, String encPath) {
        long deltaCost = 0;
        List<String> encode = new ArrayList<>();
        encode.addAll(SDC.encrypt(sk, 0, TYPE_LEFT, preRange[0].data[0]));
        encode.addAll(SDC.encrypt(sk, 1, TYPE_LEFT, preRange[0].data[1]));
        encode.addAll(SDC.encrypt(sk, 0, TYPE_RIGHT, preRange[1].data[0]));
        encode.addAll(SDC.encrypt(sk, 1, TYPE_RIGHT, preRange[1].data[1]));
        encode.addAll(SDC.encrypt(sk, 0, TYPE_LEFT, updatedRange[0].data[0]));
        encode.addAll(SDC.encrypt(sk, 1, TYPE_LEFT, updatedRange[0].data[1]));
        encode.addAll(SDC.encrypt(sk, 0, TYPE_RIGHT, updatedRange[1].data[0]));
        encode.addAll(SDC.encrypt(sk, 1, TYPE_RIGHT, updatedRange[1].data[1]));

        for (String s : encode) {

            String encToken = SHA.HASHDataToString(sk + s);
            int key0_path = attachPathToKey(s, encPath);
            BitSet encBitset = encryptedBitsets.getOrDefault(key0_path, null);
            if (encBitset != null) {
                encBitset.flip(index);
            } else {
                encBitset = new BitSet(bitsetLen);
                encBitset.flip(index);
                byte[] key1_path = getDecKey(encToken, encPath);
                encBitset = bitsetEncOrDec(encBitset, key1_path);
                encryptedBitsets.put(key0_path, encBitset);
                deltaCost += 64 + (bitsetLen + 63) / 64 * 64;
            }

        }
        return deltaCost * STO_W;
    }

    public boolean isFull() {

        int m = ids == null ? chd.size() : ids.size();
        return m >= bitsetLen;
    }

    public long addData(DataPoint dataPoint, String encPath) {
        long deltaCost = 0;

        int p = ids.size();
        ids.add(p);
        List<String> encode = new ArrayList<>();
        encode.addAll(SDC.encrypt(sk, 0, TYPE_LEFT, dataPoint.data[0]));
        encode.addAll(SDC.encrypt(sk, 1, TYPE_LEFT, dataPoint.data[1]));
        encode.addAll(SDC.encrypt(sk, 0, TYPE_RIGHT, dataPoint.data[0]));
        encode.addAll(SDC.encrypt(sk, 1, TYPE_RIGHT, dataPoint.data[1]));

        for (String s : encode) {

            String encToken = SHA.HASHDataToString(sk + s);
            int key0_path = attachPathToKey(s, encPath);
            BitSet encBitset = encryptedBitsets.getOrDefault(key0_path, null);
            if (encBitset != null) {
                encBitset.flip(p);
            } else {
                encBitset = new BitSet(bitsetLen);
                encBitset.flip(p);
                byte[] key1_path = getDecKey(encToken, encPath);
                encBitset = bitsetEncOrDec(encBitset, key1_path);
                encryptedBitsets.put(key0_path, encBitset);
                deltaCost += 64 + (bitsetLen + 63) / 64 * 64;
            }

        }

        return deltaCost * STO_W;
    }


    public void encryptToBitset(PartNode node, String path) {
        int len;
        if (node.isLeafNode()) len = node.dataList.size();
        else len = node.getChdSize();

        HashMap<String, List<Integer>> mp = new HashMap<>();
        for (int i = 0; i < len; i++) {
            // get each token
            for (int j = 0; j < 2; ++j) {
                List<String> encryptedLeftData;
                List<String> encryptedRightData;
                if (node.isLeafNode()) {
                    encryptedLeftData = SDC.encrypt(sk, j, TYPE_LEFT, node.dataList.get(i).data[j]);
                    encryptedRightData = SDC.encrypt(sk, j, TYPE_RIGHT, node.dataList.get(i).data[j]);
                } else {
                    encryptedLeftData = SDC.encrypt(sk, j, TYPE_LEFT, node.getChdNode(i).range[0].data[j]);
                    encryptedRightData = SDC.encrypt(sk, j, TYPE_RIGHT, node.getChdNode(i).range[1].data[j]);
                }

                for (String leftToken : encryptedLeftData) {
                    mp.computeIfAbsent(leftToken, k -> new ArrayList<>()).add(i);
                }
                for (String rightToken : encryptedRightData) {
                    mp.computeIfAbsent(rightToken, k -> new ArrayList<>()).add(i);
                }
            }
        }

//        node.dataList = null;

        bitsetLen = (int) (len * bitsetSizeRate);
        // map tokens to filters, and add filters to hash map
        for (Map.Entry<String, List<Integer>> entry : mp.entrySet()) {
            String token = entry.getKey();
            String encToken = SHA.HASHDataToString(sk + token);
            BitSet bitSet = new BitSet(bitsetLen);

            int key0_path = attachPathToKey(token, path);
            byte[] key1_path = getDecKey(encToken, path);

            for (int v : entry.getValue()) {
                bitSet.set(v, true);
            }

            bitSet = bitsetEncOrDec(bitSet, key1_path);
            encryptedBitsets.put(key0_path, bitSet);
        }

        node.WBTreeNode = this;
    }


    public static int attachPathToKey(String str, String path) {

        return (str + path).hashCode();
    }

    public static byte[] getDecKey(String str, String path) {
        return SHA.hashToBytes(str + path);
    }

    public static BitSet bitsetEncOrDec(BitSet bitSet, byte[] key) {
        BitSet result = (BitSet) bitSet.clone();
        result.xor(BitSet.valueOf(key));
        return result;
    }

    public BitSet decToken(int str, byte[] decKey) {
        BitSet tarBitSet = encryptedBitsets.getOrDefault(str, null);
        if (tarBitSet == null) {
            return new BitSet(ids == null ? chd.size() : ids.size());
        }
        return bitsetEncOrDec(tarBitSet, decKey);
    }


    public static void main(String[] args) {
        byte[] x = SHA.hashToBytes("abc");
        BitSet bitSet = new BitSet(10);
        int[] t = new int[]{1,3,5,7};
        for (int l : t) {
            bitSet.set(l, true);
        }

        for (int i = 0; i < bitSet.size(); ++i) {
            System.out.print((bitSet.get(i) ? 1 : 0) + ", ");
        }
        System.out.println();

        bitSet = bitsetEncOrDec(bitSet, x);
        for (int i = 0; i < bitSet.size(); ++i) {
            System.out.print((bitSet.get(i) ? 1 : 0) + ", ");
        }
        System.out.println();

        bitSet = bitsetEncOrDec(bitSet, x);
        for (int i = 0; i < bitSet.size(); ++i) {
            System.out.print((bitSet.get(i) ? 1 : 0) + ", ");
        }
    }
}
