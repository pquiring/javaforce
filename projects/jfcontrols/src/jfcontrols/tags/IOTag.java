package jfcontrols.tags;

/** Hardware I/O Tag
 *
 * Name : 'io'
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.pi.GPIO;

public class IOTag extends MonitoredTag implements TagBaseArray {
  private Object arrayLock = new Object();
  private HashMap<TagID, TagValue> values;
  private HashMap<String, Integer> mids;
  private boolean loaded;
  private int dis[];
  private int dos[];

  public IOTag(String name, int type, boolean unsigned, boolean array, SQL sql) {
    super(type, unsigned, array);
    mids = new HashMap<>();
    String data[][] = sql.select("select name,mid from udtmems where uid=" + type);
    for(int a=0;a<data.length;a++) {
      mids.put(data[a][0], Integer.valueOf(data[a][1]));
    }
    values = new HashMap<>();
    try {
      loaded = GPIO.init();
    } catch (Error e) {}
    if (!loaded) {
      //TODO : set an alarm bit
      JFLog.log("Error:Failed to init GPIO");
      return;
    }
    String dimap[] = sql.select1value("select hw_di").split(",");
    dis = new int[dimap.length];
    TagAddr ta = new TagAddr();
    ta.member = "di";
    for(int a=0;a<dis.length;a++) {
      dis[a] = Integer.valueOf(dimap[a]);
      GPIO.configInput(dis[a]);
      ta.midx = a;
      getValue(ta);
    }
    String domap[] = sql.select1value("select hw_do").split(",");
    dos = new int[domap.length];
    ta.member = "do";
    for(int a=8;a<16;a++) {
      dos[a] = Integer.valueOf(domap[a]);
      GPIO.configOutput(dos[a]);
      ta.midx = a;
      setValue(ta, "0");
    }
  }

  public void updateRead(SQL sql) {
    if (!loaded) return;
    TagValue tvs[] = values.values().toArray(new TagValue[values.size()]);
    for(int a=0;a<tvs.length;a++) {
      TagValue tv = tvs[a];
      if (tv.id.mid != 0) continue;  //di only
      tv.value = GPIO.read(dis[tv.id.mid]) ? "1" : "0";
      tagChanged(tv.id, tv.value, tv.oldValue);
    }
  }

  public void updateWrite(SQL sql) {
    if (!loaded) return;
    if (dirty) {
      TagValue tvs[] = values.values().toArray(new TagValue[values.size()]);
      for(int a=0;a<tvs.length;a++) {
        TagValue tv = tvs[a];
        if (tv.id.mid != 1) continue;  //do only
        GPIO.write(dos[tv.id.mid], !tv.value.equals("0"));
        tagChanged(tv.id, tv.value, tv.oldValue);
      }
      dirty = false;
    }
  }

  private void readValue(TagValue tv) {
    tv.value = GPIO.read(dis[tv.id.midx]) ? "1" : "0";
  }

  private void writeValue(TagValue tv) {
    GPIO.write(dos[tv.id.midx], !tv.value.equals("0"));
  }

  public String getValue(TagAddr ta) {
    if (!loaded) return "0";
    synchronized(arrayLock) {
      int mid = mids.get(ta.member);
      TagID id = new TagID(0, ta.idx, mid, ta.midx);
      TagValue tv = values.get(id);
      if (tv == null) {
        tv = new TagValue(id);
        readValue(tv);
        values.put(id, tv);
      }
      return tv.value;
    }
  }

  public void setValue(TagAddr ta, String value) {
    synchronized(arrayLock) {
      int mid = mids.get(ta.member);
      TagID id = new TagID(0, ta.idx, mid, ta.midx);
      TagValue tv = values.get(id);
      if (tv == null) {
        tv = new TagValue(id);
        writeValue(tv);
        values.put(id, tv);
      }
      tv.dirty = true;
      tv.value = value;
    }
    dirty = true;
  }

  public TagBase getIndex(TagAddr ta) {
    return new TagArray(this, this, ta);
  }

  public String getValue() {
    return null;
  }

  public void setValue(String value) {
  }
}
