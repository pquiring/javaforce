package jfcontrols.tags;

/** Remote Controllers (PLCs)
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.controls.*;

import jfcontrols.db.*;

public class RemoteControllers {
  private static HashMap<Integer, RemoteController> map = new HashMap<>();
  private static Object lock = new Object();

  public void reset() {
    RemoteController ctrls[] = map.values().toArray(new RemoteController[0]);
    for(int a=0;a<ctrls.length;a++) {
      ctrls[a].cancel();
    }
    map.clear();
  }

  private static RemoteController getController(int cid) {
    RemoteController ctrl = map.get(cid);
    if (ctrl == null) {
      ControllerRow cc = Database.getControllerById(cid);
      ctrl = new RemoteController(cid, cc.type, cc.ip, cc.speed);
      map.put(cid, ctrl);
    }
    return ctrl;
  }

  public static TagBase getTag(int cid, String name) {
    synchronized(lock) {
      RemoteController ctrl = getController(cid);
      return ctrl.getTag(name);
    }
  }

  public static void addTag(TagBase tag) {
    synchronized(lock) {
      RemoteController ctrl = getController(tag.cid);
      ctrl.addTag(tag);
    }
  }

  public static void deleteTag(TagBase tag) {
    synchronized(lock) {
      RemoteController ctrl = getController(tag.cid);
      ctrl.deleteTag(tag);
    }
  }
}
