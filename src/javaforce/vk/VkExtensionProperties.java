package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkExtensionProperties.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkExtensionProperties.html
 *
 * @author pquiring
 */

public class VkExtensionProperties extends FFMStruct {
  /** */
  public byte[] extensionName = new byte[256];
  /** */
  public int specVersion;
  /** */
  public String getExtensionName() {
    for(int i=0;i<256;i++) {
      if (extensionName[i] == 0) {
        return new String(extensionName, 0, i);
      }
    }
    return new String(extensionName);
  }
}
