/** Convert main args.
 *
 * @author pquiring
 */
public class Main {
  public static void main(String args[]) {
    //convert arg "host[:port]" into "HOST" host "PORT" port
    if (args.length != 1) {
      System.out.println("Usage:jfvnc HOST[:port]");
    }
    int idx = args[0].indexOf(':');
    String host, port;
    if (idx == -1) {
      host = args[0];
      port = "5900";
    } else {
      host = args[0].substring(0, idx);
      port = args[0].substring(idx+1);
    }
    VncViewer.main(new String[] {"HOST" , host, "PORT", port});
  }
}
