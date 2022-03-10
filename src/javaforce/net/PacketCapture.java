/**
  * Packet Capture API (pcap)
  *
  */

package javaforce.net;

import java.io.*;

import javaforce.jni.JFNative;
import javaforce.*;

public class PacketCapture {

  public static native boolean ninit(String lib);

  /** Load native libraries. */
  public static boolean init() {
    if (JF.isWindows()) {
      String windir = System.getenv("windir").replaceAll("\\\\", "/");
      {
        //npcap
        String dll = windir + "/system32/npcap/wpcap.dll";
        if (new File(dll).exists()) {
          return ninit(dll);
        }
      }
      {
        //pcap
        String dll = windir + "/system32/pcap.dll";
        if (new File(dll).exists()) {
          return ninit(dll);
        }
      }
      return false;
    }
    if (JF.isUnix()) {
      return ninit("/usr/lib/x86_64-linux-gnu/libpcap.so.0.8");
    }
    return false;
  }

  /** List local interfaces. */
  public native String[] listLocalInterfaces();

  /** Start process on local interface. */
  public native long start(String local_interface);

  /** Stop processing. */
  public native void stop(long id);

  /** Request MAC address from ip (ARP).  Timeout in ms. */
  public native String arp(long id, String ip, int timeout);

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage : PacketCapture cmd [...]");
      System.out.println("      : arp {ip} [timeout]");
      System.out.println("      : list");
      return;
    }
    JFNative.load();
    if (!init()) {
      System.out.println("init failed");
      return;
    }
    switch (args[0]) {
      case "list":
        cmd_list();
        break;
      case "arp":
        cmd_arp(args[1]);
        break;
      default:
        System.out.println("Unknown cmd:" + args[0]);
        break;
    }
  }

  public static void cmd_list() {
    PacketCapture cap = new PacketCapture();
    String[] ifs = cap.listLocalInterfaces();
    for(int a=0;a<ifs.length;a++) {
      System.out.println(ifs[a]);
    }
  }

  public static void cmd_arp(String ip) {
    PacketCapture cap = new PacketCapture();
    long id = cap.start(null);  //start on default interface
    String mac = cap.arp(id, ip, 2000);
    System.out.println(mac);
    cap.stop(id);
  }
}
