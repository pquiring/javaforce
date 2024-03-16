package javaforce.vm;

/** VMProvider
 *
 * Provides details from VM Management System of a Virtual Machine.
 *
 * @author pquiring
 */

public interface VMProvider {
  public int getVLAN(String name);
  public String getBridge(String name);
  public int getVNCPort();
}
