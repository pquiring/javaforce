package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkImageLayout.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkImageLayout.html
 *
 * @author pquiring
 */

public class VkImageLayout extends FFMType.Uint32 {
  public VkImageLayout() {}
  public VkImageLayout(int value) {super(value);}
  public VkImageLayout(FFMType.Uint32 value) {super(value);}
}
