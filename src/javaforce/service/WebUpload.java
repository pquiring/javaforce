package javaforce.service;

/** WebUpload.
 *
 * Processes HTTP upload requests.
 *
 * @author pquiring
 *
 * Created : Sept 16, 2013
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.webui.*;
import javaforce.webui.tasks.*;

/** A class to handle file uploads (default max 64MBs) */
public class WebUpload {
  public static boolean debug = false;
  /** Checks if web request includes multipart form data normally used with file uploads. */
  public static boolean isMultipartContent(WebRequest req) {
    //Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...\r\n
    String contentType = req.getHeader("Content-Type");
    if (contentType == null) return false;
    return (contentType.trim().startsWith("multipart/form-data;"));
  }

  /** Uploaded file. */
  public static class WebFile {
    /** Uploaded file. */
    public File file;
    /** Name of file (excluding any path info) */
    public String name;
    /** Moves uploaded file to dest file. (optional) */
    public boolean move(File dest) throws Exception {
      if (dest.exists()) return false;
      return JF.moveFile(file, dest);
    }
    public String getName() {return name;}
  }

  private static long maxlength = 64 * JF.MB;  //64MBs
  /** Sets max file upload (-1 = unlimited) (default = 64MBs) */
  public static void setMaxLength(long maxlength) {
    WebUpload.maxlength = maxlength;
  }

  private static final int max_buffer_size = (8 * 1024);
  private static byte[] end_of_line = "\r\n".getBytes();
  private static byte[] end_of_headers = "\r\n\r\n".getBytes();
  private static byte[] end_of_files = "--\r\n".getBytes();

  private static final int SIZE = 1;
  private static final int CLIENT = 2;
  private static final int FILE = 3;

