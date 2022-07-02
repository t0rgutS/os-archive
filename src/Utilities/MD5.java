package Utilities;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {
    public static String hash(String arg){
        try{
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(arg.getBytes());
            byte[] hashed = digest.digest();
            StringBuffer res = new StringBuffer();
            res.append((new BigInteger(1, hashed)).toString(16));
            while(res.length() < 32)
                res.insert(0, "0");
            //System.out.println(res.toString());
           // System.out.println(res.toString().length());
            return res.toString();
        } catch (NoSuchAlgorithmException nsae){
            nsae.printStackTrace();
        }
        return "";
    }
}
