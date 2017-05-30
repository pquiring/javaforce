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

  public LocalTag(int cid, String name, int type, boolean unsigned, boolean array, SQL sql) {
    super(cid, name, type, unsigned, array, sql);
    if (array) {
      values = new HashMap<>();
      tid = Integer.valueOf(sql.select1value("select id from tags where cid=0 and name=" + SQL.quote(name)));
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
            sql.execute("inset into tagvalues (tid,idx,value) values (" + tid + "," + tv.idx + "," + SQL.quote(tv.value) + ")");
          } else {
            sql.execute("update tagvalues set value=" + SQL.quote(tv.value) + " where tid=" + tid + " and idx=" + tv.idx);
          }
        }
      } else {
        sql.execute("update tags set value=" + SQL.quote(value) + " where cid=0 and name=" + SQL.quote(name));
      }
      dirty = false;
      tagChanged();
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

  public String getValue(int idx) {
    synchronized(arrayLock) {
      TagValue tv = values.get(idx);
      if (tv == null) {
        tv = new TagValue();
        tv.idx = idx;
        readValue(tv);
        values.put(idx, tv);
      }
      return tv.value;
    }
  }

  public void setValue(String value, int idx) {
    synchronized(arrayLock) {
      TagValue tv = values.get(idx);
      if (tv == null) {
        tv = new TagValue();
        tv.idx = idx;
        readValue(tv);
        values.put(idx, tv);
      }
      tv.dirty = true;
      tv.value = value;
    }
  }
}
