package javaforce.webui;

import java.io.*;
import java.lang.reflect.*;

import javaforce.*;

/** WebUIServletContext.
 *
 * Server-side context.
 *
 * @author pquiring
 */

public class WebUIServletContext {
  private JFClassLoader loader;

  private Object servlet;
  private Method init;
  private Method destroy;
  private Method getName;

  private Object server;
  private Method startServlet;
  private Method stopServlet;
  private Method connectServlet;

  public WebUIServletContext(JFClassLoader loader, Object servlet) {
    this.loader = loader;
    this.servlet = servlet;

    try {
      Class<?> servlet_cls = loader.findClass("javaforce.webui.WebUIServlet");
      init = servlet_cls.getMethod("init");
      destroy = servlet_cls.getMethod("destroy");
      getName = servlet_cls.getMethod("getName");

      Class<?> server_cls = loader.findClass("javaforce.webui.WebUIServer");
      Constructor<?> server_ctor = server_cls.getConstructor();
      server = server_ctor.newInstance();
      startServlet = server_cls.getMethod("startServlet", servlet_cls);
      stopServlet = server_cls.getMethod("stopServlet");
      connectServlet = server_cls.getMethod("connectServlet", String.class, String.class, InputStream.class, OutputStream.class, String.class, String.class, String[].class);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public JFClassLoader getClassLoader() {
    return loader;
  }

  //start WebUIServer within Servlet context
  public void startServlet() {
    try {
      startServlet.invoke(server, servlet);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //stop WebUIServer within Servlet context
  public void stopServlet() {
    try {
      stopServlet.invoke(server);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void connectServlet(String server_host, String client_host, InputStream is, OutputStream os, String host, String url, String[] cookies) {
    try {
      connectServlet.invoke(server, server_host, client_host, is, os, host, url, cookies);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }


  public void init() {
    try {
      init.invoke(servlet);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void destroy() {
    try {
      destroy.invoke(servlet);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String getName() {
    try {
      return (String)getName.invoke(servlet);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }
}
