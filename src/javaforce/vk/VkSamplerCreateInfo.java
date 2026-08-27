package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSamplerCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSamplerCreateInfo.html
 *
 * @author pquiring
 */

public class VkSamplerCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkBufferCreateFlags */
  public int flags;
  /** VkFilter */
  public int magFilter;
  /** VkFilter */
  public int minFilter;
  /** VkSamplerMipmapMode */
  public int mipmapMode;
  /** VkSamplerAddressMode */
  public int addressModeU;
  /** VkSamplerAddressMode */
  public int addressModeV;
  /** VkSamplerAddressMode */
  public int addressModeW;
  /** */
  public float mipLodBias;
  /** VkBool32 */
  public int anisotropyEnable;
  /** */
  public float maxAnisotropy;
  /** VkBool32 */
  public int compareEnable;
  /** */
  public VkCompareOp compareOp;
  /** */
  public float minLod;
  /** */
  public float maxLod;
  /** VkBorderColor */
  public int borderColor;
  /** VkBool32 */
  public int unnormalizedCoordinates;
}
