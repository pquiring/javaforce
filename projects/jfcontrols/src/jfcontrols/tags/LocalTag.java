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
      sql.execute("update tags set value=" + SQL.quote(value) + " where cid=0 and name=" + SQL.quote(name));
      JFLog.log("update:" + name + "=" + value);
      dirty = false;
      tagChanged();
    }
  }

  private String readValue(int idx) {
    SQL sql = SQLService.getSQL();
    String value = sql.select1value("select value from tagvalues where cid=0 and tid=" + tid + " and idx=" + idx);
    sql.close();
    return value;
  }

  public String getValue(int idx) {
    TagValue tv = values.get(idx);
    if (tv == null) {
      tv = new TagValue();
      tv.value = readValue(idx);
    }
    return tv.value;
  }

  public void setValue(String value, int idx) {
    TagValue tv = values.get(idx);
    if (tv == null) {
      tv = new TagValue();
      values.put(idx, tv);
    }
    tv.dirty = true;
    tv.value = value;
  }
}
