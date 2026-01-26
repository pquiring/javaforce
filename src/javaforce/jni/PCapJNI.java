package javaforce.jni;

/** pcap native api
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.api.*;

public class PCapJNI implements PCapAPI {

  private static PCapJNI instance;

  public static synchronized PCapJNI getInstance() {
    if (instance == null) {
      instance = new PCapJNI();
      if (!instance.init()) {
        instance = null;
      }
    }
    return instance;
  }

  private native boolean pcapInit(String lib1, String lib2);

  /** Load native libraries. */
  private boolean init() {
    if (JF.isWindows()) {
      String windir = System.getenv("windir").replaceAll("\\\\", "/");
      {
        //npcap
        String dll1 = windir + "/system32/npcap/packet.dll";
        String dll2 = windir + "/system32/npcap/wpcap.dll";
        if (new File(dll1).exists() && new File(dll2).exists()) {
          return pcapInit(dll1, dll2);
        }
      }
      {
        //pcap
        String dll1 = windir + "/system32/packet.dll";
        String dll2 = windir + "/system32/wpcap.dll";
        if (new File(dll1).exists() && new File(dll2).exists()) {
          return pcapInit(dll1, dll2);
        }
      }
      return false;
    }
    if (JF.isUnix()) {
      Library so = new Library("pcap");
      JFNative.findLibraries(new File[] {new File("/usr/lib"), new File(LnxNative.getArchLibFolder())}, new Library[] {so}, ".so");
      return pcapInit(null, so.path);
    }
    return false;
  }

  public native String[] pcapListLocalInterfaces();

  public native long pcapStart(String local_interface, boolean nonblocking);

  public native void pcapStop(long id);

  public native boolean pcapCompile(long handle, String program);

  public native byte[] pcapRead(long handle);

  public native boolean pcapWrite(long handle, byte[] packet, int offset, int length);
}
