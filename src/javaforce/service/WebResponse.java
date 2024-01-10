package javaforce.service;

/**
 * Created : Aug 23, 2013
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class WebResponse extends OutputStream {
  OutputStream os;
  boolean liveStream;
  private ByteArrayOutputStream buf = new ByteArrayOutputStream();
  private int statusCode = 200;
  private String statusString = "OK";
  private String contentType = "text/html; charset=UTF-8";
  private ArrayList<String> cookies = new ArrayList<String>();
  private ArrayList<String> headers = new ArrayList<String>();

  public void write(int b) throws IOException {
    if (liveStream) throw new IOException("");
    buf.write(b);
  }

  public void write(byte b[], int off, int len) throws IOException {
    if (liveStream) throw new IOException("");
    buf.write(b, off, len);
  }

  void writeAll(WebRequest req) throws Exception {
    if (liveStream) throw new IOException("");
    byte data[] = buf.toByteArray();
    boolean gzip = false;
    if (data.length > 0) {
      if (WebServer.config_enable_gzip) {
        String accept = req.getHeader("Accept-Encoding");
        if (accept != null) {
          String encodings[] = accept.split(",");
          for(int a=0;a<encodings.length;a++) {
            if (encodings[a].trim().equals("gzip")) {
              gzip = true;
              break;
            }
          }
        }
      }
      if (gzip) {
        addHeader("Content-Encoding: gzip");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(baos);
        gos.write(data);
        gos.finish();
        data = baos.toByteArray();
      }
    }
    int size = data.length;
    writeHeaders(size, gzip);
    if (data.length > 0) {
      os.write(WebServer.chunkHeader(data));
      os.write(data);
      os.write("\r\n0\r\n\r\n".getBytes());
    }
  }

  void writeHeaders(int contentLength, boolean gzip) throws Exception {
    StringBuilder res = new StringBuilder();
    res.append("HTTP/1.1 " + statusCode + " " + statusString + "\r\n");
    if (contentLength != -1) {
      res.append("Content-Length: " + contentLength + "\r\n");
    }
    res.append("Content-Type: " + contentType + "\r\n");
    if (contentLength != 0) {
      res.append("Transfer-Encoding: chunked\r\n");
    }
    for(int a=0;a<cookies.size();a++) {
      res.append("Set-Cookie: ");
      res.append(cookies.get(a));
      res.append("\r\n");
    }
    for(int a=0;a<headers.size();a++) {
      res.append(headers.get(a));
      res.append("\r\n");
    }
    res.append("\r\n");  //terminate response
    os.write(res.toString().getBytes());
  }

  //public methods

  /** Returns buffered output stream.
   * When doGet() or doPut() returns the headers and content are sent to client.
   *
   * @return OutputStream
   */
  public OutputStream getOutputStream() {
    if (liveStream) return null;
    return this;
  }

  /** Returns real output stream.
   * Writes default header without Content-Length;
   * Send Data in chunked format. See Web.chunkHeader()
   *
   * @return OutputStream
   */
  public OutputStream getLiveOutputStream() {
    if (liveStream) return null;
    if (buf.size() > 0) {
      return null;
    }
    try {
      writeHeaders(-1, false);
    } catch (Exception e) {
      return null;
    }
    liveStream = true;
    return os;
  }

  public void setStatus(int sc, String msg) {
    statusCode = sc;
    statusString = msg;
  }

  public void setContentType(String type) {
    contentType = type;
  }

  public void addCookie(String name, String value) {
    cookies.add(name + "=" + value);
  }

  public void addHeader(String header) {
    headers.add(header);
  }

  public void sendRedirect(String url) {
    setStatus(301, "Moved");
    addHeader("Location: " + url);
  }
}
