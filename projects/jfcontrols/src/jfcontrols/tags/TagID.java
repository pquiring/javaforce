package jfcontrols.tags;

/** Tag ID
 *
 * @author pquiring
 */

public class TagID {
  public TagID(int tid, int idx, int mid, int midx) {
    this.tid = tid;
    this.idx = idx;
    this.mid = mid;
    this.midx = midx;
  }
  public int tid;
  public int idx;
  public int mid;
  public int midx;

  public boolean equals(Object o) {
    if (!(o instanceof TagID)) return false;
    TagID t = (TagID)o;
    if (t.tid != tid) return false;
    if (t.idx != idx) return false;
    if (t.mid != mid) return false;
    if (t.midx != midx) return false;
    return true;
  }

  public int hashCode() {
    return idx + mid + midx;
  }

  public String toString() {
    return "TagID{" + tid + "," + idx + "," + mid + "," + midx + "}";
  }
}
