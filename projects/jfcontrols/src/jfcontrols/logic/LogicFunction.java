package jfcontrols.logic;

/** LogicFunction
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class LogicFunction {
  public int id;
  public long revision;
  public LogicRung root;
  public boolean debug;
  public boolean[][] debug_en;
  public String[] debug_tv;
  public void execute(LogicPos pos) throws Exception {
    if (root == null) return;
    pos.rung = root;
    while (pos.rung != null) {
      pos.rung.execute(pos);
      pos.rung.moveNext(pos);
      if (pos.rung == null && pos.stackpos > 0) {
        pos.pop();
      }
    }
  }
}
