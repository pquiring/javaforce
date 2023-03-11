/**
 *
 * @author pquiring
 */

import java.util.*;

public class MetaDict extends MetaTag {
  public MetaDict() {
    type = 'd';
  }
  public ArrayList<MetaDictEntry> list = new ArrayList<>();
}
