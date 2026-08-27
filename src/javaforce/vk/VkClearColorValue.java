package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkClearColorValue (union).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkClearColorValue.html
 *
 * @author pquiring
 */

public class VkClearColorValue extends FFMStruct.Union {
  /** */
  public float[] floats = new float[4];
  /** */
  public int[] ints = new int[4];
}
