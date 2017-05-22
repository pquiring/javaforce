package jfcontrols.logic;

/** Calls another function.
 *
 * @author pquiring
 */

import javaforce.*;

import jfcontrols.tags.Types;

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

  public String getCode() {
    return "  jfcontrols.functions.Invoke.invoke(tags[0]);";
  }

  public int getTagsCount() {
    //return types.length;
    return 1;
  }

  public int getTagType(int idx) {
    //return types[idx];
    return Types.FUNCTION;
  }
}
