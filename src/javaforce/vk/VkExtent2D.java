package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkExtent2D.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkExtent2D.html
 *
 * @author pquiring
 */

public class VkExtent2D extends FFMStruct {
  /** */
  public int width;
  /** */
  public int height;

  public VkExtent2D() {}
  public VkExtent2D(int width, int height) {
    this.width = width;
    this.height = height;
  }
}
