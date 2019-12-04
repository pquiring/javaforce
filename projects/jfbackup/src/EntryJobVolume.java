/** EntryHost
 *
 * @author pquiring
 */

import java.io.*;

public class EntryJobVolume implements Serializable {
  private static final long serialVersionUID = 1L;

  public String host;  //null = localhost
  public String volume;  //C, D, etc.
  public String path;  //base path (default = '\\')
}
