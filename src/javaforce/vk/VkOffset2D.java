package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkOffset2D.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkOffset2D.html
 *
 * @author pquiring
 */

public class VkOffset2D extends FFMStruct {
  /** */
  public int x;
  /** */
  public int y;

  public VkOffset2D() {}
  public VkOffset2D(int x, int y) {
    this.x = x;
    this.y = y;
  }
}
