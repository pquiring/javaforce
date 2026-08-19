package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSpecializationInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSpecializationInfo.html
 *
 * @author pquiring
 */

public class VkSpecializationInfo extends FFMStruct {
  /** */
  public int mapEntryCount;
  /** */
  public VkSpecializationMapEntry ptr_pMapEntries = new VkSpecializationMapEntry();
  /** */
  public long dataSize;
  /** */
  public long data;
}
