package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkVertexInputBindingDescription.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkVertexInputBindingDescription.html
 *
 * @author pquiring
 */

public class VkVertexInputBindingDescription extends FFMStruct {
  /** */
  public int binding;
  /** */
  public int stride;
  /** */
  public VkVertexInputRate inputRate = new VkVertexInputRate();
}
