package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPushConstantRange.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPushConstantRange.html
 *
 * @author pquiring
 */

public class VkPushConstantRange extends FFMStruct {
  /** VkShaderStageFlags */
  public int stageFlags;
  /** */
  public int offset;
  /** */
  public int size;
}
