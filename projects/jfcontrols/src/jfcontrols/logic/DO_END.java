package jfcontrols.logic;

/** Do End
 *
 * } while
 *
 * Ends a looping block of code and jumps to last Do if enabled.
 *
 * See Do for starting block of code.
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class DO_END extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "} while()";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "} while(enabled);";
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return -1;
  }

  public boolean isLast() {
    return true;
  }

  public boolean isFlowControl() {
    return true;
  }

  public boolean canClose(String name) {
    return name.equals("DO");
  }
}
