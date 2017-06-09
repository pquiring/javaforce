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
    for(int a=0;a<dis.length;a++) {
      dis[a] = Integer.valueOf(dimap[a]);
      GPIO.configInput(dis[a]);
      getValue(0, IDs.io_mid_di, a);
    }
    String domap[] = sql.select1value("select hw_do").split(",");
    dos = new int[domap.length];
    for(int a=8;a<16;a++) {
      dos[a] = Integer.valueOf(domap[a]);
      GPIO.configOutput(dos[a]);
      setValue("0", 0, IDs.io_mid_do, a);
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

  public String getValue(int idx, int mid, int midx) {
    if (!loaded) return "0";
    synchronized(arrayLock) {
      TagID id = new TagID(0, idx, mid, midx);
      TagValue tv = values.get(id);
      if (tv == null) {
        tv = new TagValue(id);
        readValue(tv);
        values.put(id, tv);
      }
      return tv.value;
    }
  }

  public void setValue(String value, int idx, int mid, int midx) {
    synchronized(arrayLock) {
      TagID id = new TagID(0, idx, mid, midx);
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

  public TagBase getIndex(int idx) {
    JFLog.log("Error:IOTag.getIndex(int) is invalid");
    return null;
  }

  public class Member extends TagBase {
    private int mid;
    public Member(IOTag _this, int mid) {
      super(_this.getType(), _this.isUnsigned(), _this.isArray());
      this.mid = mid;
    }

    public String getValue() {
      return IOTag.this.getValue(0, mid, 0);
    }

    public void setValue(String value) {
      IOTag.this.setValue(value, 0, mid, 0);
    }

    public TagBase getIndex(int midx) {
      return new MemberIndex(IOTag.this, mid, midx);
    }

    public TagBase getMember(int mid) {
      return null;
    }

    public int getMember(String member) {
      return -1;
    }

    public int getTagID() {
      return IOTag.this.getTagID();
    }

    public int getIndex() {
      return 0;
    }

    public boolean isMember() {
      return true;
    }

    public int getMember() {
      return mid;
    }

    public int getMemberIndex() {
      return 0;
    }
  }

  public class MemberIndex extends TagBase {
    private int mid, midx;
    public MemberIndex(IOTag _this, int mid, int midx) {
      super(_this.getType(), _this.isUnsigned(), _this.isArray());
      this.mid = mid;
      this.midx = midx;
    }

    public String getValue() {
      return IOTag.this.getValue(0, mid, midx);
    }

    public void setValue(String value) {
      IOTag.this.setValue(value, 0, mid, midx);
    }

    public TagBase getIndex(int midx) {
      return null;
    }

    public TagBase getMember(int mid) {
      return null;
    }

    public int getMember(String member) {
      return -1;
    }

    public int getTagID() {
      return IOTag.this.getTagID();
    }

    public int getIndex() {
      return 0;
    }

    public boolean isMember() {
      return true;
    }

    public int getMember() {
      return mid;
    }

    public int getMemberIndex() {
      return 0;
    }
  }

  public TagBase getMember(int mid) {
    return new Member(this, mid);
  }

  public String getValue() {
    return null;
  }

  public void setValue(String value) {
  }

  public int getMember(String name) {
    return mids.get(name);
  }

  public int getTagID() {
    return -1;  //???
  }

  public int getIndex() {
    return 0;
  }

  public boolean isMember() {
    return false;
  }

  public int getMember() {
    return 0;
  }

  public int getMemberIndex() {
    return 0;
  }
}
