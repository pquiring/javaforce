package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineBindPoint (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineBindPoint.html
 *
 * @author pquiring
 */

public class VkPipelineBindPoint extends FFMType.Uint32 {
  public VkPipelineBindPoint() {}
  public VkPipelineBindPoint(int value) {super(value);}
  public VkPipelineBindPoint(FFMType.Uint32 value) {super(value);}
}
