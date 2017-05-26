package jfcontrols.logic;

/** Calls another function.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.controls.*;

public class CALL extends Logic {

/*
  private int fid;
  private int types[];
  public void setFunction(int fid, int types[]) {
    this.fid = fid;
    this.types = types;
  }
*/

  public boolean isBlock() {
    return true;
  }

  public String getName() {
    return "Call";
  }

  public String getCode() {return null;}

  public String getCode(String func) {
    return "  if (enabled) enabled = func_" + func + ".code(tags);";
  }

  public int getTagsCount() {
    //return types.length;
    return 1;
  }

  public int getTagType(int idx) {
    //return types[idx];
    return TagType.function;
  }
}
