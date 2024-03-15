package javaforce.vm;

/** Task status callback.
 *
 * @author pquiring
 */

public interface Status {
  public int status(String taskid, String msg);
}
