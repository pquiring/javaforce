<%@ page import="java.util.*,java.io.*,java.net.*" %><%

BufferedReader in = request.getReader();  //input POST data

String args = in.readLine();

if (args == null) {out.println("No args"); return;}

String ln[] = args.split("&");

String headers = null;
String secure = null;
String post = null;
boolean isPost = false;

for(int a=0;a<ln.length;a++) {
  if (ln[a].startsWith("headers=")) {headers = ln[a].substring(8); continue;}
  if (ln[a].startsWith("secure=")) {secure = ln[a].substring(7); continue;}
  if (ln[a].startsWith("post=")) {post = ln[a].substring(5); continue;}
}

if (headers == null) {out.println("No headers"); return;}
if (secure == null) {out.println("Secure?"); return;}
if (post != null && post.length() > 0) isPost = true;

//out.println("Headers = " + headers);  //test
//out.println("Post = " + post);  //test

headers = URLDecoder.decode(headers);
if (isPost) post = URLDecoder.decode(post);

ln = headers.split("\r\n");

int hostidx = -1;
boolean disconn = false;

ArrayList<String> relay_key = new ArrayList<String>();
ArrayList<String> relay_value = new ArrayList<String>();

if (ln[0].endsWith("1.0")) disconn = true;  //HTTP/1.0
for(int a=0;a<ln.length;a++) {
  if (ln[a].regionMatches(true, 0, "Host: ", 0, 6)) {
    hostidx = a;
    continue;
  }
  if (ln[a].regionMatches(true, 0, "Cookie: ", 0, 8)) {
    relay_key.add("Cookie");
    relay_value.add(ln[a].substring(8));
  }
  if (ln[a].regionMatches(true, 0, "Referer: ", 0, 9)) {
    relay_key.add("Referer");
    relay_value.add(ln[a].substring(9));
  }
  if (ln[a].regionMatches(true, 0, "Content-Type: ", 0, 14)) {
    relay_key.add("Content-Type");
    relay_value.add(ln[a].substring(14));
  }
  if (ln[a].regionMatches(true, 0, "Content-Length: ", 0, 16)) {
    relay_key.add("Content-Length");
    relay_value.add(ln[a].substring(16));
  }
  if (ln[a].regionMatches(true, 0, "Range: ", 0, 7)) {
    relay_key.add("Range");
    relay_value.add(ln[a].substring(7));
  }
}

if (hostidx == -1) {out.println("No Host"); return;}

String host = null;
int port = 80;
int portidx = ln[hostidx].substring(6).indexOf(':');
if (portidx != -1) {
  host = ln[hostidx].substring(6, portidx);
  port = Integer.valueOf(ln[hostidx].substring(portidx+1));
} else {
  host = ln[hostidx].substring(6);
}

//older webservers don't like the host in the request line
//in fact I didn't even know that some would accept it
String parts[] = ln[0].split(" ");
if ((parts.length == 3) && ((parts[1].startsWith("http://")) || (parts[1].startsWith("https://")))) {
  parts[1] = new URL(parts[1]).getFile();
}

InputStream is;
OutputStream os;

String ct = null;

OutputStream myos = response.getOutputStream();

try {
  URL url = new URL( (secure.equals("true") ? "https://" : "http://") + host + parts[1] );
  URLConnection conn = (URLConnection)url.openConnection();
  for(int a=0;a<relay_key.size();a++) {
    conn.addRequestProperty(relay_key.get(a),relay_value.get(a));
  }
  ((HttpURLConnection)conn).setInstanceFollowRedirects(false);  //must use Instance - without causes access denied exception
  if (isPost) {
    ((HttpURLConnection)conn).setDoOutput(true);
  }
  conn.connect();
  if (isPost) {
    os = conn.getOutputStream();
    os.write(post.getBytes());
    os.flush();
  }
  int sc = ((HttpURLConnection)conn).getResponseCode();
  String msg = ((HttpURLConnection)conn).getResponseMessage();
  StringBuilder newheaders = new StringBuilder();
  newheaders.append("HTTP/1.1 " + sc + " " + msg + "\r\n");
  int cnt = 0;
  while (true) {
    cnt++;
    String key = conn.getHeaderFieldKey(cnt);
    if (key == null) break;
    String value = conn.getHeaderField(cnt);
    newheaders.append(key + ": " + value + "\r\n");
  }
  newheaders.append("\r\n");
  is = conn.getInputStream();
  byte data[] = new byte[is.available()];
  int read = is.read(data);
  myos.write(newheaders.toString().getBytes());
  myos.write(data);
  myos.flush();
} catch (Exception e) {
  String es = e.toString();
  if (es.indexOf("was too large") != -1) {
    //com.google.appengine.api.urlfetch.ResponseTooLargeException
    //max request size is 32MBs
    //try again and split up request into 1MB chunks
    if (isPost) {
      out.println("Response too large with POST");
      return;
    }
    try {
      int pos = 0;
      boolean first = true;
      while (true) {
        URL url = new URL( (secure.equals("true") ? "https://" : "http://") + host + parts[1] );
        URLConnection conn = (URLConnection)url.openConnection();
        for(int a=0;a<relay_key.size();a++) {
          conn.addRequestProperty(relay_key.get(a),relay_value.get(a));
        }
        conn.addRequestProperty("Range", "" + pos + "-" + (pos + 1024*1024 - 1));
        ((HttpURLConnection)conn).setInstanceFollowRedirects(false);  //must use Instance - without causes access denied exception
        conn.connect();
        if (first) {
          first = false;
          int sc = ((HttpURLConnection)conn).getResponseCode();
          String msg = ((HttpURLConnection)conn).getResponseMessage();
          StringBuilder newheaders = new StringBuilder();
          newheaders.append("HTTP/1.1 " + sc + " " + msg + "\r\n");
          int cnt = 0;
          while (true) {
            cnt++;
            String key = conn.getHeaderFieldKey(cnt);
            if (key == null) break;
            String value = conn.getHeaderField(cnt);
            newheaders.append(key + ": " + value + "\r\n");
          }
          newheaders.append("\r\n");
          myos.write(newheaders.toString().getBytes());
        }
        is = conn.getInputStream();
        int size = is.available();
        if (size == 0) break;
        byte data[] = new byte[size];
        int read = is.read(data);
        myos.write(data);
        myos.flush();
        pos += 1024*1024;
      }
    } catch (Exception e2) {
      StringBuffer sb = new StringBuffer();
      sb.append("Exception Large Download:<br>");
      sb.append(e.toString());
      sb.append("<br>");
      StackTraceElement ste[] = e.getStackTrace();
      if (ste != null) {
        for(int a=0;a<ste.length;a++) {
          sb.append("\tat ");
          sb.append(ste[a].toString());
          sb.append("<br>");
        }
      }
      myos.write(sb.toString().getBytes());
    }
    return;
  }
  StringBuffer sb = new StringBuffer();
  sb.append("Exception:<br>");
  sb.append(es);
  sb.append("<br>");
  StackTraceElement ste[] = e.getStackTrace();
  if (ste != null) {
    for(int a=0;a<ste.length;a++) {
      sb.append("\tat ");
      sb.append(ste[a].toString());
      sb.append("<br>");
    }
  }
  myos.write(sb.toString().getBytes());
}

%>