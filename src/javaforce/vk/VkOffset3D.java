package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkOffset3D.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkOffset3D.html
 *
 * @author pquiring
 */

public class VkOffset3D extends FFMStruct {
  /** */
  public int x;
  /** */
  public int y;
  /** */
  public int z;

  public VkOffset3D() {}
  public VkOffset3D(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
}
