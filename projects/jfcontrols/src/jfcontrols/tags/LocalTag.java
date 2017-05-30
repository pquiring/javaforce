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
  private HashMap<Integer, TagValue> values;

  public LocalTag(int cid, String name, int type, boolean unsigned, boolean array, SQL sql) {
    super(cid, type, unsigned, array);
    tid = Integer.valueOf(sql.select1value("select id from tags where cid=0 and name=" + SQL.quote(name)));
    if (array) {
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
            sql.execute("insert into tagvalues (tid,idx,value) values (" + tid + "," + tv.idx + "," + SQL.quote(tv.value) + ")");
            tv.insert = false;
          } else {
            sql.execute("update tagvalues set value=" + SQL.quote(tv.value) + " where tid=" + tid + " and idx=" + tv.idx);
          }
          tagChanged(tv.idx, value);
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
    String value = sql.select1value("select value from tagvalues where tid=" + tid + " and idx=" + tv.idx);
    sql.close();
    if (value == null) {
      tv.insert = true;
      value = "0";
    }
    tv.value = value;
  }

  public String getValue(TagAddr ta) {
    if (array) {
      synchronized(arrayLock) {
        TagValue tv = values.get(ta.nidx);
        if (tv == null) {
          tv = new TagValue();
          tv.idx = ta.nidx;
          readValue(tv);
          values.put(ta.nidx, tv);
        }
        return tv.value;
      }
    } else {
      return value;
    }
  }

  public void setValue(TagAddr addr, String value) {
    if (array) {
      synchronized(arrayLock) {
        TagValue tv = values.get(addr.nidx);
        if (tv == null) {
          tv = new TagValue();
          tv.idx = addr.nidx;
          readValue(tv);
          values.put(addr.nidx, tv);
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