  private JFByteBuffer buffer = new JFByteBuffer(max_buffer_size);
  public WebFile[] processRequest(WebRequest req, String out_folder) throws Exception {
    String file_size = null;
    Status status = null;
    //Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...
    String contentType = req.getHeader("Content-Type");
    if (contentType == null) throw new Exception("WebUpload:No Content-Type");
    if (debug) {
      for(String field : req.fields) {
        JFLog.log(field);
      }
    }
    int idx = contentType.indexOf("boundary=");
    if (idx == -1) throw new Exception("WebUpload:No boundary");
    byte[] boundary = ("--" + contentType.substring(idx+9)).getBytes();
    int boundaryLength = boundary.length;
    if (debug) JFLog.log("WebUpload:boundary=" + new String(boundary) + ":length=" + boundaryLength);
    String contentLength = req.getHeader("Content-Length");
    if (contentLength == null) throw new Exception("WebUpload:No Content-Length");
    long postLength = Long.valueOf(contentLength);
    if (maxlength > 0 && postLength > maxlength) {
      throw new Exception("WebUpload:Upload > Max allowed");
    }
    long postCopied = 0;
    long postLeft = postLength;
    long pos = 0;
    boolean done = false;
    if (debug) JFLog.log("postLength=" + postLength);
    ArrayList<WebFile> files = new ArrayList<WebFile>();
    while (postCopied < postLength) {
      //read boundary
      while (buffer.getLength() < boundaryLength + 2) {
        buffer.compact();
        int toRead = (boundaryLength + 2) - buffer.getLength();
        if (toRead > postLeft) {
          toRead = (int)postLeft;
        }
        if (toRead > buffer.available()) {
          toRead = (int)buffer.available();
        }
        if (toRead == 0) throw new Exception("WebUpload:out of data");
        int bytes = buffer.write(req.is, toRead);
        if (bytes > 0) {
          postCopied += bytes;
          postLeft -= bytes;
        }
      }
      if (buffer.startsWith(end_of_line)) {
        if (debug) JFLog.log("skip end_of_line");
        buffer.skip(2);
        pos += 2;
      }
      int offset = buffer.getOffset();
      if (Arrays.compare(buffer.getBuffer(), offset, offset + boundaryLength, boundary, 0, boundaryLength) != 0) {
        throw new Exception("WebUpload:Bad boundary @ " + postCopied);
      }
      if (debug) {
        JFLog.log("Boundary@" + pos);
      }
      buffer.skip(boundaryLength);
      pos += boundaryLength;
      buffer.compact();
      //read headers
      int headers_end = 0;
      do {
        if (buffer.indexOf(end_of_files) == 0) {
          done = true;
          if (debug) JFLog.log("end_of_files");
          break;
        }
        int toRead = 1024;
        if (debug) {
          JFLog.log("toRead=" + toRead + "," + postLeft + "," + buffer.available());
        }
        if (toRead > postLeft) {
          toRead = (int)postLeft;
        }
        if (toRead > buffer.available()) {
          toRead = (int)buffer.available();
        }
        if (toRead == 0) throw new Exception("WebUpload:out of data");
        int bytes = buffer.write(req.is, toRead);
        if (bytes > 0) {
          postCopied += bytes;
          postLeft -= bytes;
          headers_end = buffer.indexOf(end_of_headers);
        }
      } while (headers_end == -1);
      if (done) break;
      headers_end += 4;
      //Content-Disposition fields
      String cd_filename = null;
      String cd_name = null;
      if (debug) {
        JFLog.log("Headers@" + pos + ":length=" + headers_end);
      }
      String headers = buffer.readString(headers_end);
      pos += headers_end;
      if (debug) {
        JFLog.log("Headers=" + headers);
      }
      HTTP.Parameters params = HTTP.Parameters.decode(headers.split("\r\n"));
      for(String p_key : params.keys()) {
        String p_value = params.get(p_key);
        switch (p_key) {
          case "Content-Disposition": {
            String[] fields = p_value.split(";");
            for(String field : fields) {
              field = field.trim();
              int eq = field.indexOf('=');
              if (eq == -1) continue;
              String f_key = field.substring(0, eq);
              String f_value = field.substring(eq + 1);
              switch (f_key) {
                case "filename":
                  //form-data; name="file"; filename="..."
                  cd_filename = f_value;
                  if (cd_filename.startsWith("\"") && cd_filename.endsWith("\"")) {
                    cd_filename = cd_filename.substring(1, cd_filename.length() - 1);  //remove quotes
                  }
                  break;
                case "name":
                  //form-data; name="size"
                  cd_name = f_value;
                  if (cd_name.startsWith("\"") && cd_name.endsWith("\"")) {
                    cd_name = cd_name.substring(1, cd_name.length() - 1);  //remove quotes
                  }
                break;
              }
            }
            break;
          }
          case "Content-Length": {
            //most browsers to not include Content-Length ???
            file_size = p_value;
            break;
          }
        }
      }
      int type = 0;
      switch (cd_name) {
        case "size": {
          file_size = null;
          type = SIZE;
          break;
        }
        case "client": {
          type = CLIENT;
          break;
        }
        case "file": {
          type = FILE;
          break;
        }
        default: {
          throw new Exception("WebUpload:Unknown field:" + cd_name);
        }
      }
      if (type == FILE) {
        if (cd_filename == null) throw new Exception("WebUpload:Upload has no filename");
      }
      OutputStream fos = null;
      if (type == FILE) {
        WebFile uploadFile = new WebFile();
        uploadFile.name = cd_filename;
        uploadFile.file = new File(out_folder + "/" + cd_filename);
        files.add(uploadFile);
        fos = new FileOutputStream(uploadFile.file);
      } else {
        fos = new ByteArrayOutputStream();
      }
      long fileLength = -1;
      if (type == FILE) {
        if (file_size != null) {
          fileLength = Long.valueOf(file_size);
          file_size = null;
        }
      }
      long fileCopied = 0;
      long fileLeft = fileLength;
      long fileCopiedMB = 0;
      long fileLengthMB = fileLength / JF.MB;
      int length = buffer.getLength();
      if (fileLength > (length + postLeft)) {
        throw new Exception("WebUpload:file exceeds post data size:" + fileLength + "," + (length + postLeft));
      }
      if (debug) {
        JFLog.log("file@" + pos + ":length=" + fileLength);
      }
      //receive form field
      while (fileLength == -1 || fileCopied < fileLength) {
        int buflen = buffer.getLength();
        if (fileLength == -1) {
          idx = buffer.indexOf(boundary);
          if (idx != -1) {
            //end of content found
            idx -= 2;  //\r\n
            if (debug) JFLog.log("end of content@" + idx);
            fileLength = fileCopied + idx;
            fileLeft = fileLength - fileCopied;
            fileLengthMB = fileLength / JF.MB;
            buflen = idx;
          } else {
            buflen -= boundaryLength;
          }
        } else {
          if (buflen > fileLeft) {
            buflen = (int)fileLeft;
          }
        }
        if (buflen > 0 ) {
          int writen = buffer.readBytes(fos, buflen);
          if (writen > 0) {
            fileCopied += writen;
            fileLeft -= writen;
            if (status != null && fileLength != -1) {
              long copiedMB = fileCopied / JF.MB;
              if (copiedMB != fileCopiedMB) {
                fileCopiedMB = copiedMB;
                status.setPercent((int)((fileCopiedMB * 100) / fileLengthMB));
              }
            }
          }
        }
        buffer.compact();
        if (fileLength != -1 && fileLeft == 0) break;
        int available = buffer.available();
        int toRead = fileLength == -1 ? available : (int)fileLeft;
        if (debug) JFLog.log("toRead=" + toRead + "," + available + "," + fileLength + "," + fileLeft);
        if (toRead > available) {
          toRead = available;
        }
        if (toRead == 0) {
          fos.close();
          throw new Exception("WebUpload:out of data");
        }
        int bytes = buffer.write(req.is, toRead);
        if (bytes > 0) {
          postCopied += bytes;
          postLeft -= bytes;
        }
      }
      if (type == SIZE) {
        ByteArrayOutputStream baos = (ByteArrayOutputStream)fos;
        file_size = new String(baos.toByteArray());
        if (debug) JFLog.log("size=" + file_size);
      }
      if (type == CLIENT) {
        ByteArrayOutputStream baos = (ByteArrayOutputStream)fos;
        String hash = new String(baos.toByteArray());
        WebUIClient client = WebUIServer.getClient(hash);
        out_folder = client.getUploadFolder();
        status = client.getUploadStatus();
      }
      if (fos != null) {
        fos.close();
      }
      pos += fileLength;
      if (status != null) {
        status.setPercent(100);
        status.setResult(true);
      }
    }
    return files.toArray(new WebFile[0]);
  }
}
