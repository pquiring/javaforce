package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkMemoryRequirements.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkMemoryRequirements.html
 *
 * @author pquiring
 */

public class VkMemoryRequirements extends FFMStruct {
  /** */
  public long size;
  /** */
  public long alignment;
  /** */
  public int memoryTypeBits;
}
