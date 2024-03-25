package service;

/** Startup.
 *
 * Starts
 *  - networking
 *  - storage systems
 *  - virtual machines
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.vm.*;

public class Startup extends Thread {
  public void run() {
    //rather than trying to program all the different network mgrs each distro could have it's easier to just do it here
    try {
      //start physical interfaces
      NetworkInterface[] nics = NetworkInterface.listPhysical();
      for(NetworkInterface nic : nics) {
        nic.link_up();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    try {
      //start network bridges
      NetworkBridge[] nics = NetworkBridge.list();
      for(NetworkBridge nic : nics) {
        nic.link_up();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    try {
      //start network virtual nics
      ArrayList<NetworkVirtual> nics = Config.current.nics;
      for(NetworkVirtual nic : nics) {
        nic.start();
        nic.link_up();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    try {
      //start storage systems
       ArrayList<Storage> pools = Config.current.pools;
       for(Storage pool : pools) {
         pool.start();
       }
    } catch (Exception e) {
      JFLog.log(e);
    }
    try {
      //finally start virtual machines
      VirtualMachine[] vms = VirtualMachine.list();
      ArrayList<String> auto_start_vms = Config.current.auto_start_vms;
      for(String auto_start_vm : auto_start_vms) {
        for(VirtualMachine vm : vms) {
          if (vm.name.equals(auto_start_vm)) {
            if (vm.getState() == VirtualMachine.STATE_OFF) {
              vm.start();
              JF.sleep(Config.current.auto_start_delay * 1000);
            }
          }
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
