package javaforce.webui;

/** WebUIServletContext
 *
 * Server-side context.
 *
 * @author pquiring
 */

import java.io.*;
import java.lang.reflect.*;

import javaforce.*;

public class WebUIServletContext {
  private Object servlet;
  private Method init;
  private Method destroy;
  private Method getName;

  private Object server;
  private Method start;
  private Method stop;
  private Method connectServlet;

  public WebUIServletContext(JFClassLoader loader, Object servlet) {
    this.servlet = servlet;

    try {
      Class<?> servlet_cls = servlet.getClass();
      init = servlet_cls.getMethod("init");
      destroy = servlet_cls.getMethod("destroy");
      getName = servlet_cls.getMethod("getName");

      Class<?> server_cls = loader.findClass("javaforce.webui.WebUIServer");
      Constructor<?> server_ctor = server_cls.getConstructor();
      server = server_ctor.newInstance();
      start = server_cls.getMethod("startServlet", servlet_cls);
      stop = server_cls.getMethod("stopServlet");
      connectServlet = server_cls.getMethod("connectServlet", String.class, InputStream.class, OutputStream.class);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //start WebUIServer within Servlet context
  public void server_start() {
    try {
      start.invoke(server, servlet);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //stop WebUIServer within Servlet context
  public void server_stop() {
    try {
      stop.invoke(server);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void connectServlet(String host, InputStream is, OutputStream os) {
    try {
      connectServlet.invoke(server, host, is, os);
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
