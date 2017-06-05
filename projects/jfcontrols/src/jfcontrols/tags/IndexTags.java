package jfcontrols.tags;

/** Index Tags @nnn
 *
 * @author pquiring
 */

import java.util.*;

public class IndexTags {
  public HashMap<Integer, Integer> map = new HashMap<>();
  public int getIndex(int idx) {
    Integer value = map.get(idx);
    if (value == null) return 0;
    return Integer.valueOf(value);
  }
  public void setIndex(int idx, int value) {
    map.put(idx, value);
  }
  public TagBase getTag(int idx) {
    return new IndexTag(this, idx);
  }
}
