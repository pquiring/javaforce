package jfcontrols.tags;

/** Local Tag
 *
 * @author pquiring
 */

import javaforce.*;

public class LocalTag extends Tag {

  public LocalTag(String name, int type, SQL sql) {
    super(name, type, sql);
    value = sql.select1value("select value from tags where cid=0 and name=" + SQL.quote(name));
  }

  public void updateRead(SQL sql) {
  }

  public void updateWrite(SQL sql) {
    if (dirty) {
      sql.execute("update tags set value=" + SQL.quote(value) + " where cid=0 and name=" + SQL.quote(name));
      dirty = false;
    }
  }
}
