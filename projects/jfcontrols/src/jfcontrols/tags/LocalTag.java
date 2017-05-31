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

  public LocalTag(int cid, String name, int type, boolean unsigned, boolean array, boolean udt, SQL sql) {
    super(cid, type, unsigned, array, udt);
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
      value = sql.select1value("select value from tags where cid=0 and name=" + SQL.quote(name));
    }
  }

  public void updateRead(SQL sql) {
  }

  public void updateWrite(SQL sql) {
    if (dirty) {
      if (array) {
        TagValue tvs[] = values.values().toArray(new TagValue[0]);
        for(int a=0;a<tvs.length;a++) {
          TagValue tv = tvs[a];
          if (tv.insert) {
            sql.execute("insert into tagvalues (tid,idx,value) values (" + tid + "," + tv.nidx + "," + SQL.quote(tv.value) + ")");
            tv.insert = false;
          } else {
            sql.execute("update tagvalues set value=" + SQL.quote(tv.value) + " where tid=" + tid + " and idx=" + tv.nidx);
          }
          tagChanged(tv.nidx, value);
        }
      } else {
        sql.execute("update tags set value=" + SQL.quote(value) + " where cid=0 and id=" + tid);
        tagChanged(0, value);
      }
      dirty = false;
    }
  }

  private void readValue(TagValue tv) {
    SQL sql = SQLService.getSQL();
    String value = sql.select1value("select value from tagvalues where tid=" + tid + " and idx=" + tv.nidx);
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
        int mid = mids.get(ta.member);
        TagID id = new TagID(ta.idx, mid, ta.midx);
        TagValue tv = values.get(id);
        if (tv == null) {
          tv = new TagValue();
          tv.nidx = ta.idx;
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
        int mid = mids.get(ta.member);
        TagID id = new TagID(ta.idx, mid, ta.midx);
        TagValue tv = values.get(id);
        if (tv == null) {
          tv = new TagValue();
          tv.nidx = ta.idx;
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
