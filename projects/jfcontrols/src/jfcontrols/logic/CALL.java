package jfcontrols.logic;

/** Calls another function.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.controls.*;

import jfcontrols.tags.*;
import jfcontrols.functions.*;

public class CALL extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "Call";
  }

  public boolean doCall;

  public boolean execute(boolean enabled) {
    doCall = enabled;
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public String getTagName(int idx) {
    return "func";
  }

  public int getTagType(int idx) {
    return TagType.function;
  }

  public void moveNext(LogicPos pos) throws Exception {
    if (doCall) {
      if (pos.stackpos == pos.stackmax) {
        throw new Exception("Too many nested CALLs");
      }
      pos.push();
      LogicFunction func = FunctionService.getFunction(fid);
      pos.func = func;
      pos.rung = func.root;
      pos.block = func.root.root;
    } else {
      super.moveNext(pos);
    }
  }

  public int fid;
}
