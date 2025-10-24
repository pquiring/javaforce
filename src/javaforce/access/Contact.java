package javaforce.access;

/** User contact.
 *
 * @author pquiring
 */

import java.io.*;

public class Contact implements Serializable {
  public int type;
  public int sub_type;
  public String value;

  //types
  public static int TYPE_EMAIL = 1;
  public static int TYPE_PHONE = 2;

  //phone sub_types
  public static int PHONE_HOME = 1;
  public static int PHONE_MOBILE = 2;
  public static int PHONE_WORK = 3;
}
