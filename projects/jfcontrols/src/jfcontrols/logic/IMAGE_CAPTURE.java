package jfcontrols.logic;

import javaforce.controls.*;

/** Image Capture
 *
 * @author pquiring
 */

public class IMAGE_CAPTURE extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ImageCapture";
  }

  public String getCode(int[] tagTypes, boolean[] array, boolean[] unsigned) {
    //TODO
    return null;
  }

  public int getTagsCount() {
    return 2;  //program# / shot#
  }

  public int getTagType(int idx) {
    return TagType.uint32;
  }
}
