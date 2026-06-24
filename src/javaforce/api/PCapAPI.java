package javaforce.api;

import javaforce.ffm.*;
import javaforce.jni.*;

/** pcap API.
 *
 * @author pquiring
 */

public interface PCapAPI {
  public static PCapAPI getInstance() {
    if (FFM.enabled()) {
      return PCapFFM.getInstance();
    } else {
      return PCapJNI.getInstance();
    }
  }
  public boolean pcapInit(String lib1, String lib2);
  public String[] pcapListLocalInterfaces();
  public long pcapStart(String local_interface, boolean nonblocking);
  public void pcapStop(long id);
  public boolean pcapCompile(long handle, String program);
  public byte[] pcapRead(long handle);
  public boolean pcapWrite(long handle, byte[] packet, int offset, int length);
}
