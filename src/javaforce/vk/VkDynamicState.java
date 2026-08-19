package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDynamicState (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDynamicState.html
 *
 * @author pquiring
 */

public class VkDynamicState extends FFMType.Uint32 {
  public VkDynamicState() {}
  public VkDynamicState(int value) {super(value);}
  public VkDynamicState(FFMType.Uint32 value) {super(value);}
}
