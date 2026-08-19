package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDescriptorPoolSize.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorPoolSize.html
 *
 * @author pquiring
 */

public class VkDescriptorPoolSize extends FFMStruct {
  /** VkDescriptorType */
  public int type = 0;
  /** */
  public int descriptorCount;
}
