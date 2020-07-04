package jfcontrols.logic;

/** Image NOK
 *
 * @author pquiring
 */

import javaforce.controls.*;
import jfcontrols.tags.*;

public class IMAGE_NOK extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ImageNOK";
  }

  public boolean execute(boolean enabled) {
    //TODO
    return enabled;
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return TagType.uint32;
  }

}
