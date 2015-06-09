package jfconfig;

import java.security.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.security.spec.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Iterator;

/**
 * ImportKey.java
 *
 * <p>This class imports a key and a certificate into a keystore.
 * If the keystore is
 * already present, it is simply deleted. Both the key and the
 * certificate file must be in <code>DER</code>-format. The key must be
 * encoded with <code>PKCS#8</code>-format. The certificate must be
 * encoded in <code>X.509</code>-format.</p>
 *
 * <p>Key format:</p>
 * <p><code>openssl pkcs8 -topk8 -nocrypt -in YOUR.KEY -out YOUR.KEY.der -outform der</code></p>
 * <p>Format of the certificate:</p>
 * <p><code>openssl x509 -in YOUR.CERT -out YOUR.CERT.der -outform der</code></p>
 * <p>Import key and certificate:</p>
 * <p><code>java javaforce.ImportKey YOUR.KEY.der YOUR.CERT.der</code></p><br />
 *
 * <p><em>Caution:</em> the old <code>keystore</code>-file is
 * deleted and replaced with a keystore only containing <code>YOUR.KEY</code>
 * and <code>YOUR.CERT</code>. The keystore and the key has no password;
 * they can be set by the <code>keytool -keypasswd</code>-command for setting
 * the key password, and the <code>keytool -storepasswd</code>-command to set
 * the keystore password.
 * <p>The key and the certificate is stored under the alias
 * <code>tomcat</code>; to change this, use <code>keytool -keyclone</code>.
 *
 * Created: Fri Apr 13 18:15:07 2001
 * Updated: Fri Apr 19 11:03:00 2002
 *
 * @author Joachim Karrer, Jens Carlberg
 *
 * Modified for jfLinux by Peter Quiring
 *
 * @version 1.1
 **/

public class ImportKey {

  /**
   * <p>Creates an InputStream from a file, and fills it with the complete
   * file. Thus, available() on the returned InputStream will return the
   * full number of bytes the file contains</p>
   * @param fname The filename
   * @return The filled InputStream
   * @exception IOException, if the Streams couldn't be created.
   **/
  private static byte[] getBytes(InputStream fis) throws IOException {
    DataInputStream dis = new DataInputStream(fis);
    byte[] bytes = new byte[dis.available()];
    dis.readFully(bytes);
    return bytes;
  }

  private static InputStream createInputStream(byte bytes[]) {
    return new ByteArrayInputStream(bytes);
  }

  /**
   * <p>Takes two file names for a key and the certificate for the key,
   * and imports those into a keystore. Optionally it takes an alias
   * for the key.
   * <p>The first argument is the filename for the key. The key should be
   * in PKCS8-format.
   * <p>The second argument is the filename for the certificate for the key.
   * <p>If a third argument is given it is used as the alias. If missing,
   * the key is imported with the alias importkey
   * <p>The name of the keystore file can be controlled by setting
   * the keystore property(java -Dkeystore=mykeystore). If no name
   * is given, the file is named <code>keystore.ImportKey</code>
   * and placed in your home directory.
   * @param args [0] Name of the key file, [1] Name of the certificate file
   * [2] Alias for the key.
   **/
  public static String importKeys(InputStream certFile, InputStream keyFile, String path) {
    StringBuffer errmsg = new StringBuffer();

    // change this if you want another password by default
    String keypass = "changeit";

    // change this if you want another alias by default
    String defaultalias = "tomcat";

    // change this if you want another keystorefile by default
    String keystorename = path;

    try {
      // initializing and clearing keystore
      KeyStore ks = KeyStore.getInstance("JKS", "SUN");
      ks.load( null , keypass.toCharArray());
//      System.out.println("Using keystore-file : " + keystorename);
      ks.store(new FileOutputStream(keystorename), keypass.toCharArray());
      ks.load(new FileInputStream(keystorename), keypass.toCharArray());

      // loading Key
      byte[] key = new byte[keyFile.available()];
      keyFile.read(key, 0, keyFile.available());
      keyFile.close();

      KeyFactory kf = KeyFactory.getInstance("RSA");
      PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(key);
      PrivateKey ff = kf.generatePrivate(keysp);

      // loading CertificateChain
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      byte[] certBytes = getBytes(certFile);
      InputStream certStream = createInputStream(certBytes);

      Collection c = cf.generateCertificates(certStream);
      Certificate[] certs = new Certificate[c.toArray().length];

      if (c.size() == 1) {
        certStream = createInputStream(certBytes);  //reset stream
        errmsg.append("Notice:One certificate, no chain.<br>");
        Certificate cert = cf.generateCertificate(certStream);
        certs[0] = cert;
      } else {
        errmsg.append("Notice:Certificate chain length: " + c.size() + "<br>");
        certs = (Certificate[])c.toArray();
      }

      // storing keystore
      ks.setKeyEntry(defaultalias, ff, keypass.toCharArray(), certs);
      errmsg.append("Key and certificate stored.<br>");
//      errmsg.append("Alias:"+defaultalias+" Password:"+keypass);
      ks.store(new FileOutputStream(keystorename), keypass.toCharArray());
    } catch (Exception e) {
      errmsg.append(e.toString());
    }
    return errmsg.toString();
  }

  public static String status(String path) {
    StringBuffer errmsg = new StringBuffer();

    errmsg.append("KeyStore Location : " + path + "/.keystore<br>");

    errmsg.append("SSL Key Status : ");

    // change this if you want another password by default
    String keypass = "changeit";

    // change this if you want another keystorefile by default
    String keystorename = path + "/.keystore";

    try {
      // initializing and clearing keystore
      KeyStore ks = KeyStore.getInstance("JKS", "SUN");
      ks.load(new FileInputStream(keystorename), keypass.toCharArray());

      if (ks.containsAlias("tomcat")) {
        if (ks.size() < 2) {
          errmsg.append("Your keystore looks okay.");
        } else {
          errmsg.append("You are missing some keys.");
        }
      } else {
        errmsg.append("Your keystore does NOT contain the proper alias.");
      }
    } catch (FileNotFoundException fnfe) {
      errmsg.append("Your keystore does NOT exist.");
    } catch (Exception e) {
      errmsg.append(e.toString());
    }
    errmsg.append("<br>");
    return errmsg.toString();
  }

  public static String importKeyStore(InputStream keyStore, String path) {
    //just copy over keystore
    StringBuffer errmsg = new StringBuffer();

    // change this if you want another keystorefile by default
    String keystorename = path + "../../.keystore";

    try {
      int size = keyStore.available();
      byte buf[] = new byte[size];
      keyStore.read(buf, 0, size);
      FileOutputStream fos = new FileOutputStream(keystorename);
      fos.write(buf, 0, size);
      fos.close();
      errmsg.append("KeyStore upload successful");
    } catch (Exception e) {
      errmsg.append(e.toString());
    }
    return errmsg.toString();
  }

}// KeyStore
