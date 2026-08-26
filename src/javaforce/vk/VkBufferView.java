package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkBufferView.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkBufferView.html
 *
 * @author pquiring
 */

public class VkBufferView extends FFMType.Uint64 {
  public VkBufferView() {}
  public VkBufferView(long value) {super(value);}
  public VkBufferView(FFMType.Uint64 value) {super(value);}
}
