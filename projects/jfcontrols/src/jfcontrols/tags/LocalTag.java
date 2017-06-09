package jfcontrols.tags;

/** Local Tag
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import jfcontrols.sql.SQLService;

public class LocalTag extends MonitoredTag {
  private int tid;
  private Object arrayLock = new Object();
  private String value;
  private String oldValue;
  private HashMap<TagID, TagValue> values;
  private HashMap<String, Integer> mids;

  public LocalTag(String name, int type, boolean unsigned, boolean array, SQL sql) {
    super(type, unsigned, array);
    tid = Integer.valueOf(sql.select1value("select id from tags where cid=0 and name=" + SQL.quote(name)));
    if (udt) {
      mids = new HashMap<>();
      String data[][] = sql.select("select name,mid from udtmems where uid=" + type);
      for(int a=0;a<data.length;a++) {
        mids.put(data[a][0], Integer.valueOf(data[a][1]));
      }
    }
    if (array || udt) {
      values = new HashMap<>();
    } else {
      value = sql.select1value("select value from tagvalues where idx=0 and mid=0 and midx=0 and tid=" + tid);
      if (value == null) value = "0";
      oldValue = value;
    }
  }

  public void updateRead(SQL sql) {
  }

  public void updateWrite(SQL sql) {
    if (dirty) {
      if (array || udt) {
        TagValue tvs[] = values.values().toArray(new TagValue[0]);
        for(int a=0;a<tvs.length;a++) {
          TagValue tv = tvs[a];
          if (tv.insert) {
            sql.execute("insert into tagvalues (tid,idx,mid,midx,value) values (" + tid + "," + tv.id.idx + "," + tv.id.mid + "," + tv.id.midx + "," + SQL.quote(tv.value) + ")");
            tv.insert = false;
          } else {
            sql.execute("update tagvalues set value=" + SQL.quote(tv.value) + " where idx=" + tv.id.idx + " and mid=" + tv.id.mid + " and midx=" + tv.id.midx + " and tid=" + tid);
          }
          tagChanged(tv.id, tv.value, tv.oldValue);
        }
      } else {
        sql.execute("update tagvalues set value=" + SQL.quote(value) + " where idx=0 and mid=0 and midx=0 and tid=" + tid);
        tagChanged(null, oldValue, value);
        oldValue = value;
      }
      dirty = false;
    }
  }

  private void readValue(TagValue tv) {
    SQL sql = SQLService.getSQL();
    String value = sql.select1value("select value from tagvalues where tid=" + tid + " and idx=" + tv.id.idx + " and mid=" + tv.id.mid + " and midx=" + tv.id.midx);
    sql.close();
    if (value == null) {
      tv.insert = true;
      value = "0";
    }
    tv.value = value;
    tv.oldValue = value;
  }

  public String getValue() {
    if (array || udt) {
      JFLog.log("Error:LocalTag array:must call getIndex() or getMember()");
      return null;
    }
    return getValue(0, 0, 0);
  }

  public String getValue(int idx, int mid, int midx) {
    if (array || udt) {
      synchronized(arrayLock) {
        TagID id;
        if (udt) {
          id = new TagID(tid, idx, mid, midx);
        } else {
          id = new TagID(tid, idx, 0, 0);
        }
        TagValue tv = values.get(id);
        if (tv == null) {
          tv = new TagValue(id);
          readValue(tv);
          values.put(id, tv);
        }
        return tv.value;
      }
    } else {
      return value;
    }
  }

  public void setValue(String value) {
    if (array || udt) {
      JFLog.log("Error:LocalTag array:must call getIndex()");
      return;
    }
    this.value = value;
    dirty = true;
  }

  public void setValue(String value, int idx, int mid, int midx) {
    if (array || udt) {
      synchronized(arrayLock) {
        TagID id;
        if (udt) {
          id = new TagID(tid, idx, mid, midx);
        } else {
          id = new TagID(tid, idx, 0, 0);
        }
        TagValue tv = values.get(id);
        if (tv == null) {
          tv = new TagValue(id);
          readValue(tv);
          values.put(id, tv);
        }
        if (!tv.dirty) {
          tv.oldValue = tv.value;
        }
        tv.dirty = true;
        tv.value = value;
      }
    } else {
      this.value = value;
    }
    dirty = true;
  }

  public int getTagID() {
    return tid;
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

  public class Index extends TagBase {
    private int idx;

    public Index(TagBase _this, int idx) {
      super(_this.getType(), _this.isUnsigned(), false);
      this.idx = idx;
    }

    public String getValue() {
      return LocalTag.this.getValue(idx, 0, 0);
    }

    public void setValue(String value) {
      LocalTag.this.setValue(value, idx, 0, 0);
    }

    public TagBase getIndex(int idx) {
      return LocalTag.this.getIndex(idx);
    }

    public TagBase getMember(int mid) {
      return LocalTag.this.getMember(idx);
    }

    public int getMember(String member) {
      return LocalTag.this.getMember(member);
    }

    public int getTagID() {
      return tid;
    }

    public int getIndex() {
      return idx;
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

  public TagBase getIndex(int idx) {
    return new Index(this, idx);
  }

  public class MemberIndex extends TagBase {
    private int idx, mid, midx;

    public MemberIndex(TagBase _this, int idx, int mid, int midx) {
      super(_this.getType(), _this.isUnsigned(), _this.isArray());
      this.mid = mid;
    }

    public String getValue() {
      return LocalTag.this.getValue(idx, mid, midx);
    }

    public void setValue(String value) {
      LocalTag.this.setValue(value, idx, mid, midx);
    }

    public TagBase getIndex(int idx) {
      return null;
    }

    public TagBase getMember(int mid) {
      return null;
    }

    public int getMember(String member) {
      return LocalTag.this.getMember(member);
    }

    public int getTagID() {
      return tid;
    }

    public int getIndex() {
      return idx;
    }

    public boolean isMember() {
      return true;
    }

    public int getMember() {
      return mid;
    }

    public int getMemberIndex() {
      return midx;
    }
  }

  public class Member extends TagBase {
    private int idx, mid;

    public Member(TagBase _this, int idx, int mid) {
      super(_this.getType(), _this.isUnsigned(), _this.isArray());
      this.idx = idx;
      this.mid = mid;
    }

    public String getValue() {
      return LocalTag.this.getValue(idx, mid, 0);
    }

    public void setValue(String value) {
      LocalTag.this.setValue(value, idx, mid, 0);
    }

    public TagBase getIndex(int midx) {
      return new MemberIndex(this, idx, mid, midx);
    }

    public TagBase getMember(int mid) {
      return LocalTag.this.getMember(mid);
    }

    public int getMember(String member) {
      return LocalTag.this.getMember(member);
    }

    public int getTagID() {
      return tid;
    }

    public int getIndex() {
      return idx;
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
    return new Member(this, -1, mid);
  }

  public int getMember(String name) {
    return mids.get(name);
  }
}
