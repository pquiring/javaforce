package jfnetboot;

/** File unix time.
 *
 * Seconds + nano seconds since epoch.
 *
 * @author pquiring
 */

public class UnixTime {
  public int secs, nsecs;
  public UnixTime(int secs, int nsecs) {
    this.secs = secs;
    this.nsecs = nsecs;
  }
}
