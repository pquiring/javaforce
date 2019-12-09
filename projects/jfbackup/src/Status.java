/** Status
 *
 * Live job status.
 *
 * @author pquiring
 */

public class Status {
  public static boolean active;  //service is active
  public static boolean running;  //job is running
  public static boolean abort;  //job is aborting
  public static String desc;
  public static StringBuilder log;
  public static Thread job;
  public static long copied;
  public static int files;
}
