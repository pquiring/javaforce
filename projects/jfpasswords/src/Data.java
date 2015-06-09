/**
 * Created : Jun 7, 2012
 *
 * @author pquiring
 */

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.*;

import javaforce.*;

public class Data {

  private static final String ALGO = "AES";
  private static byte[] keyValue;

  public static byte[] encrypt(byte[] data) throws Exception {
    Key key = generateKey();
    Cipher c = Cipher.getInstance(ALGO);
    c.init(Cipher.ENCRYPT_MODE, key);
    return c.doFinal(data);
  }

  public static byte[] decrypt(byte[] data) throws Exception {
    Key key = generateKey();
    Cipher c = Cipher.getInstance(ALGO);
    c.init(Cipher.DECRYPT_MODE, key);
    return c.doFinal(data);
  }

  public static void setPassword(String pass) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      keyValue = Arrays.copyOfRange(md.digest(pass.getBytes()),0,16);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static Key generateKey() throws Exception {
    Key key = new SecretKeySpec(keyValue, ALGO);
    return key;
  }
}
