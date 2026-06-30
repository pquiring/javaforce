package javaforce.ffm;

import java.awt.*;
import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;
import javaforce.linux.*;

/** X11API FFM implementation.
 *
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */


public class X11FFM implements X11API {

  private FFM ffm;

  private static X11FFM instance;
  public static X11FFM getInstance() {
    if (instance == null) {
      instance = new X11FFM();
      if (!instance.ffm_init()) {
        JFLog.log("X11FFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle x11_get_id;
  public long x11_get_id(Window w) { try { long _ret_value_ = (long)x11_get_id.invokeExact(FFM.ref_object(w));FFM.unref_object(w);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle x11_set_desktop;
  public void x11_set_desktop(long xid) { try { x11_set_desktop.invokeExact(xid); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_set_dock;
  public void x11_set_dock(long xid) { try { x11_set_dock.invokeExact(xid); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_set_strut;
  public void x11_set_strut(long xid,int panelHeight,int x,int y,int width,int height) { try { x11_set_strut.invokeExact(xid,panelHeight,x,y,width,height); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_tray_main;
  public void x11_tray_main(long parentid,int screenWidth,int trayPos,int trayHeight) { try { x11_tray_main.invokeExact(parentid,screenWidth,trayPos,trayHeight); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_tray_reposition;
  public void x11_tray_reposition(int screenWidth,int trayPos,int trayHeight) { try { x11_tray_reposition.invokeExact(screenWidth,trayPos,trayHeight); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_tray_width;
  public int x11_tray_width() { try { int _ret_value_ = (int)x11_tray_width.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle x11_tray_stop;
  public void x11_tray_stop() { try { x11_tray_stop.invokeExact(); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_set_listener;
  public void x11_set_listener(X11Listener listener) { try { FFM.setX11Listener(listener);x11_set_listener.invokeExact(FFM.upcall_X11Listener); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_window_list_main;
  public void x11_window_list_main() { try { x11_window_list_main.invokeExact(); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_window_list_stop;
  public void x11_window_list_stop() { try { x11_window_list_stop.invokeExact(); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_minimize_all;
  public void x11_minimize_all() { try { x11_minimize_all.invokeExact(); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_raise_window;
  public void x11_raise_window(long xid) { try { x11_raise_window.invokeExact(xid); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_map_window;
  public void x11_map_window(long xid) { try { x11_map_window.invokeExact(xid); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_unmap_window;
  public void x11_unmap_window(long xid) { try { x11_unmap_window.invokeExact(xid); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle x11_keysym_to_keycode;
  public int x11_keysym_to_keycode(char keysym) { try { int _ret_value_ = (int)x11_keysym_to_keycode.invokeExact(keysym);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle x11_send_event;
  public boolean x11_send_event(int keycode,boolean down) { try { boolean _ret_value_ = (boolean)x11_send_event.invokeExact(keycode,down);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  public boolean x11_send_event(long id,int keycode,boolean down) { try { boolean _ret_value_ = (boolean)x11_send_event.invokeExact(id,keycode,down);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("X11APIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    x11_get_id = ffm.getFunctionPtr("_x11_get_id", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS));
    x11_set_desktop = ffm.getFunctionPtr("_x11_set_desktop", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    x11_set_dock = ffm.getFunctionPtr("_x11_set_dock", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    x11_set_strut = ffm.getFunctionPtr("_x11_set_strut", ffm.getFunctionDesciptorVoid(JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    x11_tray_main = ffm.getFunctionPtr("_x11_tray_main", ffm.getFunctionDesciptorVoid(JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT));
    x11_tray_reposition = ffm.getFunctionPtr("_x11_tray_reposition", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT));
    x11_tray_width = ffm.getFunctionPtr("_x11_tray_width", ffm.getFunctionDesciptor(JAVA_INT));
    x11_tray_stop = ffm.getFunctionPtr("_x11_tray_stop", ffm.getFunctionDesciptorVoid());
    x11_set_listener = ffm.getFunctionPtr("_x11_set_listener", ffm.getFunctionDesciptorVoid(ADDRESS));
    x11_window_list_main = ffm.getFunctionPtr("_x11_window_list_main", ffm.getFunctionDesciptorVoid());
    x11_window_list_stop = ffm.getFunctionPtr("_x11_window_list_stop", ffm.getFunctionDesciptorVoid());
    x11_minimize_all = ffm.getFunctionPtr("_x11_minimize_all", ffm.getFunctionDesciptorVoid());
    x11_raise_window = ffm.getFunctionPtr("_x11_raise_window", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    x11_map_window = ffm.getFunctionPtr("_x11_map_window", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    x11_unmap_window = ffm.getFunctionPtr("_x11_unmap_window", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    x11_keysym_to_keycode = ffm.getFunctionPtr("_x11_keysym_to_keycode", ffm.getFunctionDesciptor(JAVA_INT,JAVA_CHAR));
    x11_send_event = ffm.getFunctionPtr("_x11_send_event", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_INT,JAVA_BOOLEAN));
    return true;
  }
}
