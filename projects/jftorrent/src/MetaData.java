/**
 *
 * @author pquiring
 */

public class MetaData extends MetaTag {
  public MetaData() {
    type = 'x';
  }
  public byte[] data;
  public boolean equals(Object val) {
    if (val instanceof String) {
      String cmp = (String)val;
      return toString().equals(cmp);
    }
    return val == this;
  }
  public String toString() {
    return new String(data);
  }
}
