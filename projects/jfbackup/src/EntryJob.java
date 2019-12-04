/** EntryHost
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

public class EntryJob implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public ArrayList<EntryJobVolume> backup = new ArrayList<EntryJobVolume>();
  public String freq;  //"day" or "week"
  public int day;   //if freq=="week" : 1=Sunday 2=Monday etc.
  public int hour;  //HH (0-23)
  public int minute;  //MM (0-59)
}
