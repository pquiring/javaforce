package jfcontrols.logic;

/** Tee T
 *
 * --T----T--
 *   A----B
 *   C----D
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class Tee extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "";
  }

  public boolean execute(boolean enabled) {
    switch (type) {
      case 't':
        this.enabled = enabled;
        break;
      case 'a':
        return upper.enabled;
      case 'b':
        this.enabled = enabled;
        break;
      case 'c':
        return upper.enabled;
      case 'd':
        this.enabled = enabled;
        break;
    }
    return enabled;
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return TagType.any;
  }

  public Tee upper, lower;
  public char type;  //t,a,b,c,d
  public boolean enabled;
}
