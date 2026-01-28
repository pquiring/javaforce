/**
  * Packet Capture API (pcap)
  *
  */

package javaforce.net;

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.api.*;
import javaforce.ffm.*;
import javaforce.jni.*;

public class PacketCapture {

  public static boolean debug = false;

  private PCapAPI api;

  private static byte[] local_mac;
  private static byte[] local_ip;

  public static int TYPE_IP4 = 0x0800;
  public static int TYPE_ARP = 0x0806;
  public static int TYPE_IP6 = 0x86dd;

  private static PacketCapture instance;

  public static PacketCapture getInstance() {
    if (instance == null) {
      instance = new PacketCapture();
      if (FFM.enabled()) {
        instance.api = PCapFFM.getInstance();
      } else {
        instance.api = PCapJNI.getInstance();
      }
      if (!instance.init()) {
        return null;
      }
    }
    return instance;
  }

  /** Load native libraries. */
  private boolean init() {
    if (JF.isWindows()) {
      String windir = System.getenv("windir").replaceAll("\\\\", "/");
      {
        //npcap
        String dll1 = windir + "/system32/npcap/packet.dll";
        String dll2 = windir + "/system32/npcap/wpcap.dll";
        if (new File(dll1).exists() && new File(dll2).exists()) {
          return api.pcapInit(dll1, dll2);
        }
      }
      {
        //pcap
        String dll1 = windir + "/system32/packet.dll";
        String dll2 = windir + "/system32/wpcap.dll";
        if (new File(dll1).exists() && new File(dll2).exists()) {
          return api.pcapInit(dll1, dll2);
        }
      }
      return false;
    }
    if (JF.isUnix()) {
      Library so = new Library("pcap");
      JFNative.findLibraries(new File[] {new File("/usr/lib"), new File(LnxNative.getArchLibFolder())}, new Library[] {so}, ".so");
      return api.pcapInit(null, so.path);
    }
    return false;
  }

  /** List local interfaces.
   * Return is array of strings, each is comma delimited list.
   * DeviceName,IP/MAC,IP/MAC,...
   */
  public String[] listLocalInterfaces() {
    return api.pcapListLocalInterfaces();
  }

  /** Find interface that contains IP address. */
  public static String findInterface(String ip) {
    String[] ifs = getInstance().listLocalInterfaces();
    if (debug) {
      JFLog.log("local interfaces:" + ifs.length + " found");
    }
    for(int a=0;a<ifs.length;a++) {
      String[] dev_ips = ifs[a].split("[,]");
      if (debug) {
        JFLog.log("local interface:" + dev_ips[0]);
      }
      for(int b=1;b<dev_ips.length;b++) {
        if (dev_ips[b].equals(ip)) {
          return dev_ips[0];
        }
      }
    }
    return null;
  }

  /** Start process on local interface. */
  public long start(String local_interface, String local_ip, boolean nonblocking) {
    this.local_ip = PacketCapture.decode_ip(local_ip);
    this.local_mac = PacketCapture.get_mac(local_ip);
    return api.pcapStart(local_interface, nonblocking);
  }

  /** Start process on local interface with blocking mode enabled. */
  public long start(String local_interface, String local_ip) {
    return start(local_interface, local_ip, true);
  }

  /** Stop processing. */
  public void stop(long id) {
    api.pcapStop(id);
  }

  /** Compile program. */
  public boolean compile(long handle, String program) {
    return api.pcapCompile(handle, program);
  }

  /** Read packet. */
  public byte[] read(long handle) {
    return api.pcapRead(handle);
  }

  /** Write packet. */
  public boolean write(long handle, byte[] packet, int offset, int length) {
    return api.pcapWrite(handle, packet, offset, length);
  }

  public static void print_mac(byte[] mac) {
    if (mac == null) {
      JFLog.log("null");
      return;
    }
    for(int a=0;a<mac.length;a++) {
      if (a > 0) System.out.print(":");
      System.out.print(String.format("%02x", mac[a]));
    }
    JFLog.log("");
  }

  public static byte[] get_mac(String ip) {
    try {
      Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
      while (nics.hasMoreElements()) {
        NetworkInterface interf = nics.nextElement();
        if (interf.isLoopback()) continue;
        Enumeration<InetAddress> nic_ips = interf.getInetAddresses();
        while (nic_ips.hasMoreElements()) {
          String nic_ip = nic_ips.nextElement().getHostAddress();
          if (nic_ip.equals(ip)) {
            byte[] mac = interf.getHardwareAddress();
            if (debug) {
              JFLog.log("IP=" + ip);
              print_mac(mac);
            }
            return mac;
          }
        }
      }
      JFLog.log("Error:mac not found:" + ip);
      return mac_zero;
    } catch (Exception e) {
      JFLog.log("Exception:" + e);
      return null;
    }
  }

