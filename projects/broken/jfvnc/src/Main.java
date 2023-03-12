
/** Convert main args.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.awt.*;

public class Main {

  public static void main(String args[]) {
    //convert arg "host[:port]" into "HOST" host "PORT" port
    String connString;
    if (args.length != 1) {
      connString = JFAWT.getString("Enter VNC Server host[:port]", null);
      if (connString == null) {
        return;
      }
    } else {
      connString = args[0];
    }
    int idx = connString.indexOf(':');
    String host, port;
    if (idx == -1) {
      host = connString;
      port = "5900";
    } else {
      host = connString.substring(0, idx);
      port = connString.substring(idx + 1);
    }
    VncViewer.main(new String[]{"HOST", host, "PORT", port});
  }
}
