package javaforce;

/**
 * Key Management class.
 *
 * Contains a Java keystore and password.
 *
 * crt = public certificate
 * key = private key (password protected)
 * csr = PKCS10 cert sign request
 *
 * @author pquiring
 *
 * Created : Oct 8, 2013
 */

import java.security.CodeSigner;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.Timestamp;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.Principal;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CRL;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.io.*;
import java.util.*;

public class KeyMgmt {

  private static boolean keytool = true;  //use keytool

  private KeyStore keyStore = null;
  private char[] password = null;
  private String keyfile = "keystore.ks";

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

  public KeyMgmt() {
    try {
      keyStore = KeyStore.getInstance("JKS", "SUN");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /**
   * Open a keystore
   */
  public boolean open(String file, char[] pwd) {
    password = pwd;
    try {
      keyStore = KeyStore.getInstance("JKS", "SUN");
      FileInputStream fis = new FileInputStream(file);
      keyStore.load(fis, pwd);
      fis.close();
      keyfile = file;
      return true;
    } catch (Exception e) {
      keyStore = null;
      password = null;
      JFLog.log(e);
      return false;
    }
  }

  /**
   * Open a keystore
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

  /** Save keystore. */
  public boolean save() {
    try {
      if (keyfile == null) keyfile = "keystore.ks";
      FileOutputStream fos = new FileOutputStream(keyfile);
      keyStore.store(fos, password);
      fos.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Save keystore. */
  public boolean save(OutputStream os) {
    try {
      if (keyfile == null) keyfile = "keystore.ks";
      keyStore.store(os, password);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Save keystore with a new keystore password. */
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
    if (keytool) {
      KeyMgmt.keytool(new String[] {
        "-genkey",
        "-alias", alias,
        "-keypass", password,
        "-storepass", password,
        "-keystore", file,
        "-validity", "3650",
        "-dname", dname,
        "-keyalg", "RSA",
        "-keysize", "2048"
      });
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
    } else {
      try {
        KeyMgmt keys = new KeyMgmt();
        keys.keyStore = KeyStore.getInstance("JKS", "SUN");
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        PrivateKey prikey = pair.getPrivate();
        PublicKey pubkey = pair.getPublic();
        //TODO : convert pubkey to self signed certificate
        keys.keyStore.setKeyEntry(alias, prikey, password.toCharArray(), new Certificate[] {null /* cert chain */});
        return keys;
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    }
  }

  /** Creates new private/public keys (valid for 10 years) and sign by existing key/pair.
   *
   * @param alias = new key pair alias
   * @param dname = distinguished name, ie: CN=javaforce.sourceforge.net, O=server, OU=webserver, C=CA
   * @param password = keystore/key password
   * @param signerAlias = existing keypair to sign with
   * @param signerPassword = signer key password
   */
  public KeyMgmt create(String alias, String dname, String password, String signerAlias, String signerPassword) {
    if (keytool) {
      save();
      KeyMgmt.keytool(new String[] {
        "-genkey",
        "-alias", alias,
        "-keypass", password,
        "-storepass", password,
        "-keystore", keyfile,
        "-validity", "3650",
        "-dname", dname,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-signer", signerAlias,
        "-signerkeypass", signerPassword
      });
      try {
        FileInputStream fis = new FileInputStream(keyfile);
        KeyMgmt keys = new KeyMgmt();
        keys.open(fis, password.toCharArray());
        fis.close();
        return keys;
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    } else {
      try {
        KeyMgmt keys = new KeyMgmt();
        keys.keyStore = KeyStore.getInstance("JKS", "SUN");
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        PrivateKey prikey = pair.getPrivate();
        PublicKey pubkey = pair.getPublic();
        //TODO : convert pubkey to self signed certificate
        //unfortunately keytool uses a lot of private api
        keys.keyStore.setKeyEntry(alias, prikey, password.toCharArray(), new Certificate[] {null /* cert */});
        return keys;
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    }
  }

  /** Create sign request. */
  public boolean createSignRequest(String alias, String out_file_csr) {
    String password = getPasswordString();
    save();
    KeyMgmt.keytool(new String[] {
      "-certreq",
      "-keystore", keyfile,
      "-alias", alias,
      "-keypass", password,
      "-storepass", password,
      "-keyalg" , "RSA",
      "-keysize", "2048",
      "-file", out_file_csr
    });
    return false;
  }

  /** Sign cert request. */
  public boolean signRequest(String alias, String dname, String in_file_csr, String out_file_crt) {
    String password = getPasswordString();
    save();
    KeyMgmt.keytool(new String[] {
      "-gencert",
      "-alias", alias,
      "-keypass", password,
      "-storepass", password,
      "-keystore", keyfile,
      "-validity", "3650",
      "-dname", dname,
      "-sigalg", "RSA",
      "-keysize", "2048",
      "-infile", in_file_csr,
      "-outfile", out_file_crt
    });
    return false;
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
      Certificate[] certs;

      certs = (Certificate[]) c.toArray();

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
      Certificate crt = cf.generateCertificate(certStream);

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
      Certificate crt = cf.generateCertificate(certStream);

      return keyStore.getCertificateAlias(crt) != null;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Get public certificate. */
  public Certificate getCRT(String alias) {
    try {
      return keyStore.getCertificate(alias);
    } catch (Exception e) {
      return null;
    }
  }

  /** Get private key w/ password. */
  public Key getKEY(String alias, char[] pwd) {
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

  public String getPasswordString() {
    return new String(password);
  }
}
