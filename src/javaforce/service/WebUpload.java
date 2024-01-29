package javaforce.service;

/**
 *
 * @author pquiring
 *
 * Created : Sept 16, 2013
 */

import java.io.*;
import java.util.*;
import javaforce.JF;

/** A class to handle file uploads (default max 64MBs) */
public class WebUpload {
  public static boolean isMultipartContent(WebRequest req) {
    //Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...\r\n
    String contentType = req.getHeader("Content-Type");
    if (contentType == null) return false;
    return (contentType.trim().startsWith("multipart/form-data;"));
  }
  public static class WebFile {
    /** File data */
    public byte[] data;
    /** Name of file (excluding any path info) */
    public String name;
    /** Copies temp uploaded file to dest */
    public void write(File dest) throws Exception {
      FileOutputStream fos = new FileOutputStream(dest);
      fos.write(data);
      fos.close();
    }
    public String getName() {return name;}
  }
  private static int maxlength = 64 * 1024 * 1024;  //64MBs
  public static void setMaxLength(int maxlength) {
    WebUpload.maxlength = maxlength;
  }
  private boolean cmp(byte[] s1, int p1, byte[] s2, int p2, int len) {
    while (len > 0) {
      if (s1[p1++] != s2[p2++]) return false;
      len--;
    }
    return true;
  }
  public WebFile[] parseRequest(WebRequest req) throws Exception {
    //TODO : read post data and save to files in temp folder
    //Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...
    String contentType = req.getHeader("Content-Type");
    if (contentType == null) throw new Exception("WebUpload:No Content-Type");
    int idx = contentType.indexOf("boundary=");
    if (idx == -1) throw new Exception("WebUpload:No boundary");
    byte[] boundary = ("--" + contentType.substring(idx+9)).getBytes();
    int boundaryLength = boundary.length;
    String contentLength = req.getHeader("Content-Length");
    if (contentLength == null) throw new Exception("WebUpload:No Content-Length");
    int postlength = Integer.valueOf(contentLength);
    if (postlength > maxlength) throw new Exception("WebUpload:Upload > Max allowed");
    byte[] data = JF.readAll(req.is, postlength);
    //now process files in post data
    //[\r\n]boundary\r\nContent-Disposition: form-data; name="???"; filename="???"\r\nContent-Type: mime/type\r\n\r\n
    int pos = 0;
    ArrayList<WebFile> files = new ArrayList<WebFile>();
    while (pos < postlength) {
      if (data[pos] == '\r' && data[pos+1] == '\n') {
        pos += 2;
        continue;
      }
      if (!cmp(data, pos, boundary, 0, boundaryLength)) throw new Exception("WebUpload:Bad boundary @ " + pos);
      pos += boundaryLength;
      //read header
      StringBuilder headers = new StringBuilder();
      while (!headers.toString().endsWith("\r\n\r\n")) {
        headers.append((char)data[pos++]);
      }
      String[] lns = headers.toString().split("\r\n");
      //extract Content-Disposition "filename"
      String filename = null;
      for(int a=0;a<lns.length;a++) {
        if (lns[a].startsWith("Content-Disposition:")) {
          String[] fields = lns[a].split(";");
          for(int b=0;b<fields.length;b++) {
            if (fields[b].trim().startsWith("filename=")) {
              filename = fields[b].trim().substring(9);
              break;
            }
          }
          break;
        }
      }
      if (filename == null) throw new Exception("WebUpload:Upload has no filename");
      if (filename.startsWith("\"") && filename.endsWith("\"")) {
        filename = filename.substring(1, filename.length() - 1);  //remove quotes
      }
      WebFile file = new WebFile();
      file.name = filename;
      //find next boundary
      int fileStart = pos;
      int fileEnd = -1;
      while (pos < postlength) {
        if (data[pos] == '\r' && data[pos+1] == '\n' && cmp(data, pos+2, boundary, 0, boundaryLength)) {
          fileEnd = pos;
          break;
        }
        pos++;
      }
      if (fileEnd == -1) throw new Exception("WebUpload:No boundary found after file data");
      int fileLength = fileEnd - fileStart;
      file.data = new byte[fileLength];
      System.arraycopy(data, fileStart, file.data, 0, fileLength);
      files.add(file);
      pos += fileLength;
    }
    return files.toArray(new WebFile[0]);
  }
}
