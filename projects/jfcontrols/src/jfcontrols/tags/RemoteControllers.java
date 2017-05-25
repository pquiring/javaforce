package jfcontrols.tags;

/** Remote Controllers (PLCs)
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

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

  public static javaforce.controls.Tag getTag(int cid, String name, int type, SQL sql) {
    synchronized(lock) {
      RemoteController ctrl = map.get(cid);
      if (ctrl == null) {
        String info[] = sql.select1row("select type,ip,speed from ctrls where cid=" + cid);
        JFLog.log("cid = " + cid);
        ctrl = new RemoteController(cid, Integer.valueOf(info[0]), info[1], Integer.valueOf(info[2]));
        map.put(cid, ctrl);
      }
      return ctrl.getTag(name, type);
    }
  }
}
