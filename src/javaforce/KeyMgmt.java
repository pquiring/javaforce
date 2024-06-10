package javaforce;

/**
 * Key Management.
 *
 * Contains a keystore and its password.
 *
 * key = private key (password protected)
 * crt = public certificate
 * csr = PKCS10 cert sign request
 *
 * see https://docs.oracle.com/en/java/javase/17/docs/specs/man/keytool.html
 *
 * @author pquiring
 *
 * Created : Oct 8, 2013
 */

import java.security.CodeSigner;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.Timestamp;
import java.security.Principal;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CRL;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.io.*;
import java.util.*;

public class KeyMgmt {

  private static boolean keytool = true;  //use keytool

  private KeyStore keyStore = null;
  private char[] storePass = new char[0];
  private String keyFile = "keystore.ks";
  private String root = "root";

  private static String keyStoreType = "PKCS12";
  private static String keyStoreProvider = "SUN";

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
      open(null);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public KeyMgmt(String keyfile, String storepass) {
    try {
      this.keyFile = keyfile;
      this.storePass = storepass.toCharArray();
      open();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void setFile(String keyfile) {
    this.keyFile = keyfile;
  }

  public void setKeyStorePass(String storepass) {
    this.storePass = storepass.toCharArray();
  }

  public void setRootAlias(String alias) {
    root = alias;
  }

  /** Sets default keystore type and provider.
   * Default = PKCS12.
   */
  public static void setKeyStoreType(String type, String provider) {
    keyStoreType = type;
    keyStoreProvider = provider;
  }

  /** Create an empty keystore and save it. */
  public void create() {
    open(null);
    save();
  }

  /** Create an empty keystore and save it. */
  public void create(String file, String password) {
    keyFile = file;
    storePass = password.toCharArray();
    open(null);
    save();
  }

  /**
   * Open a keystore
   */
  public boolean open() {
    try {
      keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
      FileInputStream fis = new FileInputStream(keyFile);
      keyStore.load(fis, storePass);
      fis.close();
      return true;
    } catch (Exception e) {
      keyStore = null;
      storePass = null;
      JFLog.log(e);
      return false;
    }
  }

  /**
   * Open a keystore
   */
  public boolean open(String file, String keystorepass) {
    storePass = keystorepass.toCharArray();
    try {
      keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
      FileInputStream fis = new FileInputStream(file);
      keyStore.load(fis, keystorepass.toCharArray());
      fis.close();
      keyFile = file;
      return true;
    } catch (Exception e) {
      keyStore = null;
      storePass = null;
      JFLog.log(e);
      return false;
    }
  }

  /**
   * Open a keystore
   */
  public boolean open(InputStream is, String keystorepass) {
    storePass = keystorepass.toCharArray();
    try {
      keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
      keyStore.load(is, storePass);
      return true;
    } catch (Exception e) {
      keyStore = null;
      storePass = null;
      JFLog.log(e);
      return false;
    }
  }

  /**
   * Open a keystore
   */
  public boolean open(InputStream is) {
    try {
      keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
      keyStore.load(is, storePass);
      return true;
    } catch (Exception e) {
      keyStore = null;
      storePass = null;
      JFLog.log(e);
      return false;
    }
  }

  /** Save keystore. */
  public boolean save() {
    try {
      if (keyFile == null) keyFile = "keystore.ks";
      FileOutputStream fos = new FileOutputStream(keyFile);
      keyStore.store(fos, storePass);
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
      if (keyFile == null) keyFile = "keystore.ks";
      keyStore.store(os, storePass);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Save keystore with a new keystorepass. */
  public boolean save(OutputStream os, String keystorepass) {
    this.storePass = keystorepass.toCharArray();
    try {
      keyStore.store(os, storePass);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static KeyMgmt getDefaultClient() {
    String file = JF.getUserPath() + "/.javaforce-client.ks";
    if (new File(file).exists()) {
      return new KeyMgmt(file, "password");
    } else {
      KeyParams params = new KeyParams();
      params.dname = "CN=javaforce.sourceforge.net, O=server, OU=webserver, C=CA";
      return create(file, "password", "client", params, "password");
    }
  }

  /** Creates new keystore with private/public keys.
   *
   * @param storefile = file name
   * @param storepass = keystorepass
   * @param alias = key pair alias
   * @param dname = distinguished name, ie: CN=javaforce.sourceforge.net, O=server, OU=webserver, C=CA
   * @param keypass = keystore and keypass
   */
  public static KeyMgmt create(String storefile, String storepass, String alias, KeyParams params, String keypass) {
    if (keytool) {
      ArrayList<String> cmd = new ArrayList<>();
      cmd.add("-genkey");
      cmd.add("-keystore");
      cmd.add(storefile);
      cmd.add("-storepass");
      cmd.add(storepass);
      cmd.add("-keypass");
      cmd.add(keypass);
      cmd.add("-alias");
      cmd.add(alias);
      cmd.add("-validity");
      cmd.add(params.validity);
      cmd.add("-dname");
      cmd.add(params.dname);
      cmd.add("-keyalg");
      cmd.add("RSA");
      cmd.add("-keysize");
      cmd.add("2048");
      if (params.exts != null) {
        for(String ext : params.exts) {
          cmd.add("-ext");
          cmd.add(ext);
        }
      }
      keytool(cmd.toArray(JF.StringArrayType));
      try {
        FileInputStream fis = new FileInputStream(storefile);
        KeyMgmt keys = new KeyMgmt();
        keys.open(fis, keypass);
        fis.close();
        return keys;
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    } else {
      try {
        KeyMgmt keys = new KeyMgmt();
        keys.keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        PrivateKey prikey = pair.getPrivate();
        PublicKey pubkey = pair.getPublic();
        //TODO : convert pubkey to self signed certificate
        keys.keyStore.setKeyEntry(alias, prikey, keypass.toCharArray(), new Certificate[] {null /* cert chain */});
        return keys;
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    }
  }

  /** Creates new private/public keys.
   *
   * @param alias = new key pair alias
   * @param dname = distinguished name, ie: CN=javaforce.sourceforge.net, O=server, OU=webserver, C=CA
   * @param keypass = keypass
   */
  public boolean create(String alias, KeyParams params, String keypass) {
    if (keytool) {
      save();
      ArrayList<String> cmd = new ArrayList<>();
      cmd.add("-genkey");
      cmd.add("-alias");
      cmd.add(alias);
      cmd.add("-keypass");
      cmd.add(keypass);
      cmd.add("-storepass");
      cmd.add(getKeyStorePass());
      cmd.add("-keystore");
      cmd.add(keyFile);
      cmd.add("-validity");
      cmd.add(params.validity);
      cmd.add("-dname");
      cmd.add(params.dname);
      cmd.add("-keyalg");
      cmd.add("RSA");
      cmd.add("-keysize");
      cmd.add("2048");
      if (params.exts != null) {
        for(String ext : params.exts) {
          cmd.add("-ext");
          cmd.add(ext);
        }
      }
      keytool(cmd.toArray(JF.StringArrayType));
      try {
        FileInputStream fis = new FileInputStream(keyFile);
        KeyMgmt keys = new KeyMgmt();
        keys.open(fis, keypass);
        fis.close();
        return true;
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    } else {
      try {
        KeyMgmt keys = new KeyMgmt();
        keys.keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        PrivateKey prikey = pair.getPrivate();
        PublicKey pubkey = pair.getPublic();
        //TODO : convert pubkey to self signed certificate
        //unfortunately keytool uses a lot of private api
        keys.keyStore.setKeyEntry(alias, prikey, keypass.toCharArray(), new Certificate[] {null /* cert */});
        return true;
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }
  }

  /** Creates new private/public keys and sign by existing key/pair.
   *
   * @param alias = new key pair alias
   * @param dname = distinguished name, ie: CN=javaforce.sourceforge.net, O=server, OU=webserver, C=CA
   * @param keypass = key password
   * @param signerAlias = existing key to sign with
   * @param signerKeyPass = signer key password
   */
  public boolean create(String alias, KeyParams params, String keypass, String signerAlias, String signerKeyPass) {
    if (keytool) {
      save();
      ArrayList<String> cmd = new ArrayList<>();
      cmd.add("-genkey");
      cmd.add("-alias");
      cmd.add(alias);
      cmd.add("-keypass");
      cmd.add(keypass);
      cmd.add("-storepass");
      cmd.add(getKeyStorePass());
      cmd.add("-keystore");
      cmd.add(keyFile);
      cmd.add("-validity");
      cmd.add(params.validity);
      cmd.add("-dname");
      cmd.add(params.dname);
      cmd.add("-keyalg");
      cmd.add("RSA");
      cmd.add("-keysize");
      cmd.add("2048");
      cmd.add("-signer");
      cmd.add(signerAlias);
      cmd.add("-signerkeypass");
      cmd.add(signerKeyPass);
      if (params.exts != null) {
        for(String ext : params.exts) {
          cmd.add("-ext");
          cmd.add(ext);
        }
      }
      keytool(cmd.toArray(JF.StringArrayType));
      try {
        FileInputStream fis = new FileInputStream(keyFile);
        KeyMgmt keys = new KeyMgmt();
        keys.open(fis, keypass);
        fis.close();
        return true;
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    } else {
      try {
        KeyMgmt keys = new KeyMgmt();
        keys.keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        PrivateKey prikey = pair.getPrivate();
        PublicKey pubkey = pair.getPublic();
        //TODO : convert pubkey to self signed certificate
        //unfortunately keytool uses a lot of private api
        keys.keyStore.setKeyEntry(alias, prikey, keypass.toCharArray(), new Certificate[] {null /* cert */});
        return true;
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }
  }

  /** Create cert sign request (csr). */
  public boolean createCertSignRequest(String alias, String keypass, String out_file_csr) {
    save();

    ArrayList<String> cmd = new ArrayList<>();
    cmd.add("-certreq");
    cmd.add("-alias");
    cmd.add(alias);
    cmd.add("-keypass");
    cmd.add(getKeyStorePass());
    cmd.add("-storepass");
    cmd.add(keypass);
    cmd.add("-keystore");
    cmd.add(keyFile);
    cmd.add("-keyalg");
    cmd.add("RSA");
    cmd.add("-keysize");
    cmd.add("2048");
    cmd.add("-file");
    cmd.add(out_file_csr);

    keytool(cmd.toArray(JF.StringArrayType));

    return true;
  }

  /** Sign cert request. */
  public boolean signRequest(String alias, String keypass, String in_file_csr, String out_file_crt) {
    save();
    ArrayList<String> cmd = new ArrayList<>();
    cmd.add("-gencert");
    cmd.add("-alias");
    cmd.add(alias);
    cmd.add("-storepass");
    cmd.add(getKeyStorePass());
    cmd.add("-keypass");
    cmd.add(keypass);
    cmd.add("-keystore");
    cmd.add(keyFile);
    cmd.add("-keyalg");
    cmd.add("RSA");
    cmd.add("-keysize");
    cmd.add("2048");

    cmd.add("-sigalg");
    cmd.add("RSA");
    cmd.add("-infile");
    cmd.add(in_file_csr);
    cmd.add("-outfile");
    cmd.add(out_file_crt);

    keytool(cmd.toArray(JF.StringArrayType));

    return true;
  }

  public boolean loadKEYandCRT(String alias, InputStream keyStream, InputStream certStream, String keypass) {
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
      keyStore.setKeyEntry(alias, ff, keypass.toCharArray(), certs);
      return true;
    } catch (Exception e) {
      keyStore = null;
      storePass = null;
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

  public KeyStore.Entry getEntry(String alias) {
    try {
      return keyStore.getEntry(alias, new KeyStore.PasswordProtection(storePass));
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public KeyPair getKeyPair(String alias, String keypass) {
    try {
      Key key = (PrivateKey) keyStore.getKey(alias, keypass.toCharArray());
      Certificate cert = keyStore.getCertificate(alias);
      PublicKey publicKey = cert.getPublicKey();
      return new KeyPair(publicKey, (PrivateKey) key);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public boolean setEntry(String alias, KeyStore.Entry entry) {
    try {
      keyStore.setEntry(alias, entry, new KeyStore.PasswordProtection(storePass));
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean deleteEntry(String alias) {
    try {
      keyStore.deleteEntry(alias);
      return true;
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

  public boolean exportCRT(String alias, String fileout) {
    try {
      Certificate crt = getCRT(alias);
      byte[] data = crt.getEncoded();
      FileOutputStream fos = new FileOutputStream(fileout);
      fos.write(data);
      fos.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  /** Get private key w/ key password. */
  public Key getKEY(String alias, String keypass) {
    try {
      return keyStore.getKey(alias, keypass.toCharArray());
    } catch (Exception e) {
      return null;
    }
  }

  public boolean exportKEY(String alias, String keypass, String fileout) {
    try {
      Key key = getKEY(alias, keypass);
      byte[] data = key.getEncoded();
      FileOutputStream fos = new FileOutputStream(fileout);
      fos.write(data);
      fos.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
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
    return keyStore != null && storePass != null;
  }

  public KeyStore getKeyStore() {
    return keyStore;
  }

  public String getKeyStorePass() {
    return new String(storePass);
  }

  public int getCount() {
    try {
      return keyStore.size();
    } catch (Exception e) {
      JFLog.log(e);
      return 0;
    }
  }

  public boolean verify(String alias) {
    Certificate rootcert = getCRT(root);
    if (rootcert == null) return false;
    PublicKey key = rootcert.getPublicKey();
    Certificate cert = getCRT(alias);
    try {
      cert.verify(key);
      return true;
    } catch (Exception e) {
      //JFLog.log(e);
      return false;
    }
  }

  public boolean contains(String alias) {
    try {
      return keyStore.containsAlias(alias);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
}
