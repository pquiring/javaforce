package jfqemu;

/**
 * Created : Apr 21, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.jbus.*;

public class Server {
  public static void main(String args[]) {
    new Server().start();
  }
  private JBusClient jbusClient;
  private Vector<String> vmsFile = new Vector<String>();
  private Vector<VM> vms = new Vector<VM>();
  private String configFile = "/etc/jfqemu.lst";

  private void start() {
    JFLog.init("/var/log/jfqemu.log", true);
    loadConfig();
    jbusClient = new JBusClient("org.jflinux.service.jfqemu", new JBusMethods());
    jbusClient.start();
  }
  private void loadConfig() {
    try {
      File config = new File(configFile);
      if (!config.exists()) return;
      FileInputStream fis = new FileInputStream(config);
      String lst = new String(JF.readAll(fis));
      fis.close();
      String lns[] = lst.split("\n");
      for(int a=0;a<lns.length;a++) {
        File vmxml = new File(lns[a]);
        String vmxmlfn = vmxml.getAbsolutePath();
        if (!vmxml.exists()) continue;  //exists?
        if (vmsFile.contains(vmxmlfn)) continue;  //dup file
        if (!loadVM(vmxmlfn)) continue;  //dup service ID
        vmsFile.add(vmxmlfn);
        _startVM(vmxmlfn);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  private boolean loadVM(String vmxml) {
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(vmxml);
      xml.read(fis);
      fis.close();
      VM vm = new VM();
      xml.writeClass(vm);
      for(int a=0;a<vms.size();a++) {
        if (vms.get(a).serviceID == vm.serviceID) return false;  //dup serviceID
      }
      vms.add(vm);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  private void saveConfig() {
    try {
      StringBuilder sb = new StringBuilder();
      for(int a=0;a<vmsFile.size();a++) {
        sb.append(vmsFile.get(a));
        sb.append("\n");
      }
      FileOutputStream fos = new FileOutputStream(configFile);
      fos.write(sb.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void _startVM(String file) {
    //refresh VM
    _removeVM(file);
    if (!_addVM(file)) return;  //dup?
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(file);
      xml.read(fis);
      fis.close();
      VM vm = new VM();
      xml.writeClass(vm);
      if (vm.serviceID == -1) throw new Exception("vm is not configured as a service:" + file);
      String cmd[] = vm.getCMD(true);
      Runtime.getRuntime().exec(cmd);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void _removeVM(String file) {
    if (!vmsFile.contains(file)) return;
    vmsFile.remove(file);
    saveConfig();
  }
  public boolean _addVM(String file) {
    if (vmsFile.contains(file)) return false;  //dup file
    if (!loadVM(file)) return false;  //dup serviceID
    vmsFile.add(file);
    saveConfig();
    return true;
  }
  public class JBusMethods {
    //stanard service methods
    public void stop() {
      System.exit(0);
    }
    public void status(String pack) {
      jbusClient.call(pack, "serviceStatus", "\"jQEMU running:" + JF.getPID() + "\"");
    }
    public void addVM(String file) {_addVM(file);}
    public void removeVM(String file) {_removeVM(file);}
    public void startVM(String file) {_startVM(file);}
  }
}
