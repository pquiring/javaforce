package javaforce.utils;

/** Utility to work with xar files.
 *
 * INCOMPLETE!!!
 *
 * Used primarily for Mac OS X installation packages (.pkg).
 *
 * @author pquiring
 *
 * Created : May 31, 2014
 */

import java.io.*;
import java.util.zip.*;

import javaforce.*;

public class xar {
  private RandomAccessFile raf;

  public static void main(String[] args) {
    xar i = new xar();
    i.open(args[0]);
  }

  public boolean open(String file) {
    try {
      raf = new RandomAccessFile(file, "r");

      //read header
      byte[] header = new byte[28];
      if (raf.read(header) != header.length) throw new Exception("bad header:read failed");
      if (BE.getuint32(header, 0) != 0x78617221) throw new Exception("bad header:magic");
      int header_size = BE.getuint16(header, 4);
      if (header_size < header.length) throw new Exception("bad header:too small");
      int version = BE.getuint16(header, 6);
      long toc_size_compressed = BE.getuint64(header, 8);
      long toc_size_uncompressed = BE.getuint64(header, 16);
      int cksum_algo = BE.getuint32(header, 24);
      if (header_size > header.length) {
        raf.seek(header_size);
      }
      //now read compressed toc
      byte[] toc_compressed = new byte[(int)toc_size_compressed];
      //TODO : support reading fragments
      if (raf.read(toc_compressed) != toc_compressed.length) throw new Exception("bad toc:read failed");
      //now decompress toc
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      InflaterOutputStream dos = new InflaterOutputStream(baos);
      dos.write(toc_compressed);
      dos.finish();
      byte[] toc_uncompressed = baos.toByteArray();
      if (toc_uncompressed.length != toc_size_uncompressed) throw new Exception("bad toc:deflat failed");
      System.out.println("toc=" + new String(toc_uncompressed));
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public void close() {
    if (raf == null) return;
    try {
      raf.close();
      raf = null;
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
