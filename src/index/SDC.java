package index;

import utils.SHA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SDC {

    static HashMap<String, List<String>> messageEncrypted = new HashMap<>();
    static HashMap<String, List<String>> messageKeyGen = new HashMap<>();


    public static void clear() {
        messageEncrypted = new HashMap<>();
        messageKeyGen = new HashMap<>();
    }

    public static int maxBitLen = 11;

    public static int symXor(int A, int B) {
        return A ^ B;
    }

    public static void setMaxBitLen (int maxBitLen){
        SDC.maxBitLen = maxBitLen;
    }

//    public static HashMap<String, Integer> encryptLeafNode (int id, String sk, int m, String tag) {
//        HashMap<String, Integer> mp = new HashMap<>();
//        for (int i = 0; i < maxBitLen; i++) {
//            int b = m % 2;
//            if (b == 0) {
//                String u = SHA.HASHDataToString(sk + i + m);
//                int v = symXor(id, u.hashCode());
//                String e = SHA.HASHDataToString(tag + u);
//                mp.put(e, v);
//            }
//            m >>= 1;
//        }
//        return mp;
//    }

    public static int getEncodingSize(int m) {
        int sum = 0;
        for (int i = 0; i < maxBitLen; i++) {
            int b = m % 2;
            if (b == 0) {
                sum++;
            }
            m >>= 1;
        }
        return sum;
    }

    public static List<String> tokenKeyGen(String sk, List<String> tokens) {
        List<String> encToken = new ArrayList<>();
        for (String token : tokens) {
            encToken.add(SHA.HASHDataToString(sk + token));
        }
        return encToken;
    }


    public static List<String> encrypt(String sk, int dimension, String type, int m) {
        String theM = m + type + dimension;
        if (messageEncrypted.containsKey(theM)) {
            return messageEncrypted.get(theM);
        }

        List<String> list = new ArrayList<>();
        for (int i = 0; i < maxBitLen; i++) {
            int b = m % 2;
            if (b == 0) {
                String s = sk + i + m + type + dimension;
                String u = SHA.HASHDataToString(s);
                list.add(u);
            }
            m >>= 1;
        }
        messageEncrypted.put(theM, list);
        return list;
    }

    public static List<String> keyGen(String sk, int dimension, String type, int m) {
        String theM = m + type + dimension;
        if (messageKeyGen.containsKey(theM)) {
            return messageKeyGen.get(theM);
        }

        List<String> list = new ArrayList<>();
        for (int i = 0; i < maxBitLen; i++) {
            int b = m % 2;
            if (b == 1) {
                String s = sk + i + (m - 1) + type + dimension;
                String t = SHA.HASHDataToString(s);
                list.add(t);
            }
            m >>= 1;
        }
        messageKeyGen.put(theM, list);
        return list;
    }

    public static String query(String m, String tag) {
        return SHA.HASHDataToString(tag + m);
    }


    public static void main(String[] args) {
//        int n = 1000;
//        for (int i = 0; i < n; ++i) {
//            int x = (int) (Math.random() * n);
//            int y = (int) (Math.random() * n);
//            x= 1; y = 1;
//            List<String> encrypt = encrypt("", x, "1");
//            HashSet<String> st = new HashSet<>();
//            st.addAll(encrypt);
//            List<String> keyGen = keyGen(k, y);
//            boolean find = false;
//            for (int j = 0; j < keyGen.size(); ++j) {
//                String query = query(keyGen.get(j), "1");
//                if (st.contains(query)) {
//                    find = true;
//                    break;
//                }
//            }
//            if (find && !(y > x)) {
//                System.out.printf("false");
//            }
//            if (!find && !(y <= x)) {
//                System.out.printf("false");
//            }
//        }
    }

}
