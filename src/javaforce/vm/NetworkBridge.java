package javaforce.vm;

/** Network Bridge - "virtual switch"
 *
 * NOTE : Open vSwitch is required for VLAN tagging guest networks.
 *
 * https://docs.openvswitch.org/en/latest/howto/libvirt/
 *
 * delete old bridge:
 *   brctl delbr virbr0
 *
 * setup Open vSwitch Bridge:
 *   ovs-vsctl add-br ovsbr
 *   ovs-vsctl add-port ovsbr eth0
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class NetworkBridge extends NetworkConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  protected NetworkBridge(String name, String type, String iface) {
    super(name);
    this.type = type;
    this.iface = iface;
  }

  public String type;  //br or os
  public String iface;  //physical nic

  private static NetworkBridge[] list_br() {
    ShellProcess p = new ShellProcess();
    p.keepOutput(true);
    String output = p.run(new String[] {"/usr/sbin/brctl", "show"}, true);
    if (output == null) return null;
    /*
bridge name\tbridge id\tSTP enabled\tinterfaces
virbr0\t8000.xxxxxxxxxxxx\tno\teth0...
    */
    String[] lns = output.split("\n");
    ArrayList<NetworkBridge> list = new ArrayList<>();
    for(int a=1;a<lns.length;a++) {
      if (lns[a].length() == 0) continue;
      String[] fs = lns[a].split("\t");
      String br = fs[0];
      String nic = fs[3];
      list.add(new NetworkBridge(br, "br", nic));
    }
    return list.toArray(new NetworkBridge[0]);
  }

  private static NetworkBridge[] list_os() {
    ShellProcess p = new ShellProcess();
    p.keepOutput(true);
    String output = p.run(new String[] {"/usr/bin/ovs-vsctl", "show"}, true);
    if (output == null) return null;
    /*
UUID
    Bridge virbr0
        Port eth0
            Interface eth0
        Port virbr0
            Interface virbr0
                Type: internal
    ovs_version: "3.1.0"
    */
    String[] lns = output.split("\n");
    ArrayList<NetworkBridge> list = new ArrayList<>();
    String br = null;
    String nic = null;
    for(int a=1;a<lns.length;a++) {
      String ln = lns[a].trim();
      if (ln.length() == 0) continue;
      if (ln.startsWith("Bridge ")) {
        br = ln.substring(7);
        continue;
      }
      if (ln.startsWith("Interface ")) {
        if (br == null) continue;
        nic = ln.substring(10);
        if (nic.equals(br)) continue;
        list.add(new NetworkBridge(br, "os", nic));
        br = null;
        nic = null;
      }
    }
    return list.toArray(new NetworkBridge[0]);
  }

  public static final int TYPE_OS = 1;
  public static final int TYPE_BR = 2;
  public static final int TYPE_ALL = 3;

  public static NetworkBridge[] list() {
    return list(TYPE_ALL);
  }

  public static NetworkBridge[] list(int flags) {
    ArrayList<NetworkBridge> list_all = new ArrayList<>();
    if ((flags & TYPE_BR) != 0) {
      NetworkBridge[] list = list_br();
      if (list != null) {
        for(int a=0;a<list.length;a++) {
          list_all.add(list[a]);
        }
      }
    }
    if ((flags & TYPE_OS) != 0) {
      NetworkBridge[] list = list_os();
      if (list != null) {
        for(int a=0;a<list.length;a++) {
          list_all.add(list[a]);
        }
      }
    }
    return list_all.toArray(new NetworkBridge[0]);
  }

  public static NetworkBridge create(String name, String iface) {
    {
      //create bridge
      ShellProcess p = new ShellProcess();
      p.keepOutput(true);
      p.run(new String[] {"/usr/bin/ovs-vsctl", "add-br", name}, true);
    }
    {
      //add nic to bridge
      ShellProcess p = new ShellProcess();
      p.keepOutput(true);
      p.run(new String[] {"/usr/bin/ovs-vsctl", "add-port", name, iface}, true);
    }
    NetworkBridge nic = new NetworkBridge(name, "os", iface);
    nic.link_up();
    return nic;
  }

  private void remove_br() {
    ShellProcess p = new ShellProcess();
    p.keepOutput(true);
    p.run(new String[] {"/usr/sbin/brctl", "delbr", name}, true);
  }

  private void remove_os() {
    ShellProcess p = new ShellProcess();
    p.keepOutput(true);
    p.run(new String[] {"/usr/bin/ovs-vsctl", "del-br", name}, true);
  }

  public boolean remove() {
    switch (type) {
      case "br": remove_br(); break;
      case "os": remove_os(); break;
    }
    return true;
  }
}
