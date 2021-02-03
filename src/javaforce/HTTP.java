package javaforce;

/** HTTP Client
 *
 * @author pquiring
 */

import java.net.*;
import java.io.*;
import java.util.*;

public class HTTP {
  protected Socket s;
  protected OutputStream os;
  protected InputStream is;
  protected String host;
  protected int port;
  private HashMap<String, String> request_headers = new HashMap<>();
  private HashMap<String, String> reply_headers = new HashMap<>();
  private int code = -1;
  protected static boolean debug = false;

  public static final String formType = "application/x-www-form-urlencoded";

  private final static int bufsiz = 1024;
  protected static class Buffer {
    public byte[] buf;
    public int pos;
    public int count;

    public Buffer() {
      buf = new byte[bufsiz];
      pos = 0;
      count = 0;
    }

    public void expand() {
      byte[] newbuf = new byte[buf.length << 1];
      if (count > 0) {
        System.arraycopy(buf, pos, newbuf, 0, count);
      }
      buf = newbuf;
      pos = 0;
    }

    public void condense() {
      if (pos == 0) return;
      if (count == 0) {
        pos = 0;
        return;
      }
      System.arraycopy(buf, pos, buf, 0, count);
      pos = 0;
    }

    public void consume(int length) {
      if (length > count) {
        JFLog.log("HTTP:Error:Buffer.consume(length > count)");
      }
      pos += length;
      count -= length;
      if (count == 0) {
        pos = 0;
      }
    }
    public void consumeAll() {
      pos = 0;
      count = 0;
    }

    public void setLength(int length) {
      if (buf.length == length) return;
      byte[] newbuf = new byte[length];
      if (buf.length > length) {
        //copy part
        System.arraycopy(buf, pos, newbuf, 0, length);
      } else {
        //copy all
        System.arraycopy(buf, pos, newbuf, 0, count);
      }
      buf = newbuf;
      pos = 0;
    }

    public void append(Buffer other) {
      if (other.count == 0) return;
      while (buf.length < count + other.count) {
        expand();
      }
      if (pos > 0) condense();
      System.arraycopy(other.buf, other.pos, buf, pos + count, other.count);
      count += other.count;
      other.consumeAll();
    }

    public void append(Buffer other, int maxCopy) {
      if (other.count == 0) return;
      if (maxCopy == 0) return;
      int toCopy = maxCopy;
      if (other.count < maxCopy) toCopy = other.count;
      while (buf.length < count + toCopy) {
        expand();
      }
      if (pos > 0) condense();
      System.arraycopy(other.buf, other.pos, buf, pos + count, toCopy);
      count += toCopy;
      other.consume(toCopy);
    }

    public byte[] toArray() {
      if (count != buf.length) {
        return Arrays.copyOfRange(buf, pos, pos + count);
      }
      return buf;
    }
  }

