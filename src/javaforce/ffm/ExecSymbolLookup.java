package javaforce.ffm;

/** Executable Symbol Lookup
 *
 * Find symbols in the main executable.
 *
 * @author pquiring
 */

import java.util.*;

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;

public class ExecSymbolLookup implements SymbolLookup {

  private MemorySegment handle;  //executable handle
  private MethodHandle getsymbol;  //GetProcAddress() or dlsym()
  private Arena arena;

  private static final int RTLD_LAZY = 1;
  private static final int RTLD_NOW = 2;
  private static final int RTLD_GLOBAL = 0x100;

  private static boolean debug = false;

  public boolean init(FFM ffm) {
    if (debug) JFLog.log("ExecSymbolLookup init");
    try {
      arena = Arena.global();
      if (JF.isWindows()) {
        SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", arena);
        ffm.setSymbolLookup(kernel32);
        MethodHandle GetModuleHandle = ffm.getFunction("GetModuleHandleA", ffm.getFunctionDesciptor(ADDRESS, ADDRESS));
        getsymbol = ffm.getFunction("GetProcAddress", ffm.getFunctionDesciptor(ADDRESS, ADDRESS, ADDRESS));
        try {
          handle = (MemorySegment)GetModuleHandle.invokeExact(MemorySegment.NULL);
        } catch (Throwable t) {
          JFLog.log(t);
        }
      } else {
        MethodHandle dlopen = ffm.getFunction("dlopen", ffm.getFunctionDesciptor(ADDRESS, ADDRESS, JAVA_INT));
        if (debug) JFLog.log("dlopen=" + dlopen);
        getsymbol = ffm.getFunction("dlsym", ffm.getFunctionDesciptor(ADDRESS, ADDRESS, ADDRESS));
        if (debug) JFLog.log("getsymbol=" + getsymbol);
        try {
          handle = (MemorySegment)dlopen.invokeExact(MemorySegment.NULL, RTLD_NOW | RTLD_GLOBAL);
          if (debug) JFLog.log("handle=" + handle);
        } catch (Throwable t) {
          JFLog.log(t);
        }
      }
      ffm.setSymbolLookup(this);
      return true;
    } catch (Throwable t) {
      JFLog.log(t);
      return false;
    }
  }

  public Optional<MemorySegment> find(String name) {
    try {
      if (debug) JFLog.log("lookup:name=" + name);
      MemorySegment sym = (MemorySegment)getsymbol.invokeExact(handle, arena.allocateFrom(name));
      if (debug) JFLog.log("lookup:sym=" + sym);
      return Optional.of(sym);
    } catch (Throwable t) {
      JFLog.log(t);
      return null;
    }
  }

}
