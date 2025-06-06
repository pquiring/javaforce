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
  private Parameters request_headers = new Parameters();
  private Parameters reply_headers = new Parameters();
  private String accept = "*/*";
  private int code = -1;
  public static boolean debug = false;
  private Progress progress;
  private static int timeout = 30000;
  private static String user_agent = "JavaForce.HTTP";

  //form types : see RFC 1867, 7568
  /** Form Type for URL encoded data. */
  public static final String formType = "application/x-www-form-urlencoded";
  /** Form Type for multipart data (supports files). */
  public static final String formTypeMultiPart = "multipart/form-data";

  /** Part Type for plain text. */
  public static final String partTypeText = "text/plain";
  /** Part Type for html text. */
  public static final String partTypeHTML = "text/html";
  /** Part Type for binary data. */
  public static final String partTypeStream = "application/octet-stream";
  /** Part Type for zip file. */
  public static final String partTypeZip = "application/x-zip-compressed";

  private final static int bufsiz = 1024;

  /** HTTP Parameters.
   * Stores HTTP Headers or URL Parameters.
   */
  public static class Parameters {
    private HashMap<String, String> params = new HashMap<>();

    public String get(String name) {
      return params.get(name);
    }

    public void put(String name, String value) {
      params.put(name, value);
    }

    public String[] keys() {
      return params.keySet().toArray(JF.StringArrayType);
    }

    public void clear() {
      params.clear();
    }

    public HashMap<String, String> getHashMap() {
      return params;
    }

    /** Decode HTTP headers starting at offset. */
    public static Parameters decode(String[] headers, char equals, int offset) {
      Parameters p = new Parameters();
      for(int i=offset;i<headers.length;i++) {
        String ln = headers[i];
        int idx = ln.indexOf(equals);
        if (idx == -1) continue;
        String key = ln.substring(0, idx).trim();
        String value = ln.substring(idx + 1);
        if (value.length() > 1) {
          value = value.trim();
        }
        p.put(key, value);
      }
      return p;
    }

    /** Decode HTTP headers.  Searches for headers after first blank line.
     * equals = ':'
     */
    public static Parameters decode(String[] headers) {
      for(int i=0;i<headers.length;i++) {
        if (headers[i].length() == 0) return decode(headers, ':', i + 1);
      }
      return null;
    }

    /** Decode HTTP headers.  Searches for headers after first blank line. */
    public static Parameters decode(String[] headers, char equals) {
      for(int i=0;i<headers.length;i++) {
        if (headers[i].length() == 0) return decode(headers, equals, i + 1);
      }
      return null;
    }

    /** Decode URL parameters. */
    public static Parameters decode(String query) {
      Parameters params = new Parameters();
      String[] items = query.split("&");
      for(String item : items) {
        int idx = item.indexOf('=');
        if (idx == -1) continue;
        String key = item.substring(0, idx);
        String value = item.substring(idx + 1);
        params.put(key, JF.decodeURL(value));
      }
      return params;
    }
  }

  /** Part a of multipart POST */
  public static class Part {
    public String name;
    public String mimeType;
    public String filename;  //optional
    public byte[] data;
  }

  /** Progress Listener */
  public static interface Progress {
    /** Invoked to update download progress.
     * @param downloaded = progress so far
     * @param length = total length (-1 = unknown)
     */
    public void progress(long downloaded, long length);
  }

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

  /** Removes user info from HTTP URL. */
  public static String cleanURL(String url) {
    return JF.cleanURL(url);
  }

  public static String getUserInfo(String urlstr) {
    try {
      return new URI(urlstr).toURL().getUserInfo();
    } catch (Exception e) {
      return null;
    }
  }

  public static String getHost(String urlstr) {
    try {
      return new URI(urlstr).toURL().getHost();
    } catch (Exception e) {
      return null;
    }
  }

  public static int getPort(String urlstr) {
    try {
      return new URI(urlstr).toURL().getPort();
    } catch (Exception e) {
      return 80;
    }
  }

  /** Set connect/read timeout in ms (default = 30000) */
  public static void setTimeout(int timeout) {
    HTTP.timeout = timeout;
  }

  public boolean open(String host) {
    return open(host, 80);
  }

  public boolean open(String host, int port) {
    this.host = host;
    this.port = port;
    try {
      if (debug) JFLog.log("HTTP.connect:" + host + ":" + port);
      s = new Socket();
      s.connect(new InetSocketAddress(host, port), timeout);
      s.setSoTimeout(timeout);
      os = s.getOutputStream();
      is = s.getInputStream();
    } catch (Exception e) {
      JFLog.log(e);
      if (s != null) {
        try {s.close();} catch (Exception e2) {}
        s = null;
      }
      s = null;
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

  /** Returns connection status. */
  public boolean isConnected() {
    return s != null;
  }

  /** Registers a download progress listener. */
  public void setProgressListener(Progress progress) {
    this.progress = progress;
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
    return reply_headers.getHashMap();
  }

  /** Returns last status code. */
  public int getCode() {
    return code;
  }

  /** Set Accept header value. */
  public void setAccept(String accept) {
    this.accept = accept;
  }

  /** Set User-Agent header value. */
  public static void setUserAgent(String ua) {
    user_agent = ua;
  }

  private int read(Buffer buf, int length) throws Exception {
    if (buf.count >= length) return length;
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

  private void sendString(String req) throws Exception {
    write(req.getBytes());
  }

  private void sendData(byte[] data) throws Exception {
    write(data);
  }

  private void sendEnter() throws Exception {
    write("\r\n".getBytes());
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
      String value = ln.substring(idx+1);
      if (value.length() > 1) {
        value = value.trim();
      }
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
    long copied = 0;
    //read data
    if (chunked) {
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
            copied += toCopy;
          }
          while (chunkLength > 0) {
            int toread = chunkLength;
            if (toread > bufsiz) toread = bufsiz;
            int read = read(buf, toread);
            if (read > 0) {
              copied += read;
              if (progress != null) {
                progress.progress(copied, -1);
              }
              os.write(buf.buf, buf.pos, read);
              chunkLength -= read;
              buf.consume(read);
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
        } else {
          read(buf, bufsiz);
        }
      }
    } else if (length != -1) {
      if (debug) System.out.println("HTTP:Reading Length:" + length);
      //read length data
      if (buf.count > 0) {
        copied += buf.count;
        os.write(buf.toArray());
        buf.consumeAll();
      }
      while (copied < length) {
        int toread = bufsiz;
        if (toread > length) {
          toread = (int)length;
        }
        int read = read(buf, toread);
        if (read > 0) {
          copied += read;
          if (progress != null) {
            progress.progress(copied, length);
          }
          os.write(buf.toArray());
          buf.consume(read);
        }
      }
      if (disconn) close();
      return true;
    } else {
      if (debug) System.out.println("HTTP:Reading till carrier dropped");
      //read till carrier is dropped
      do {
        if (buf.count > 0) {
          copied += buf.count;
          if (progress != null) {
            progress.progress(copied, -1);
          }
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
    String[] keys = request_headers.keys();
    for(String key : keys) {
      String value = request_headers.get(key);
      req.append(key + ": " + value + "\r\n");
    }
  }

  /** HTTP GET using url.
   * Writes content to OutputStream.
   */
  public boolean get(String url, OutputStream os) {
    if (debug) {
      JFLog.log("HTTP.get():" + url);
    }
    code = -1;
    if (s == null) return false;
    StringBuilder req = new StringBuilder();
    req.append("GET " + url + " HTTP/1.1\r\n");
    req.append("Host: " + host + (port != 80 ? (":" + port) : "") + "\r\n");
    req.append("User-Agent: " + user_agent + "\r\n");
    req.append("Content-Length: 0\r\n");
    req.append("Accept: " + accept + "\r\n");
    req.append("Accept-Encoding: chunked\r\n");
    appendHeaders(req);
    req.append("\r\n");
    if (debug) {
      JFLog.log("request=" + req.toString());
    }
    try {
      sendString(req.toString());
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

  private void sendBoundaryHashes() throws Exception {
    write("--".getBytes());
  }

  private void sendBoundary(String boundary) throws Exception {
    sendBoundaryHashes();
    write(boundary.getBytes());
  }

  private String makePart(Part part) {
    StringBuilder buf = new StringBuilder();
    buf.append(("Content-Disposition: form-data; name=\"" + part.name + "\""));
    if (part.filename != null) {
      buf.append(("; filename=\"" + part.filename + "\""));
    }
    buf.append("\r\n");
    buf.append(("Content-Type: " + part.mimeType + "\r\n"));
    buf.append(("Content-Length: " + part.data.length + "\r\n"));
    buf.append("\r\n");  //end of headers
    return buf.toString();
  }

  private void sendPart(Part part) throws Exception {
    String buf = makePart(part);
    sendString(buf);
  }

  /** HTTP POST (multipart/form-data encoded)
   * Writes content to OutputStream.
   */
  public boolean post(String url, Part[] parts, OutputStream os) {
    code = -1;
    if (s == null) return false;
    StringBuilder req = new StringBuilder();
    req.append("POST " + url + " HTTP/1.1\r\n");
    req.append("Host: " + host + (port != 80 ? (":" + port) : "") + "\r\n");
    req.append("User-Agent: " + user_agent + "\r\n");
    req.append("Accept: " + accept + "\r\n");
    req.append("Accept-Encoding: chunked\r\n");
    appendHeaders(req);
    String boundary = "----JavaForceHTTP" + System.currentTimeMillis();
    int boundary_length = boundary.length();
    req.append("Content-Type: " + formTypeMultiPart + "; boundary=" + boundary + "\r\n");
    long length = 0;
    {
      boolean first = true;
      for(Part part : parts) {
        if (first) {
          first = false;
        } else {
          length += 2;  //\r\n
        }
        length += 2 + boundary_length;
        length += 2;  //\r\n
        length += makePart(part).length();
        length += part.data.length;
        length += 2;  //\r\n
        length += 2 + boundary_length;
      }
      length += 4;  //hashes + \r\n
    }
    req.append("Content-Length: " + length + "\r\n");
    req.append("\r\n");
    try {
      sendString(req.toString());
      boolean first = true;
      for(Part part : parts) {
        if (first) {
          first = false;
        } else {
          sendEnter();
        }
        sendBoundary(boundary);
        sendEnter();
        sendPart(part);
        sendData(part.data);
        sendEnter();
        sendBoundary(boundary);
      }
      sendBoundaryHashes();  //end of transmission
      sendEnter();
      return getReply(os);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** HTTP POST (multipart/form-data encoded)
   * @return content (null on error)
   */
  public byte[] post(String url, Part[] parts) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (!post(url, parts, baos)) return null;
    return baos.toByteArray();
  }

  /** HTTP POST (URL encoded)
   * Writes content to OutputStream.
   */
  public boolean post(String url, byte[] data, OutputStream os, String encType) {
    code = -1;
    if (s == null) return false;
    StringBuilder req = new StringBuilder();
    req.append("POST " + url + " HTTP/1.1\r\n");
    req.append("Host: " + host + (port != 80 ? (":" + port) : "") + "\r\n");
    req.append("User-Agent: " + user_agent + "\r\n");
    req.append("Accept: " + accept + "\r\n");
    req.append("Accept-Encoding: chunked\r\n");
    if (encType == null) {
      encType = "application/x-www-form-urlencoded";
    }
    req.append("Content-Type: " + encType + "\r\n");
    req.append("Content-Length: " + data.length + "\r\n");
    appendHeaders(req);
    req.append("\r\n");
    try {
      sendString(req.toString());
      sendData(data);
      return getReply(os);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** HTTP POST (URL encoded)
   * Writes content to OutputStream.
   */
  public boolean post(String url, byte[] data, OutputStream os) {
    return post(url, data, os, null);
  }

  /** HTTP POST (URL encoded)
   * Returns content as byte[]
   */
  public byte[] post(String url, byte[] data) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (!post(url, data, baos, null)) return null;
    return baos.toByteArray();
  }

  /** HTTP POST (URL encoded)
   * Returns content as byte[]
   */
  public byte[] post(String url, byte[] data, String encType) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (!post(url, data, baos, encType)) return null;
    return baos.toByteArray();
  }

  /** HTTP POST using url with post data using enctype = application/x-www-form-urlencoded.
   * Returns content as String.
   */
  public String postString(String url, byte[] data) {
    return new String(post(url, data));
  }

  /** Test HTTP */
  public static void main(String[] args) {
    HTTP http = new HTTP();
    String html;
    byte[] data;
    boolean print = false;
    if (args.length > 0 && args[0].equals("print")) print = true;

    debug = true;

    http.open("google.com");
    html = http.getString("/");
    http.close();
    if (html == null || html.length() == 0) {
      System.out.println("Error:HTTP.get() failed");
    }
    if (print) System.out.println(html);

    http.open("www.google.com");
    html = http.getString("/");
    http.close();
    if (html == null || html.length() == 0) {
      System.out.println("Error:HTTP.get() failed");
    }
    if (print) System.out.println(html);

    //test post file to example.com
    Part part = new Part();
    part.name = "file";
    part.filename = "file.zip";
    part.data = new byte[] {1,2,3};
    part.mimeType = partTypeStream;
    http.open("example.com");
    data = http.post("/upload", new Part[] {part});
    http.close();
    if (data == null || data.length == 0) {
      System.out.println("Error:HTTP.post() failed");
      return;
    }
    if (print) {
      System.out.println(new String(data));
    }
  }

  /**
   * Returns HTTP style parameter from list of parameters.
   *
   * @param params = HTTP list of parameters
   * @param name = name of parameter to return
   * @return parameter
   */
  public static String getParameter(String[] params, String name) {
    for(int a=0;a<params.length;a++) {
      String param = params[a];
      int idx = param.indexOf(":");
      if (idx == -1) continue;
      String key = param.substring(0, idx).trim();
      String value = param.substring(idx + 1);
      if (value.length() > 1) {
        value = value.trim();
      }
      if (key.equalsIgnoreCase(name)) {
        return value;
      }
    }
    return null;
  }

  /**
   * Returns HTTP style parameter(s) from list of parameters.
   *
   * @param params = HTTP list of parameters
   * @param name = name of parameter to return
   * @return parameter(s)
   */
  public static String[] getParameters(String[] params, String name) {
    ArrayList<String> lst = new ArrayList<>();
    for(int a=0;a<params.length;a++) {
      String param = params[a];
      int idx = param.indexOf(":");
      if (idx == -1) continue;
      String key = param.substring(0, idx).trim();
      String value = param.substring(idx + 1);
      if (value.length() > 1) {
        value = value.trim();
      }
      if (key.equalsIgnoreCase(name)) {
        lst.add(value);
      }
    }
    return lst.toArray(JF.StringArrayType);
  }

  /**
   * Get Content from a HTTP message.
   */
  public static String[] getContent(String[] msg) {
    ArrayList<String> content = new ArrayList<String>();
    for (int a = 0; a < msg.length; a++) {
      //look for double \r\n (which appears as an empty line) that marks end of headers
      if (msg[a].length() == 0) {
        for (int b = a + 1; b < msg.length; b++) {
          content.add(msg[b]);
        }
        break;
      }
    }
    return content.toArray(JF.StringArrayType);
  }
}
