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
  private Tag first;
  private HashMap<String, TagBase> map = new HashMap<>();
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
      Tag[] tags = map.values().toArray(new Tag[0]);
      for(int a=0;a<tags.length;a++) {
        tags[a].stop();
      }
      map.clear();
      first = null;
    }
  }

  public TagBase getTag(String name) {
    synchronized(lock) {
      TagBase tag = map.get(name);
      if (tag == null) {
        JFLog.log("Unknown Tag:" + name);
      }
      return tag;
    }
  }

  public void addTag(TagBase tagBase) {
    Tag tag = new Tag();
    tagBase.remoteTag = tag;
    tag.delay = delay;
    tag.host = ip;
    switch (tagBase.type) {
      case TagType.bit: tag.size = TagType.bit; break;
      case TagType.int8: tag.size = TagType.int8; break;
      case TagType.int16: tag.size = TagType.int16; break;
      case TagType.int32: tag.size = TagType.int32; break;
      case TagType.int64: tag.size = TagType.int64; break;
      case TagType.float32: tag.size = TagType.float32; break;
      case TagType.float64: tag.size = TagType.float64; break;
      default: JFLog.log("Error:TagType unknown:" + tagBase.type); return;
    }
    tag.tag = tagBase.name;
    switch (controllerType) {
      case 0: tag.type = ControllerType.JF; break;
      case 1: tag.type = ControllerType.S7; break;
      case 2: tag.type = ControllerType.AB; break;
      case 3: tag.type = ControllerType.MB; break;
      case 4: tag.type = ControllerType.NI; break;
      default: JFLog.log("Error:Controller type unknown:" + controllerType);
    }
    map.put(tagBase.name, tagBase);
    if (ip.length() == 0) return;
    tag.start(first);
    if (first == null) first = tag;
  }

  public void deleteTag(TagBase tag) {
    map.put(tag.name, null);
    //TODO : delete tag completely
  }
}
