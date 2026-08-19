package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkShaderModuleCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkShaderModuleCreateInfo.html
 *
 * @author pquiring
 */

public class VkShaderModuleCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkShaderModuleCreateFlags */
  public int flags;
  /** */
  public long codeSize;
  /** */
  public byte[] ptr_pCode;
}
