/** Compression
 *
 * @author pquiring
 */
package javaforce;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Compression {
  public static void main(String[] args) {
    //test compress/decompress
    try {
      File input = new File("tapetool.exe");
      File output = new File("compress.dat");
      FileInputStream fis = new FileInputStream(input);
      FileOutputStream fos = new FileOutputStream(output);
      long uncompressed = input.length();
      System.out.println("  org size=" + uncompressed);
      long compressed = compress(fis, fos, uncompressed);
      System.out.println("  compressed=" + compressed);
      fis.close();
      fos.close();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    try {
      File input = new File("compress.dat");
      File output = new File("uncompress.dat");
      long compressed = input.length();
      FileInputStream fis = new FileInputStream(input);
      FileOutputStream fos = new FileOutputStream(output);
      long uncompressed = decompress(fis, fos, compressed);
      System.out.println("uncompressed=" + uncompressed);
      fis.close();
      fos.close();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    System.out.println("done");
  }
  private final static int blocksize = (64 * 1024);
  public static long compress(InputStream is, OutputStream os, long uncompressed) throws Exception {
    long left = uncompressed;
    long wrote = 0;
    byte[] in = new byte[blocksize];
    byte[] out = new byte[blocksize];
    Deflater compress = new Deflater();
    while (left > 0) {
      if (compress.needsInput()) {
        int toread = left > blocksize ? blocksize : (int)left;
        int read = is.read(in, 0, toread);
        if (read > 0) {
          compress.setInput(in, 0, read);
          left -= read;
        }
      }
      int compressed = compress.deflate(out);
      if (compressed > 0) {
        os.write(out, 0, compressed);
        wrote += compressed;
      }
    }
    compress.finish();
    while (!compress.finished()) {
      int compressed = compress.deflate(out);
      if (compressed > 0) {
        os.write(out, 0, compressed);
        wrote += compressed;
      }
    }
    compress.end();
    return wrote;
  }
  public static long decompress(InputStream is, OutputStream os, long compressed) throws Exception {
    long left = compressed;
    long wrote = 0;
    byte[] in = new byte[blocksize];
    byte[] out = new byte[blocksize];
    Inflater decompress = new Inflater();
    while (left > 0) {
      if (decompress.needsInput()) {
        int toread = left > blocksize ? blocksize : (int)left;
        int read = is.read(in, 0, toread);
        if (read > 0) {
          decompress.setInput(in, 0, read);
          left -= read;
        }
      }
      int uncompressed = decompress.inflate(out);
      if (uncompressed > 0) {
        os.write(out, 0, uncompressed);
        wrote += uncompressed;
      }
    }
    while (!decompress.finished()) {
      int uncompressed = decompress.inflate(out);
      if (uncompressed > 0) {
        os.write(out, 0, uncompressed);
        wrote += uncompressed;
      }
    }
    decompress.end();
    return wrote;
  }
}
