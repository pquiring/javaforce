package jfcontrols.tags;

/** Tag Address
 *
 * @author pquiring
 */

public class TagAddr {
  public String name;
  public int nidx = -1;
  public String member;
  public int midx = -1;
  public static TagAddr decode(String addr) {
    TagAddr ta = new TagAddr();
    int idx = addr.indexOf('.');
    if (idx != -1) {
      ta.name = addr.substring(0, idx);
      ta.member = addr.substring(idx + 1);
    } else {
      ta.name = addr;
    }
    idx = ta.name.indexOf('[');
    if (idx != -1) {
      ta.nidx = Integer.valueOf(ta.name.substring(idx+1, ta.name.length() - 1));
      ta.name = ta.name.substring(0, idx);
    }
    idx = ta.member.indexOf('[');
    if (idx != -1) {
      ta.midx = Integer.valueOf(ta.member.substring(idx+1, ta.member.length() - 1));
      ta.member = ta.member.substring(0, idx);
    }
    return ta;
  }
}
