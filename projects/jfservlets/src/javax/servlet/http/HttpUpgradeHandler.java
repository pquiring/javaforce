package javax.servlet.http;

/** HttpUpgradeHandler
 *
 * @author peter.quiring
 */

public interface HttpUpgradeHandler {
  public void destroy();
  public void init(WebConnection wc);
}
