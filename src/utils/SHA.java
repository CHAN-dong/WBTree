
package utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/*
SHA(Secure Hash Algorithm，安全散列算法），数字签名等密码学应用中重要的工具，
被广泛地应用于电子商务等信息安全领域。虽然，SHA与MD5通过碰撞法都被破解了，
但是SHA仍然是公认的安全加密算法，较之MD5更为安全
*/
public class SHA {
//    public static final String KEY_SHA = "MD5";
    public static final String KEY_SHA = "SHA-256";

    //一个将字符串转换成32长的数字

    public static BigInteger hashToBig(String inputStr){
        BigInteger sha = null;
        try{
            //核心代码，调用java库实现的部分
            MessageDigest messageDigest = MessageDigest.getInstance(KEY_SHA); //确定计算方法
            messageDigest.update(inputStr.getBytes());//字节型
            byte[] digest = messageDigest.digest();
            sha = new BigInteger(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sha;
    }

    public static byte[] hashToBytes(String inputStr){
        byte[] sha = null;
        try{
            //核心代码，调用java库实现的部分
            MessageDigest messageDigest = MessageDigest.getInstance(KEY_SHA); //确定计算方法
            messageDigest.update(inputStr.getBytes());//字节型
            sha = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sha;
    }

    public static byte[] bytesXor(byte[] bs1, byte[] bs2){
        byte[] xor = new byte[32];
        for (int i = 0; i < 32; ++i) {
            xor[i] = (byte) (bs1[i] ^ bs2[i]);
        }
        return xor;
    }


    public static String PRF(String inputStr){
        StringBuilder sha = new StringBuilder();
        try{
            //核心代码，调用java库实现的部分
            MessageDigest messageDigest = MessageDigest.getInstance(KEY_SHA); //确定计算方法
            messageDigest.update(inputStr.getBytes());//字节型
            byte[] digest = messageDigest.digest();
            for (int i = 0; i < digest.length / 2; ++i) {
                byte b = digest[i];
                int v = b & 0xFF;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) sha.append(0);
                sha.append(hv);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sha.toString();
    }


        public static String HASHDataToString(String inputStr){
            byte[] digest = null;

            //System.out.println("原始数据:"+inputStr);
            try{
                //核心代码，调用java库实现的部分
                MessageDigest messageDigest = MessageDigest.getInstance(KEY_SHA); //确定计算方法
                messageDigest.update(inputStr.getBytes());//字节型
                digest = messageDigest.digest();
//                System.out.println("SHA值:" + sha.toString(2));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return new String(digest);
        }

    public static void main(String args[]){



        long startTime, endTime;
        String s1 = "19";//
        String s2 = "124";

        BigInteger b1 = SHA.hashToBig(s1);
        BigInteger b2 = SHA.hashToBig(s2);

        startTime = System.nanoTime();
        BigInteger xor1 = b1.xor(b2);
        endTime = System.nanoTime();
        System.out.println(xor1.toString() + " ::" + (endTime - startTime));


        byte[] bs1 = SHA.hashToBytes(s1);
        byte[] bs2 = SHA.hashToBytes(s2);
        startTime = System.nanoTime();
        byte[] xor2 = bytesXor(bs1, bs2);
        endTime = System.nanoTime();
        byte[] bytes = bytesXor(xor2, bs2);
        System.out.println(endTime - startTime);
    }
}