  public boolean open(String host) {
    return open(host, 80);
  }
  public boolean open(String host, int port) {
    this.host = host;
    this.port = port;
    try {
      s = new Socket(host, port);
      os = s.getOutputStream();
      is = s.getInputStream();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  /** Close connection. */
  public void close() {
    if (s != null) {
      try {
        s.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
      s = null;
    }
  }

  public boolean isConnected() {
    return s != null;
  }

  /** Set a request header. */
  public void setHeader(String key, String value) {
    request_headers.put(key, value);
  }

  /** Clears all request headers. */
  public void clearHeaders() {
    request_headers.clear();
  }

  /** Returns reply header. */
  public String getHeader(String key) {
    return reply_headers.get(key);
  }

  /** Returns reply headers. */
  public HashMap<String, String> getHeaders() {
    return reply_headers;
  }

  /** Returns last status code. */
  public int getCode() {
    return code;
  }

  private int read(Buffer buf, int length) throws Exception {
    int maxlength = buf.buf.length - buf.pos - buf.count;
    if (length > maxlength) {
      if (buf.pos > 0) {
        buf.condense();
        maxlength = buf.buf.length - buf.count;
      }
    }
    if (length > maxlength) {
      length = maxlength;
      if (length == 0) {
        JFLog.log("HTTP:Error:buffer full:" + buf.buf.length + "," + buf.pos + "," + buf.count);
        return 0;
      }
    }
    int read = is.read(buf.buf, buf.pos + buf.count, length);
    if (read > 0) {
      buf.count += read;
    }
    return read;
  }

  private void write(byte[] data) throws Exception {
    os.write(data);
  }

  private void sendRequest(String req) throws Exception {
    write(req.getBytes());
  }

  private void sendData(byte[] data) throws Exception {
    write(data);
  }

  private boolean endOfHeaders(Buffer buf) {
    if (buf.count < 4) return false;
    int length = buf.count;
    return ((buf.buf[length-4] == '\r') && (buf.buf[length-3] == '\n') && (buf.buf[length-2] == '\r') && (buf.buf[length-1] == '\n'));
  }

  private boolean haveChunkLength(Buffer buf) {
    if (buf.count < 2) return false;
    int length = buf.count;
    int start = buf.pos;
    int end = buf.pos + length - 1;
    for(int pos=start;pos<end;pos++) {
      if (buf.buf[pos] == '\r' && buf.buf[pos + 1] == '\n') return true;
    }
    return false;
  }

  private int getChunkLength(Buffer buf) {
    int value = 0;
    while (buf.buf[buf.pos] != '\r') {
      value *= 16;
      char digit = (char)buf.buf[buf.pos];
      if ((digit >= '0') && (digit <= '9')) {
        value += (digit - '0');
      } else if ((digit >= 'A' && digit <= 'F')) {
        value += (digit - 'A' + 10);
      } else if ((digit >= 'a' && digit <= 'f')) {
        value += (digit - 'a' + 10);
      } else {
        JFLog.log("HTTP:Error:Invalid chunk length");
        return -1;
      }
      buf.consume(1);
    }
    buf.consume(2);  //consume \r\n
    return value;
  }

  private boolean getReply(OutputStream os) throws Exception {
    Buffer buf = new Buffer();
    Buffer req = new Buffer();
    //read headers
    reply_headers.clear();
    do {
      int read = read(buf, bufsiz);
      buf.pos = 0;
      buf.count = read;
      while (buf.count > 0) {
        if (req.count == req.buf.length) {
          req.expand();
        }
        req.buf[req.count++] = buf.buf[buf.pos++];
        buf.count--;
        read--;
        if (endOfHeaders(req)) break;
      }
    } while (!endOfHeaders(req));
    //parse headers
    long length = -1;
    boolean chunked = false;
    String[] lns = new String(req.toArray()).split("\r\n");
    for(int i=1;i<lns.length;i++) {
      String ln = lns[i];
      int idx = ln.indexOf(':');
      if (idx == -1) continue;
      String key = ln.substring(0, idx).trim();
      String value = ln.substring(idx+1).trim();
      reply_headers.put(key, value);
      if (debug) System.out.println("Reply:" + key + "=" + value);
      switch (key.toLowerCase()) {
        case "content-length": length = Long.valueOf(value); break;
        case "transfer-encoding": chunked = value.contains("chunked"); break;
      }
    }
    //parse reply : HTTP/1.x code msg
    String reply = lns[0];
    if (debug) System.out.println("Reply=" + reply);
    int i1 = reply.indexOf(' ');
    if (i1 == -1) {
      JFLog.log("HTTP:Error:Invalid Reply");
      return false;
    }
    int i2 = reply.substring(i1+1).indexOf(' ');
    if (i2 == -1) {
      JFLog.log("HTTP:Error:Invalid Reply");
      return false;
    }
    code = Integer.valueOf(reply.substring(i1+1, i1+i2+1));
    boolean disconn = reply.endsWith("1.0");
    if (length == 0) {
      if (disconn) close();
      return true;
    }
    //read data
    if (length != -1) {
      if (debug) System.out.println("HTTP:Reading Length:" + length);
      //read length data
      int total = 0;
      if (buf.count > 0) {
        total += buf.count;
        os.write(buf.toArray());
        buf.consumeAll();
      }
      while (total < length) {
        int toread = bufsiz;
        if (toread > length) {
          toread = (int)length;
        }
        int read = read(buf, toread);
        if (read > 0) {
          total += read;
          os.write(buf.toArray());
          buf.consumeAll();
        }
      }
      if (disconn) close();
      return true;
    } else if (chunked) {
      if (debug) System.out.println("HTTP:Reading Chunked reply");
      //read chunked data
      while (true) {
        if (haveChunkLength(buf)) {
          int chunkLength = getChunkLength(buf);
          boolean endOfChunks = chunkLength == 0;
          //read chunk
          if (buf.count > 0) {
            int toCopy = buf.count;
            if (toCopy > chunkLength) toCopy = chunkLength;
            os.write(buf.buf, buf.pos, toCopy);
            chunkLength -= toCopy;
            buf.consume(toCopy);
          }
          while (chunkLength > 0) {
            int toread = chunkLength;
            if (toread > bufsiz) toread = bufsiz;
            int read = read(buf, toread);
            if (read > 0) {
              os.write(buf.buf, buf.pos, read);
              chunkLength -= read;
              buf.consumeAll();
            }
          }
          //read \r\n
          while (buf.count < 2) {
            read(buf, 2 - buf.count);
          }
          buf.consume(2);
          if (endOfChunks) {
            if (disconn) close();
            return true;
          }
        }
        read(buf, bufsiz);
      }
    } else {
      if (debug) System.out.println("HTTP:Reading till carrier dropped");
      //read till carrier is dropped
      do {
        if (buf.count > 0) {
          os.write(buf.toArray());
          buf.consumeAll();
        }
      } while (read(buf, bufsiz) >= 0);
      if (disconn) close();
      return true;
    }
  }

  /** Append user headers. */
  private void appendHeaders(StringBuilder req) {
    String[] keys = request_headers.keySet().toArray(new String[request_headers.keySet().size()]);
    for(String key : keys) {
      String value = request_headers.get(key);
      req.append(key + ": " + value + "\r\n");
    }
  }

  /** HTTP GET using url.
   * Writes content to OutputStream.
   */
  public boolean get(String url, OutputStream os) {
    code = -1;
    if (s == null) return false;
    StringBuilder req = new StringBuilder();
    req.append("GET " + url + " HTTP/1.1\r\n");
    req.append("Host: " + host + (port != 80 ? (":" + port) : "") + "\r\n");
    req.append("Content-Length: 0\r\n");
    req.append("Accept-Encoding: chunked\r\n");
    appendHeaders(req);
    req.append("\r\n");
    try {
      sendRequest(req.toString());
      return getReply(os);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** HTTP GET using url.
   * Returns as a byte[]
   */
  public byte[] get(String url) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (!get(url, baos)) return null;
    return baos.toByteArray();
  }

  /** HTTP GET using url.
   * Returns as a String.
   */
  public String getString(String url) {
    return new String(get(url));
  }

  /** HTTP POST using url with post data encoding with mimeType.
   * Writes content to OutputStream.
   */
  public boolean post(String url, byte[] data, String mimeType, OutputStream os) {
    code = -1;
    if (s == null) return false;
    StringBuilder req = new StringBuilder();
    req.append("POST " + url + " HTTP/1.1\r\n");
    req.append("Host: " + host + (port != 80 ? (":" + port) : "") + "\r\n");
    req.append("Content-Length: " + data.length + "\r\n");
    req.append("Content-Type: " + mimeType + "\r\n");
    req.append("Accept-Encoding: chunked\r\n");
    appendHeaders(req);
    req.append("\r\n");
    try {
      sendRequest(req.toString());
      sendData(data);
      return getReply(os);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** HTTP POST using url with post data encoding with mimeType.
   * Returns content as byte[]
   */
  public byte[] post(String url, byte[] data, String mimeType) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (!post(url, data, mimeType, baos)) return null;
    return baos.toByteArray();
  }

  /** HTTP POST using url with post data encoding with mimeType.
   * Returns content as String.
   */
  public String postString(String url, byte[] data, String mimeType) {
    return new String(post(url, data, mimeType));
  }

  /** Test HTTP */
  public static void main(String[] args) {
    HTTP http = new HTTP();
    String html;
    boolean print = false;
    if (args.length > 0 && args[0].equals("print")) print = true;

    debug = true;

    http.open("google.com");
    html = http.getString("/");
    http.close();
    if (html == null || html.length() == 0) {
      System.out.println("Error:HTTP.get() failed");
      return;
    }
    if (print) System.out.println(html);

    http.open("www.google.com");
    html = http.getString("/");
    http.close();
    if (html == null || html.length() == 0) {
      System.out.println("Error:HTTP.get() failed");
      return;
    }
    if (print) System.out.println(html);
  }
}
