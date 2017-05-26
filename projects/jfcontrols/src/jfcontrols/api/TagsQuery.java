package jfcontrols.api;

/** Tags Query
 *
 * @author pquiring
 */

import jfcontrols.tags.*;

public class TagsQuery {
  public TagsQuery(int cnt) {
    count = cnt;
    tags = new TagBase[cnt];
    values = new String[cnt];
    sizes = new int[cnt];
  }
  public int count;
  public TagBase tags[];
  public String values[];
  public int sizes[];
}
