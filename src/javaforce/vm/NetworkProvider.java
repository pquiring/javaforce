package javaforce.vm;

/** NetworkProvider
 *
 * @author pquiring
 */

public interface NetworkProvider {
  public int getVLAN(String name);
  public String getBridge(String name);
}
