/**
 *
 * @author pquiring
 */

import java.util.*;

public class MetaList extends MetaTag {
  public MetaList() {
    type = 'l';
  }
  public ArrayList<MetaTag> list = new ArrayList<>();
}
