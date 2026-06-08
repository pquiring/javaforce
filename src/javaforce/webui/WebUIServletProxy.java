package javaforce.webui;

/** WebUIServletProxy
 *
 * @author pquiring
 */

import java.lang.reflect.*;

import javaforce.*;
import javaforce.service.*;

public class WebUIServletProxy implements WebUIServlet {
  private Object servlet;

  private Class<?> cls_string;
  private Class<?> cls_http_params;
  private Class<?> cls_webuiclient;
  private Class<?> cls_webrequest;
  private Class<?> cls_webresponse;

  private Method init;
  private Method destroy;
  private Method getName;
  private Method getPanel;
  private Method getResource;
  private Method clientConnected;
  private Method clientDisconnected;

  private boolean inited;
  private Object lock = new Object();

  private static boolean debug = false;

  public WebUIServletProxy(ClassLoader loader, Class<?> cls, Object servlet) {
    this.servlet = servlet;
    try {
      cls_string = loader.loadClass("java.lang.String");
      cls_http_params = loader.loadClass("javaforce.HTTP$Parameters");
      cls_webuiclient = loader.loadClass("javaforce.webui.WebUIClient");
      cls_webrequest = loader.loadClass("javaforce.service.WebRequest");
      cls_webresponse = loader.loadClass("javaforce.service.WebResponse");
      init = cls.getMethod("init");
      destroy = cls.getMethod("destroy");
      getName = cls.getMethod("getName");
      getPanel = cls.getMethod("getPanel", cls_string, cls_http_params, cls_webuiclient);
      getResource = cls.getMethod("getResource", cls_string, cls_http_params, cls_webrequest, cls_webresponse);
      clientConnected = cls.getMethod("clientConnected", cls_webuiclient);
      clientDisconnected = cls.getMethod("clientDisconnected", cls_webuiclient);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }


  public void init() {
    synchronized (lock) {
      if (inited) return;
      try {
        if (init != null) {
          init.invoke(servlet);
        }
      } catch (Throwable t) {
        JFLog.log(t);
      }
      inited = true;
    }
  }

  public void destroy() {
    synchronized (lock) {
      if (!inited) return;
      try {
        if (destroy != null) {
          destroy.invoke(servlet);
        }
      } catch (Throwable t) {
        JFLog.log(t);
      }
      inited = false;
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

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    try {
      return (Panel)getPanel.invoke(servlet, name, params, client);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebRequest request, WebResponse response) {
    try {
      return (byte[])getResource.invoke(servlet, url, params, request, response);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public void clientConnected(WebUIClient client) {
    try {
      clientConnected.invoke(servlet, client);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void clientDisconnected(WebUIClient client) {
    try {
      clientDisconnected.invoke(servlet, client);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
