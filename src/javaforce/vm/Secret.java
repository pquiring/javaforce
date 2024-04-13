package javaforce.vm;

/** Secret.
 *
 * Define a password for iSCSI chap auth.
 *
 * @author pquiring
 */

public class Secret {
  private static native boolean ncreate(String xml, String passwd);
  public static boolean create(String name, String passwd) {
    StringBuilder xml = new StringBuilder();
    xml.append("<secret ephemeral='yes' private='yes'>");
    xml.append("<usage type='iscsi'>");
    xml.append("<target>" + name + "</target>");
    xml.append("</usage>");
    xml.append("</secret>");
    return ncreate(xml.toString(), passwd);
  }
}
