package jfcontrols.tags;

/** Remote Controller
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.controls.*;

public class RemoteController {
  private int cid;
  private Object lock = new Object();
  private javaforce.controls.Tag first;
  private HashMap<Tag, javaforce.controls.Tag> map = new HashMap<>();
  private int delay;
  private String ip;
  private int controllerType;

  public RemoteController(int cid, int type, String ip, int speed) {
    this.cid = cid;
    this.controllerType = type;
    this.ip = ip;
    switch (speed) {
      case 1: delay = 1000; break;
      case 0:  //auto -> 100ms
      case 2: delay = 100; break;
      case 3: delay = 10; break;
    }
  }

  public void cancel() {
    synchronized(lock) {
      javaforce.controls.Tag tags[] = map.values().toArray(new javaforce.controls.Tag[0]);
      for(int a=0;a<tags.length;a++) {
        tags[a].stop();
      }
      map.clear();
      first = null;
    }
  }

  public javaforce.controls.Tag getTag(String name, int tagType) {
    synchronized(lock) {
      javaforce.controls.Tag tag = map.get(name);
      if (tag == null) {
        tag = new javaforce.controls.Tag();
        tag.delay = delay;
        tag.host = ip;
        switch (tagType) {
          case TagType.BIT: tag.size = Controller.sizes.bit; break;
          case TagType.INT8: tag.size = Controller.sizes.int8; break;
          case TagType.INT16: tag.size = Controller.sizes.int16; break;
          case TagType.INT32: tag.size = Controller.sizes.int32; break;
//          case TagType.LONG: tag.size = Controller.sizes.int64; break;
          case TagType.FLOAT32: tag.size = Controller.sizes.float32; break;
          case TagType.FLOAT64: tag.size = Controller.sizes.float64; break;
          default: JFLog.log("Error:TagType unknown:" + tagType); return null;
        }
        tag.tag = name;
        JFLog.log("type=" + controllerType);
        switch (controllerType) {
          case 0: tag.type = Controller.types.JF; break;
          case 1: tag.type = Controller.types.S7; break;
          case 2: tag.type = Controller.types.AB; break;
          case 3: tag.type = Controller.types.MB; break;
          case 4: tag.type = Controller.types.NI; break;
        }
        tag.start(first);
        if (first == null) first = tag;
      }
      return tag;
    }
  }
}
