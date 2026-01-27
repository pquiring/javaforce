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
    }
    return instance;
  }

  public native boolean pcapInit(String lib1, String lib2);

  public native String[] pcapListLocalInterfaces();

  public native long pcapStart(String local_interface, boolean nonblocking);

  public native void pcapStop(long id);

  public native boolean pcapCompile(long handle, String program);

  public native byte[] pcapRead(long handle);

  public native boolean pcapWrite(long handle, byte[] packet, int offset, int length);
}
