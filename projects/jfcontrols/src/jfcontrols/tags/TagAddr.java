package jfcontrols.tags;

/** Tag Address
 *
 * @author pquiring
 */

public class TagAddr {
  public int cid = 0;
  public String name;
  public int idx = -1;
  public String member;
  public int midx = -1;

  public TagAddr() {}
  public TagAddr(TagAddr copy) {
    cid = copy.cid;
    name = copy.name;
    idx = copy.idx;
    member = copy.member;
    midx = copy.midx;
  }

  public String toString() {
    if (member != null) {
      return "TagAddr:" + name + "[" + idx + "]." + member;
    } else {
      return "TagAddr:" + name + "[" + idx + "]";
    }
  }

  public boolean isArray() {
    return idx != -1 || midx != -1;
  }
}
