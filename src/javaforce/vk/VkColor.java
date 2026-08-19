package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkClearColorValue.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkClearColorValue.html
 *
 * @author pquiring
 */

public class VkColor extends FFMStruct {
  /** */
  public float[] color = new float[4];
}
