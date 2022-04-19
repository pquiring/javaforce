/** Storage
 *
 * @author pquiring
 */

public class Storage {
  public Storage(String name) {
    this.name = name;
  }
  public String name;  //C:, /mount, etc.
  public long size;
  public long free;
}
