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

public class IOTag extends MonitoredTag {
  private Object arrayLock = new Object();
  private HashMap<TagID, TagValue> values;
  private HashMap<String, Integer> mids;
  private boolean loaded;
  private int dis[];
  private int dos[];

  public IOTag(int cid, String name, int type, boolean unsigned, boolean array, SQL sql) {
    super(cid, type, unsigned, array);
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
      if (tv.mid != 0) continue;  //di only
      tv.value = GPIO.read(dis[tv.mid]) ? "1" : "0";
      tagChanged(tv.idx, tv.value);
    }
  }

  public void updateWrite(SQL sql) {
    if (!loaded) return;
    if (dirty) {
      TagValue tvs[] = values.values().toArray(new TagValue[values.size()]);
      for(int a=0;a<tvs.length;a++) {
        TagValue tv = tvs[a];
        if (tv.mid != 1) continue;  //do only
        GPIO.write(dos[tv.mid], !tv.value.equals("0"));
        tagChanged(tv.idx, tv.value);
      }
      dirty = false;
    }
  }

  private void readValue(TagValue tv) {
    tv.value = GPIO.read(dis[tv.midx]) ? "1" : "0";
  }

  private void writeValue(TagValue tv) {
    GPIO.write(dos[tv.midx], !tv.value.equals("0"));
  }

  public String getValue(TagAddr ta) {
    if (!loaded) return "0";
    synchronized(arrayLock) {
      int mid = mids.get(ta.member);
      TagID id = new TagID(ta.idx, mid, ta.midx);
      TagValue tv = values.get(id);
      if (tv == null) {
        tv = new TagValue();
        tv.idx = ta.idx;
        readValue(tv);
        values.put(id, tv);
      }
      return tv.value;
    }
  }

  public void setValue(TagAddr ta, String value) {
    synchronized(arrayLock) {
      int mid = mids.get(ta.member);
      TagID id = new TagID(ta.idx, mid, ta.midx);
      TagValue tv = values.get(id);
      if (tv == null) {
        tv = new TagValue();
        tv.idx = ta.idx;
        writeValue(tv);
        values.put(id, tv);
      }
      tv.dirty = true;
      tv.value = value;
    }
    dirty = true;
  }
}
