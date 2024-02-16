package javaforce.service;

/**
 * Created : Aug 23, 2013
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class WebRequest {
  //public fields
  public String request;
  public String[] fields;
  public String[] fields0;
  public InputStream is;
  public String[] cookies;  //name=value;...
  public static HashMap<String, Session> sessions = new HashMap<String, Session>();
  public Session session;
  public String serverIP, remoteIP;
  public int serverPort, remotePort;
  public String method;

  public static class Session {
    public String id;
    public HashMap<String, Object> props = new HashMap<String, Object>();
    public void setAttribute(String key, Object value) {props.put(key, value);}
    public Object getAttribute(String key) {return props.get(key);}
  }

  public Session getSession() {return session;}

  public String getQueryString() {
    int idx = fields0[1].indexOf("?");
    if (idx == -1) return "";
    return fields0[1].substring(idx+1);
  }
  public String getURL() {
    int idx = fields0[1].indexOf("?");
    if (idx == -1) return fields0[1];
    return fields0[1].substring(0, idx);
  }
  public String getCookie(String name) {
    name += "=";
    for(int a=0;a<cookies.length;a++) {
      if (cookies[a].startsWith(name)) {
        return cookies[a].substring(name.length());
      }
    }
    return null;
  }
  public String getHeader(String name) {
    name += ":";
    for(int a=0;a<fields.length;a++) {
      if (fields[a].startsWith(name)) {
        return fields[a].substring(name.length()).trim();
      }
    }
    return null;
  }
  private String randomID() {
    Random r = new Random();
    return "" + r.nextLong() + "-" + r.nextLong() + "-" + System.currentTimeMillis();
  }
  void init(WebResponse res) {
    ArrayList<String> list = new ArrayList<String>();
    for(int a=1;a<fields.length;a++) {
      if (fields[a].startsWith("Cookie: ")) {
        String[] sets = fields[a].substring(8).split(";");
        for(int b=0;b<sets.length;b++) {
          list.add(sets[b].trim());
        }
      }
    }
    cookies = list.toArray(JF.StringArrayType);
    String id = getCookie("jsession-id");
    String orgid = id;
    if (id == null) {
      id = randomID();
      res.addCookie("jsession-id", id);
      session = new Session();
      session.id = id;
      sessions.put(id, session);
    } else {
      session = sessions.get(id);
      if (session == null) {
        session = new Session();
        session.id = id;
        sessions.put(id, session);
      }
    }
  }

  public String getHost() {
    String host = getHeader("Host");
    if (host == null) return "";
    int idx = host.indexOf(":");
    if (idx == -1) return host;
    return host.substring(0, idx);
  }

  public String getLocalAddr() { return serverIP; }
  public int getLocalPort() { return serverPort; }

  public String getRemoteAddr() { return remoteIP; }
  public int getRemotePort() { return remotePort; }

  public String getMethod() { return method; }

  public InputStream getInputStream() { return is; }
};
