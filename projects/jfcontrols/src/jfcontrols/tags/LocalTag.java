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
            sql.execute("insert into tagvalues (tid,idx,mid,midx,value) values (" + tid + "," + tv.idx + "," + tv.mid + "," + tv.midx + "," + SQL.quote(tv.value) + ")");
            tv.insert = false;
          } else {
            sql.execute("update tagvalues set value=" + SQL.quote(tv.value) + " where idx=" + tv.idx + " and mid=" + tv.mid + " and midx=" + tv.midx + " and tid=" + tid);
          }
          tagChanged(tv.idx, tv.value);
        }
      } else {
        sql.execute("update tagvalues set value=" + SQL.quote(value) + " where idx=0 and mid=0 and midx=0 and tid=" + tid);
        tagChanged(0, value);
      }
      dirty = false;
    }
  }

  private void readValue(TagValue tv) {
    SQL sql = SQLService.getSQL();
    String value = sql.select1value("select value from tagvalues where tid=" + tid + " and idx=" + tv.idx + " and mid=" + tv.mid + " and midx=" + tv.midx);
    sql.close();
    if (value == null) {
      tv.insert = true;
      value = "0";
    }
    tv.value = value;
  }

  public String getValue(TagAddr ta) {
    if (array || udt) {
      synchronized(arrayLock) {
        int mid = 0;
        if (udt) {
          mid = mids.get(ta.member);
        }
        TagID id = new TagID(ta.idx, mid, ta.midx);
        TagValue tv = values.get(id);
        if (tv == null) {
          tv = new TagValue();
          tv.idx = ta.idx;
          tv.mid = mid;
          tv.midx = ta.midx;
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
        TagID id = new TagID(ta.idx, mid, ta.midx);
        TagValue tv = values.get(id);
        if (tv == null) {
          tv = new TagValue();
          tv.idx = ta.idx;
          tv.mid = mid;
          tv.midx = ta.midx;
          readValue(tv);
          values.put(id, tv);
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
