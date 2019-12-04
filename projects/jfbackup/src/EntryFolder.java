/** EntryFolder
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

public class EntryFolder implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public ArrayList<EntryFolder> folders = new ArrayList<EntryFolder>();
  public ArrayList<EntryFile> files = new ArrayList<EntryFile>();

  public transient boolean isVolume;
}
