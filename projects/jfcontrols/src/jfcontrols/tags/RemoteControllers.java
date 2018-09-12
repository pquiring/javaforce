package jfcontrols.tags;

/** Remote Controllers (PLCs)
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

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

  public static javaforce.controls.Tag getTag(TagBase tag) {
    synchronized(lock) {
      RemoteController ctrl = map.get(tag.cid);
      if (ctrl == null) {
        ControllerRow cc = Database.getControllerById(tag.cid);
        JFLog.log("cid = " + tag.cid);
        ctrl = new RemoteController(tag.cid, cc.type, cc.ip, cc.speed);
        map.put(tag.cid, ctrl);
      }
      return ctrl.getTag(tag.name, tag.type);
    }
  }
}
