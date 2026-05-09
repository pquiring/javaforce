package javaforce.vm;

/** Secret.
 *
 * Define a password for iSCSI chap auth.
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.ffm.*;

public class Secret {
  private static FFMArray array = new FFMArray();
  public static boolean create(String name, String passwd) {
    StringBuilder xml = new StringBuilder();
    xml.append("<secret ephemeral='yes' private='yes'>");
    xml.append("<usage type='iscsi'>");
    xml.append("<target>" + name + "</target>");
    xml.append("</usage>");
    xml.append("</secret>");
    return VMAPI.getInstance(array).vmSecretCreate(xml.toString(), passwd);
  }
}
