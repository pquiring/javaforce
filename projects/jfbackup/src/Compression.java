/** Test
 *
 * - test compress/decompress
 *
 * @author pquiring
 */

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Compression {
  public static void main(String args[]) {
    //test compress/decompress
    try {
      File input = new File("warning.gif");
      File output = new File("compress.dat");
      FileInputStream fis = new FileInputStream(input);
      FileOutputStream fos = new FileOutputStream(output);
      long uncompressed = input.length();
      long compressed = compress(fis, fos, uncompressed);
      System.out.println("compressed=" + compressed);
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
  public static long compress(FileInputStream fis, FileOutputStream fos, long uncompressed) throws Exception {
    long left = uncompressed;
    long wrote = 0;
    byte[] in = new byte[blocksize];
    byte[] out = new byte[blocksize];
    Deflater compress = new Deflater();
    while (left > 0) {
      int toread = left > blocksize ? blocksize : (int)left;
      int read = fis.read(in, 0, toread);
      if (read > 0) {
        while (compress.needsInput() == false) {
          int compressed = compress.deflate(out);
          if (compressed > 0) {
            fos.write(out, 0, compressed);
            wrote += compressed;
          }
        }
        compress.setInput(in, 0, read);
        left -= read;
      }
    }
    compress.finish();
    while (!compress.finished()) {
      int compressed = compress.deflate(out);
      if (compressed > 0) {
        fos.write(out, 0, compressed);
        wrote += compressed;
      }
    }
    compress.end();
    return wrote;
  }
  public static long decompress(FileInputStream fis, FileOutputStream fos, long compressed) throws Exception {
    long left = compressed;
    long wrote = 0;
    byte[] in = new byte[blocksize];
    byte[] out = new byte[blocksize];
    Inflater decompress = new Inflater();
    while (left > 0) {
      int toread = left > blocksize ? blocksize : (int)left;
      int read = fis.read(in, 0, toread);
      if (read > 0) {
        while (decompress.needsInput() == false) {
          int uncompressed = decompress.inflate(out);
          if (uncompressed > 0) {
            fos.write(out, 0, uncompressed);
            wrote += uncompressed;
          }
        }
        decompress.setInput(in, 0, read);
        left -= read;
      }
    }
    while (!decompress.finished()) {
      int uncompressed = decompress.inflate(out);
      if (uncompressed > 0) {
        fos.write(out, 0, uncompressed);
        wrote += uncompressed;
      }
    }
    decompress.end();
    return wrote;
  }
}
