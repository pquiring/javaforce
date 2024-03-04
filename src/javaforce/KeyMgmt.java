package javaforce;

/**
 * Key Management class.
 *
 * Contains a Java keystore and password.
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
  private char[] password = null;

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
      InputStream is = p.getInputStream();
      p.waitFor();
      String output = new String(is.readAllBytes());
      JFLog.log("KeyMgmt.keyTool()=" + output);
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
    password = pwd;
    try {
      keyStore = KeyStore.getInstance("JKS", "SUN");
      keyStore.load(is, pwd);
      return true;
    } catch (Exception e) {
      keyStore = null;
      password = null;
      JFLog.log(e);
      return false;
    }
  }

  public boolean save(OutputStream os) {
    try {
      keyStore.store(os, password);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean save(OutputStream os, char[] pwd) {
    this.password = pwd;
    try {
      keyStore.store(os, pwd);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Creates new keystore with private/public keys (valid for 10 years).
   *
   * @param file = file name
   * @param alias = key pair alias
   * @param dname = distinguished name, ie: CN=javaforce.sourceforge.net, O=server, OU=webserver, C=CA
   * @param password = keystore/key password, required for open()
   */
  public static KeyMgmt create(String file, String alias, String dname, String password) {
    KeyMgmt.keytool(new String[] {
      "-genkey", "-debug", "-alias", alias, "-keypass", password, "-storepass", password,
      "-keystore", file, "-validity", "3650", "-dname", dname,
      "-keyalg" , "RSA", "-keysize", "2048"}
    );
    try {
      FileInputStream fis = new FileInputStream(file);
      KeyMgmt keys = new KeyMgmt();
      keys.open(fis, password.toCharArray());
      fis.close();
      return keys;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
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
      keyStore = KeyStore.getInstance("JKS", "SUN");
      keyStore.setKeyEntry(alias, ff, pwd, certs);
      password = pwd;
      return true;
    } catch (Exception e) {
      keyStore = null;
      password = null;
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

  public java.security.Key getKEY(String alias, char[] pwd) {
    try {
      return keyStore.getKey(alias, pwd);
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

  public boolean isValid() {
    return keyStore != null && password != null;
  }

  public KeyStore getKeyStore() {
    return keyStore;
  }

  public char[] getPassword() {
    return password;
  }
}
