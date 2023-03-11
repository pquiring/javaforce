/**
 *
 * @author pquiring
 */

public class MetaString extends MetaTag {
  public MetaString() {
    type = 's';
  }
  public String str;
  public boolean equals(Object val) {
    if (val instanceof String) {
      String cmp = (String)val;
      return str.equals(cmp);
    }
    return val == this;
  }
  public String toString() {
    return str;
  }
}
