/** EntryTape
 *
 * @author pquiring
 */

import java.io.*;

public class EntryTape implements Serializable, Comparable<EntryTape> {
  private static final long serialVersionUID = 1L;

  public EntryTape(String barcode, long backup, long retention, String job, int number) {
    this.barcode = barcode;
    this.backup = backup;
    this.retention = retention;
    this.job = job;
    this.number = number;
  }
  public String barcode;
  public long backup;
  public long retention;
  public String job;
  public int number;

  public long capacity;  //in 64k blocks
  public long left;  //in 64k blocks

  public transient long position;
  public static boolean sorted;

  /** Sort tapes by retention date. */
  public int compareTo(EntryTape o) {
    EntryTape ot = (EntryTape)o;
    if (retention > ot.retention) {sorted = true; return -1;}
    if (retention < ot.retention) {return 1;}
    return 0;
  }
}
