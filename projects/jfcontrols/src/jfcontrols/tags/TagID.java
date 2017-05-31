package jfcontrols.tags;

/** Tag ID
 *
 * @author pquiring
 */

public class TagID {
  public TagID(int idx, int mid, int midx) {
    this.idx = idx;
    this.mid = mid;
    this.midx = midx;
  }
  public int idx;
  public int mid;
  public int midx;

  public boolean equals(Object o) {
    if (!(o instanceof TagID)) return false;
    TagID t = (TagID)o;
    if (t.idx != idx) return false;
    if (t.mid != mid) return false;
    if (t.midx != midx) return false;
    return true;
  }

  public int hashCode() {
    return idx + mid + midx;
  }
}
