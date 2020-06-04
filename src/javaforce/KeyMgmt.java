package javaforce;

/**
 * Key Management API - just some members to load OpenSSL cert.
 *
 * @author pquiring
 *
 * Created : Oct 8, 2013
 */

import java.security.*;
import java.security.spec.*;
import java.security.cert.*;
import java.io.*;
import java.util.*;

public class KeyMgmt {

  private KeyStore keyStore = null;

  /** Executes keytool directly */
  public static boolean keytool(String[] args) {
    ArrayList<String> cmd = new ArrayList<String>();
    try {
      if (JF.isWindows()) {
        cmd.add(System.getProperty("java.home") + "\\bin\\keytool.exe");
      } else {
        cmd.add(System.getProperty("java.home") + "/bin/keytool");
      }
      for(int a=0;a<args.length;a++) {
        cmd.add(args[a]);
      }
      String[] sa = cmd.toArray(new String[cmd.size()]);
/*
      System.out.print("cmd=");
      for(int a=0;a<sa.length;a++) {
        System.out.print(sa[a] + " ");
      }
*/
      Process p = Runtime.getRuntime().exec(sa);
      p.waitFor();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /**
   * Open an existing keystore (Note: use null for InputStream to create a blank keystore)
   */
  public boolean open(InputStream is, char[] pwd) {
    try {
      keyStore = KeyStore.getInstance("JKS", "SUN");
      keyStore.load(is, pwd);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean save(OutputStream os, char[] pwd) {
    try {
      keyStore.store(os, pwd);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean loadKEYandCRT(String alias, InputStream keyStream, InputStream certStream, char[] pwd) {
    try {
      // loading Key
      KeyFactory kf = KeyFactory.getInstance("RSA");
      byte[] key = JF.readAll(keyStream);
      PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(key);
      PrivateKey ff = kf.generatePrivate(keysp);

      // loading CertificateChain
      CertificateFactory cf = CertificateFactory.getInstance("X.509");

      Collection c = cf.generateCertificates(certStream);
      java.security.cert.Certificate[] certs;

      certs = (java.security.cert.Certificate[]) c.toArray();

      // set key / cert pair
      keyStore.setKeyEntry(alias, ff, pwd, certs);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean loadCRT(String alias, InputStream certStream) {
    try {
      // loading CertificateChain
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      java.security.cert.Certificate crt = cf.generateCertificate(certStream);

      // set one cert
      keyStore.setCertificateEntry(alias, crt);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean hasCRT(InputStream certStream) {
    try {
      // loading CertificateChain
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      java.security.cert.Certificate crt = cf.generateCertificate(certStream);

      return keyStore.getCertificateAlias(crt) != null;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public java.security.cert.Certificate getCRT(String alias) {
    try {
      return keyStore.getCertificate(alias);
    } catch (Exception e) {
      return null;
    }
  }

  public java.security.Key getKEY(String alias, char[] password) {
    try {
      return keyStore.getKey(alias, password);
    } catch (Exception e) {
      return null;
    }
  }

  public static String fingerprintSHA256(byte[] key) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] res = md.digest(key);
      StringBuilder sb = new StringBuilder();
      for(int a=0;a<res.length;a++) {
        int b = ((int)res[a]) & 0xff;
        if (a > 0) sb.append(":");
        if (b < 16) sb.append("0");
        sb.append(Integer.toString(b, 16).toUpperCase());
      }
      return sb.toString();
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }
}
