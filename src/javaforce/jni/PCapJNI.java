package javaforce.jni;

/** pcap native api
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.net.*;

public class PCapJNI implements PacketCapture {

  private static PCapJNI instance;

  private byte[] local_mac;
  private byte[] local_ip;

  public static synchronized PCapJNI getInstance() {
    if (instance == null) {
      instance = new PCapJNI();
      if (!instance.init()) {
        instance = null;
      }
    }
    return instance;
  }

  public native boolean ninit(String lib1, String lib2);

  /** Load native libraries. */
  private boolean init() {
    if (JF.isWindows()) {
      String windir = System.getenv("windir").replaceAll("\\\\", "/");
      {
        //npcap
        String dll1 = windir + "/system32/npcap/packet.dll";
        String dll2 = windir + "/system32/npcap/wpcap.dll";
        if (new File(dll1).exists() && new File(dll2).exists()) {
          return ninit(dll1, dll2);
        }
      }
      {
        //pcap
        String dll1 = windir + "/system32/packet.dll";
        String dll2 = windir + "/system32/wpcap.dll";
        if (new File(dll1).exists() && new File(dll2).exists()) {
          return ninit(dll1, dll2);
        }
      }
      return false;
    }
    if (JF.isUnix()) {
      Library so = new Library("pcap");
      JFNative.findLibraries(new File[] {new File("/usr/lib"), new File(LnxNative.getArchLibFolder())}, new Library[] {so}, ".so");
      return ninit(null, so.path);
    }
    return false;
  }

  public native String[] listLocalInterfaces();

  private native long nstart(String local_interface, boolean nonblocking);

  /** Start process on local interface. */
  public long start(String local_interface, String local_ip, boolean nonblocking) {
    this.local_ip = PacketCapture.decode_ip(local_ip);
    this.local_mac = PacketCapture.get_mac(local_ip);
    return nstart(local_interface, nonblocking);
  }

  /** Start process on local interface with blocking mode enabled. */
  public long start(String local_interface, String local_ip) {
    return start(local_interface, local_ip, true);
  }

  /** Stop processing. */
  public native void stop(long id);

  public native boolean compile(long handle, String program);

  public native byte[] read(long handle);

  public native boolean write(long handle, byte[] packet, int offset, int length);

  public byte[] get_local_ip() {return local_ip;}

  public byte[] get_local_mac() {return local_mac;}

}
