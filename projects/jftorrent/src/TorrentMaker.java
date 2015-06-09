/**
 * Created : May 27, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.security.*;
import java.util.*;

import javaforce.*;

public class TorrentMaker {
  public static void usage() {
    System.out.println("Usage : outfile.torrent <infile | infolder> \"url\" [--name=NAME] [-comment=COMMENT]");
    System.out.println("Example : jftorrentMaker test.torrent test.zip \"http://192.168.1.2:6969/\"");
    System.exit(1);
  }
  private static long totalLength = 0;
  private static final int piece_length = 512 * 1024;
  private static ArrayList<String> files = new ArrayList<String>();
  private static ArrayList<Long> flength = new ArrayList<Long>();
  private static ArrayList<byte[]> pieces = new ArrayList<byte[]>();
  private static byte[] buf = new byte[piece_length];
  private static int bufpos = 0;
  private static String fsPath, torrentPath;
  public static void main(String args[]) {
    if (args.length == 0) {
      usage();
    }
    String outFile = null;
    String inFile = null;
    String name = null;
    String url = null;
    String comment = null;
    for(int a=0;a<args.length;a++) {
      if (args[a].startsWith("--name=")) {
        name = args[a].substring(7);
        continue;
      }
      if (args[a].startsWith("--comment=")) {
        comment = args[a].substring(10);
        continue;
      }
      if (outFile == null) {
        outFile = args[a];
        continue;
      }
      if (inFile == null) {
        inFile = args[a];
        continue;
      }
      if (url == null) {
        url = args[a];
        continue;
      }
      usage();
    }
    if ((outFile == null) || (inFile == null) || (url == null)) {
      usage();
    }
    if (JF.isWindows()) {
      outFile = outFile.replaceAll("\\\\", "/");
      inFile = inFile.replaceAll("\\\\", "/");
    }
    try {
      File file = new File(inFile);
      if (!file.exists()) throw new Exception("infile doesn't exist");
      if (name == null) {
        int idx = inFile.lastIndexOf("/");
        if (idx == -1)
          name = inFile;
        else
          name = inFile.substring(idx+1);
      }
      if (file.isDirectory()) {
        String fullPath = file.getAbsolutePath();
        int idx = fullPath.lastIndexOf("/");
        if (idx == -1) {
          fsPath = ".";
        } else {
          fsPath = fullPath.substring(0, idx);
        }
        torrentPath = "";
        if (inFile.endsWith("/")) inFile = inFile.substring(0, inFile.length()-1);
        addFolder(inFile);
      } else {
        fsPath = ".";
        torrentPath = "";
        addFile(inFile);
      }
      lastPiece();
      ByteArrayOutputStream bb = new ByteArrayOutputStream();
      bb.write("d".getBytes());
      bb.write("8:announce".getBytes());
      bb.write(("" + url.length() + ":" + url).getBytes());
      if (comment != null) {
        bb.write("7:comment".getBytes());
        bb.write((comment.length() + ":" + comment).getBytes());
      }
      bb.write("4:info".getBytes());
      bb.write("d".getBytes());
      bb.write("6:length".getBytes());
      bb.write(("i" + totalLength + "e").getBytes());
      bb.write(("4:name").getBytes());
      bb.write((name.length() + ":" + name).getBytes());
      bb.write("12:piece length".getBytes());
      bb.write(("i" + piece_length + "e").getBytes());
      if (files.size() > 1) {
        bb.write("5:files".getBytes());
        bb.write("l".getBytes());
        int noFiles = files.size();
        for(int a=0;a<noFiles;a++) {
          bb.write("d".getBytes());
          bb.write("6:length".getBytes());
          bb.write(("i" + flength.get(a).longValue() + "e").getBytes());
          bb.write("4:path".getBytes());
          bb.write("l".getBytes());
          String path[] = files.get(a).split("/");
          for(int b=0;b<path.length;b++) {
            bb.write((path[b].length() + ":" + path[b]).getBytes());
          }
          bb.write("e".getBytes());  //path
          bb.write("e".getBytes());  //file dict
        }
        bb.write("e".getBytes());  //files list
      }
      int noPieces = pieces.size();
      bb.write("6:pieces".getBytes());
      bb.write(((noPieces * 20) + ":").getBytes());
      for(int a=0;a<noPieces;a++) {
        bb.write(pieces.get(a));
      }
      bb.write("e".getBytes());  //info dict
      bb.write("e".getBytes());  //torrent dict
      FileOutputStream fos = new FileOutputStream(outFile);
      fos.write(bb.toByteArray());
      fos.close();
      System.out.println("Done!");
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  private static final int tmplength = 1024 * 64;
  private static byte tmpbuf[] = new byte[tmplength];
  private static void addFile(String filename) {
    try {
      File file = new File(fsPath + "/" + torrentPath + "/" + filename);
      files.add(torrentPath + "/" + filename);
      long length = file.length();
      totalLength += length;
      flength.add(length);
      FileInputStream fis = new FileInputStream(file);
      long toRead = length;
      int read;
      while (toRead > 0) {
        read = piece_length - bufpos;
        if (read > tmplength) read = tmplength;
        if (read > toRead) read = (int)toRead;
        read = fis.read(tmpbuf, 0, read);
        if (read <= 0) throw new Exception("file read failed");
        System.arraycopy(tmpbuf, 0, buf, bufpos, read);
        toRead -= read;
        bufpos += read;
        if (bufpos == piece_length) {
          addPiece();
        }
      }
      fis.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  private static void addFolder(String folder) {
    String orgPath = torrentPath;
    if (torrentPath.length() > 0) torrentPath += "/";
    torrentPath += folder;
    File file = new File(fsPath + "/" + torrentPath);
    File files[] = file.listFiles();
    if (files != null) {
      for(int a=0;a<files.length;a++) {
        if (files[a].isDirectory()) {
          addFolder(files[a].getName());
        } else {
          addFile(files[a].getName());
        }
      }
    }
    torrentPath = orgPath;
  }
  private static void lastPiece() {
    if (bufpos == 0) return;  //no partial piece
    Arrays.fill(buf, bufpos, piece_length, (byte)0);
    addPiece();
  }
  private static void addPiece() {
    if (bufpos == piece_length) {
      pieces.add(SHA1sum(buf));
    } else {
      pieces.add(SHA1sum(Arrays.copyOfRange(buf, 0, bufpos)));
    }
    bufpos = 0;
  }
  public static byte[] SHA1sum(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      return md.digest(data);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
      return null;
    }
  }
}
