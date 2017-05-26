package jfcontrols.api;

/** Tags Query
 *
 * @author pquiring
 */

import jfcontrols.tags.*;

public class TagsQuery {
  public TagsQuery(int cnt) {
    count = cnt;
    tags = new Tag[cnt];
    values = new String[cnt];
    sizes = new int[cnt];
  }
  public int count;
  public Tag tags[];
  public String values[];
  public int sizes[];
}
