package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkExtent3D.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkExtent3D.html
 *
 * @author pquiring
 */

public class VkExtent3D extends FFMStruct {
  /** */
  public int width;
  /** */
  public int height;
  /** */
  public int depth;

  public VkExtent3D() {}
  public VkExtent3D(int width, int height, int depth) {
    this.width = width;
    this.height = height;
    this.depth = depth;
  }
}
