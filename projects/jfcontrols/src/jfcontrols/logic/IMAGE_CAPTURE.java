package jfcontrols.logic;

/** Image Capture
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class IMAGE_CAPTURE extends LogicBlock {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ImageCapture";
  }

  public boolean execute(boolean enabled) {
    //TODO
    return enabled;
  }

  public int getTagsCount() {
    return 2;  //program# / shot#
  }

  public int getTagType(int idx) {
    return TagType.uint32;
  }
}