  public static String build_mac(byte[] mac) {
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<mac.length;a++) {
      if (a > 0) sb.append(':');
      sb.append(String.format("%02x", mac[a] & 0xff));
    }
    return sb.toString();
  }

  public static boolean valid_ip(String ip) {
    return IP4.isIP(ip);
  }

  public static byte[] decode_ip(String ip) {
    String[] ips = ip.split("[.]");
    byte[] ret = new byte[ips.length];
    for(int a=0;a<ips.length;a++) {
      ret[a] = (byte)(int)Integer.valueOf(ips[a]);
    }
    return ret;
  }

  public static String build_ip(byte[] ip) {
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<ip.length;a++) {
      if (a > 0) sb.append('.');
      sb.append(String.format("%d", ip[a] & 0xff));
    }
    return sb.toString();
  }

  public static boolean compare_ip(byte[] ip1, byte[] ip2) {
    if (ip1.length != ip2.length) return false;
    for(int a=0;a<ip1.length;a++) {
      if (ip1[a] != ip2[a]) return false;
    }
    return true;
  }

  public static void increment_ip(byte[] ip) {
    int pos = ip.length - 1;
    while ((ip[pos] & 0xff) == 255) {
      ip[pos] = 0;
      pos--;
      if (pos == -1) pos = ip.length - 1;
    }
    ip[pos]++;
  }

  public static int get_ip_range_length(byte[] ip_start, byte[] ip_end) {
    int start32 = BE.getuint32(ip_start, 0);
    int end32 = BE.getuint32(ip_end, 0);
    if (end32 < start32) return -1;
    return end32 - start32 + 1;
  }

  /** Build ethernet header. (14 bytes) */
  public static void build_ethernet(byte[] pkt, byte[] dest, byte[] src, int type) {
    //dest MAC (6)
    //src  MAC (6)
    //type (2)
    int offset = 0;
    System.arraycopy(dest, 0, pkt, offset, 6); offset += 6;
    System.arraycopy(src, 0, pkt, offset, 6); offset += 6;
    BE.setuint16(pkt, offset, type); offset += 2;
  }

  public static int ethernet_size = 14;

  public static int get_ethernet_type(byte[] pkt) {
    return BE.getuint16(pkt, 12);
  }

  public static byte[] mac_broadcast = {-1,-1,-1,-1,-1,-1};
  public static byte[] mac_zero = {0,0,0,0,0,0};

  public static byte[] ip_broadcast = {-1,-1,-1,-1};
  public static byte[] ip_zero = {0,0,0,0};

  public static int ARP_REQUEST = 0x0001;
  public static int ARP_REPLY = 0x0002;

  /** Build ARP header. (28 bytes) */
  public static void build_arp(byte[] pkt, byte[] src_mac, byte[] src_ip, byte[] request_ip) {
    //hw_type (2) = 0x0001
    //proto   (2) = 0x0800 (IP4)
    //hw_size (1) = 6
    //pt_size (1) = 4
    //opcode  (2) = req=0x0001 reply=0x0002
    //src MAC (6)
    //src IP  (4)
    //dst MAC (6) = all zeros
    //dst IP  (4) = requested IP ?
    int offset = ethernet_size;
    BE.setuint16(pkt, offset, 0x0001); offset += 2;  //hw_type
    BE.setuint16(pkt, offset, 0x0800); offset += 2; //proto (IP4)
    pkt[offset] = 6; offset++;  //hw_size
    pkt[offset] = 4; offset++; //pt_size
    BE.setuint16(pkt, offset, ARP_REQUEST); offset += 2; //request
    System.arraycopy(src_mac, 0, pkt, offset, 6); offset += 6;  //src MAC
    System.arraycopy(src_ip, 0, pkt, offset, 4); offset += 4;  //src IP
    System.arraycopy(mac_zero, 0, pkt, offset, 6); offset += 6;  //request MAC
    System.arraycopy(request_ip, 0, pkt, offset, 4); offset += 4;  //request IP
  }

  public static int arp_size = 28;

  public static int get_arp_opcode(byte[] pkt) {
    return BE.getuint16(pkt, ethernet_size + 6);
  }

  public static boolean arp_ip_equals(byte[] pkt, byte[] ip) {
    int pkt_offset = ethernet_size + arp_size - 14;
    for(int a=0;a<ip.length;a++) {
      if (pkt[pkt_offset++] != ip[a]) return false;
    }
    return true;
  }

  public static byte[] get_arp_mac(byte[] pkt) {
    int pkt_offset = ethernet_size + arp_size - 20;  //src_mac
    byte[] ret = new byte[6];
    System.arraycopy(pkt, pkt_offset, ret, 0, 6);
    return ret;
  }

  /** Returns MAC address for IP address. */
  public static byte[] arp(long handle, String target_ip, int ms) {
    PacketCapture pcap = getInstance();
    //padding (packet must not be < 52 bytes)
    if (debug) {
      JFLog.log("arp.timeout=" + ms);
    }
    byte[] ip = decode_ip(target_ip);
    byte[] pkt = new byte[ethernet_size + arp_size + 18];  //18 = padding
    build_ethernet(pkt, mac_broadcast, local_mac, TYPE_ARP);
    build_arp(pkt, local_mac, local_ip, ip);
    if (debug) {
      JFLog.log("arp.write()");
    }
    pcap.write(handle, pkt, 0, pkt.length);
    int time = 0;
    int pkt_length = ethernet_size + arp_size;  //min size
    while (time < ms) {
      do {
        if (debug) {
          JFLog.log("arp.read()");
        }
        pkt = pcap.read(handle);
        if (debug) {
          JFLog.log("arp.pkt=" + pkt);
        }
        if (pkt != null) {
          if (pkt.length >= pkt_length) {
            if (get_ethernet_type(pkt) == TYPE_ARP) {
              if (get_arp_opcode(pkt) == ARP_REPLY) {
                if (arp_ip_equals(pkt, ip)) {
                  return get_arp_mac(pkt);
                }
              }
            }
          }
        }
      } while (pkt != null);
      JF.sleep(100);
      time += 100;
    }
    return null;
  }
}
