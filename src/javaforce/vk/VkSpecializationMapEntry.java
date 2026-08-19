package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSpecializationMapEntry.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSpecializationMapEntry.html
 *
 * @author pquiring
 */

public class VkSpecializationMapEntry extends FFMStruct {
  /** */
  public int constantID;
  /** */
  public int offset;
  /** */
  public long size;
}
