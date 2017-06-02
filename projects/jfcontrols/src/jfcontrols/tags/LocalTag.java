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

  public LocalTag(int cid, String name, int type, boolean unsigned, boolean array, SQL sql) {
    super(cid, type, unsigned, array);
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

  public String getValue(TagAddr ta) {
    if (array || udt) {
      synchronized(arrayLock) {
        int mid = 0;
        if (udt) {
          mid = mids.get(ta.member);
        }
        TagID id = new TagID(tid, ta.idx, mid, ta.midx);
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

  public void setValue(TagAddr ta, String value) {
    if (array || udt) {
      synchronized(arrayLock) {
        int mid = 0;
        if (udt) {
          mid = mids.get(ta.member);
        }
        TagID id = new TagID(tid, ta.idx, mid, ta.midx);
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
}
