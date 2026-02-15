package javaforce.ffm;

/** UIAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;
import javaforce.ui.*;

public class UIFFM implements UIAPI {

  private FFM ffm;

  private static UIFFM instance;
  public static UIFFM getInstance() {
    if (instance == null) {
      instance = new UIFFM();
      if (!instance.ffm_init()) {
        JFLog.log("UIFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle uiLoadFont;
  public int uiLoadFont(byte[] font,int ptSize,int[] fontinfo,int[] coords,int[] adv,int[] cps,byte[] pixels,int px,int py) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_font = FFM.toMemory(arena, font);MemorySegment _array_fontinfo = FFM.toMemory(arena, fontinfo);MemorySegment _array_coords = FFM.toMemory(arena, coords);MemorySegment _array_adv = FFM.toMemory(arena, adv);MemorySegment _array_cps = FFM.toMemory(arena, cps);MemorySegment _array_pixels = FFM.toMemory(arena, pixels);int _ret_value_ = (int)uiLoadFont.invokeExact(_array_font,ptSize,_array_fontinfo,_array_coords,_array_adv,_array_cps,_array_pixels,px,py);FFM.copyBack(_array_font,font);FFM.copyBack(_array_fontinfo,fontinfo);FFM.copyBack(_array_coords,coords);FFM.copyBack(_array_adv,adv);FFM.copyBack(_array_cps,cps);FFM.copyBack(_array_pixels,pixels);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle uiLoadPNG;
  public int[] uiLoadPNG(byte[] data,int[] dim) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);MemorySegment _array_dim = FFM.toMemory(arena, dim);int[] _ret_value_ = FFM.toArrayInt((MemorySegment)uiLoadPNG.invokeExact(_array_data,_array_dim));FFM.copyBack(_array_data,data);FFM.copyBack(_array_dim,dim);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle uiSavePNG;
  public byte[] uiSavePNG(int[] pixels,int width,int height) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_pixels = FFM.toMemory(arena, pixels);byte[] _ret_value_ = FFM.toArrayByte((MemorySegment)uiSavePNG.invokeExact(_array_pixels,width,height));FFM.copyBack(_array_pixels,pixels);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle uiLoadJPG;
  public int[] uiLoadJPG(byte[] data,int[] dim) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);MemorySegment _array_dim = FFM.toMemory(arena, dim);int[] _ret_value_ = FFM.toArrayInt((MemorySegment)uiLoadJPG.invokeExact(_array_data,_array_dim));FFM.copyBack(_array_data,data);FFM.copyBack(_array_dim,dim);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle uiSaveJPG;
  public byte[] uiSaveJPG(int[] pixels,int width,int height,int quality) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_pixels = FFM.toMemory(arena, pixels);byte[] _ret_value_ = FFM.toArrayByte((MemorySegment)uiSaveJPG.invokeExact(_array_pixels,width,height,quality));FFM.copyBack(_array_pixels,pixels);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle uiInit;
  public boolean uiInit() { try { boolean _ret_value_ = (boolean)uiInit.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle uiWindowCreate;
  public long uiWindowCreate(int style,String title,int width,int height,UIEvents events,long shared) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)uiWindowCreate.invokeExact(style,arena.allocateFrom(title),width,height,events.store(ffm.getFunctionUpCall(events, "dispatchEvent", void.class, new Class[] {int.class, int.class, int.class}, arena)),shared);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle uiWindowDestroy;
  public void uiWindowDestroy(long id) { try { uiWindowDestroy.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiWindowSetCurrent;
  public void uiWindowSetCurrent(long id) { try { uiWindowSetCurrent.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiWindowSetIcon;
  public void uiWindowSetIcon(long id,String icon,int x,int y) { try { Arena arena = Arena.ofAuto(); uiWindowSetIcon.invokeExact(id,arena.allocateFrom(icon),x,y); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiPollEvents;
  public void uiPollEvents(long id,int wait) { try { uiPollEvents.invokeExact(id,wait); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiPostEvent;
  public void uiPostEvent() { try { uiPostEvent.invokeExact(); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiWindowShow;
  public void uiWindowShow(long id) { try { uiWindowShow.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiWindowHide;
  public void uiWindowHide(long id) { try { uiWindowHide.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiWindowSwap;
  public void uiWindowSwap(long id) { try { uiWindowSwap.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiWindowHideCursor;
  public void uiWindowHideCursor(long id) { try { uiWindowHideCursor.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiWindowShowCursor;
  public void uiWindowShowCursor(long id) { try { uiWindowShowCursor.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiWindowLockCursor;
  public void uiWindowLockCursor(long id) { try { uiWindowLockCursor.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiWindowGetPos;
  public void uiWindowGetPos(long id,int[] pos) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_pos = FFM.toMemory(arena, pos);uiWindowGetPos.invokeExact(id,_array_pos);FFM.copyBack(_array_pos,pos); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle uiWindowSetPos;
  public void uiWindowSetPos(long id,int x,int y) { try { uiWindowSetPos.invokeExact(id,x,y); } catch (Throwable t) { JFLog.log(t); } }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("UIAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    uiLoadFont = ffm.getFunctionPtr("_uiLoadFont", ffm.getFunctionDesciptor(JAVA_INT,ADDRESS,JAVA_INT,ADDRESS,ADDRESS,ADDRESS,ADDRESS,ADDRESS,JAVA_INT,JAVA_INT));
    uiLoadPNG = ffm.getFunctionPtr("_uiLoadPNG", ffm.getFunctionDesciptor(ADDRESS,ADDRESS,ADDRESS));
    uiSavePNG = ffm.getFunctionPtr("_uiSavePNG", ffm.getFunctionDesciptor(ADDRESS,ADDRESS,JAVA_INT,JAVA_INT));
    uiLoadJPG = ffm.getFunctionPtr("_uiLoadJPG", ffm.getFunctionDesciptor(ADDRESS,ADDRESS,ADDRESS));
    uiSaveJPG = ffm.getFunctionPtr("_uiSaveJPG", ffm.getFunctionDesciptor(ADDRESS,ADDRESS,JAVA_INT,JAVA_INT,JAVA_INT));
    uiInit = ffm.getFunctionPtr("_uiInit", ffm.getFunctionDesciptor(JAVA_BOOLEAN));
    uiWindowCreate = ffm.getFunctionPtr("_uiWindowCreate", ffm.getFunctionDesciptor(JAVA_LONG,JAVA_INT,ADDRESS,JAVA_INT,JAVA_INT,ADDRESS,JAVA_LONG));
    uiWindowDestroy = ffm.getFunctionPtr("_uiWindowDestroy", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    uiWindowSetCurrent = ffm.getFunctionPtr("_uiWindowSetCurrent", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    uiWindowSetIcon = ffm.getFunctionPtr("_uiWindowSetIcon", ffm.getFunctionDesciptorVoid(JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    uiPollEvents = ffm.getFunctionPtr("_uiPollEvents", ffm.getFunctionDesciptorVoid(JAVA_LONG,JAVA_INT));
    uiPostEvent = ffm.getFunctionPtr("_uiPostEvent", ffm.getFunctionDesciptorVoid());
    uiWindowShow = ffm.getFunctionPtr("_uiWindowShow", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    uiWindowHide = ffm.getFunctionPtr("_uiWindowHide", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    uiWindowSwap = ffm.getFunctionPtr("_uiWindowSwap", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    uiWindowHideCursor = ffm.getFunctionPtr("_uiWindowHideCursor", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    uiWindowShowCursor = ffm.getFunctionPtr("_uiWindowShowCursor", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    uiWindowLockCursor = ffm.getFunctionPtr("_uiWindowLockCursor", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    uiWindowGetPos = ffm.getFunctionPtr("_uiWindowGetPos", ffm.getFunctionDesciptorVoid(JAVA_LONG,ADDRESS));
    uiWindowSetPos = ffm.getFunctionPtr("_uiWindowSetPos", ffm.getFunctionDesciptorVoid(JAVA_LONG,JAVA_INT,JAVA_INT));
    return true;
  }
}
