package jfcontrols.tags;

/** Tag IO
 *
 */

import javaforce.*;

public interface TagIO {
  public void updateRead(SQL sql);
  public void updateWrite(SQL sql);
}
