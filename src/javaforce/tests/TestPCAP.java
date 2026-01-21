package javaforce.tests;

/** Tests PCAP
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.net.*;

public class TestPCAP {
  private static String nic_ip = null;
  private static String timeout = "2000";

  public static void main(String[] args) {
    if (args.length == 0) {
      JFLog.log("Usage : PacketCapture cmd [...] [opts]");
      JFLog.log("  cmd : list");
      JFLog.log("      : arp {ip}");
      JFLog.log(" opts : -i interface_ip");
      JFLog.log("      : -t timeout");
      return;
    }
    try {
      switch (args[0]) {
        case "list":
          cmd_list();
          break;
        case "arp":
          parse_opts(args);
          cmd_arp(args[1]);
          break;
        default:
          JFLog.log("Unknown cmd:" + args[0]);
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void cmd_list() {
    PacketCapture cap = PacketCapture.getInstance();
    String[] ifs = cap.listLocalInterfaces();
    for(int a=0;a<ifs.length;a++) {
      JFLog.log(ifs[a]);
    }
  }

  public static void cmd_arp(String ip) {
    PacketCapture cap = PacketCapture.getInstance();
    String[] nics = cap.listLocalInterfaces();
    int nicidx = 0;
    if (nic_ip != null) {
      nicidx = -1;
      for(int a=0;a<nics.length;a++) {
        String sif = nics[a];
        String[] pif = sif.split(",");
        for(int b=1;b<pif.length;b++) {
          if (pif[b].equals(nic_ip)) {
            nicidx = a;
            break;
          }
        }
      }
    }
    if (nicidx == -1) {
      JFLog.log("Interface not found for IP:" + nic_ip);
      return;
    }
    String sif = nics[nicidx];
    String[] pif = sif.split(",");
    long id = cap.start(pif[0], pif[1]);
    cap.compile(id, "arp");
    byte[] mac = PacketCapture.arp(id, ip, Integer.valueOf(timeout));
    cap.stop(id);
    System.out.print("MAC=");
    PacketCapture.print_mac(mac);
  }

  private static void parse_opts(String[] args) {
    for(int a=0;a<args.length;a++) {
      if (args[a].startsWith("-")) {
        switch (args[a]) {
          case "-i":
            nic_ip = args[a+1];
            break;
          case "-t":
            timeout = args[a+1];
            break;
        }
      }
    }
  }

}
