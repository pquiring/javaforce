package service;

/** Port Settings
 *
 * @author pquiring
 */

import java.io.*;

public class PortSettings implements Serializable {
  private static final long serialVersionUID = 1L;

  public String port;
  public String name;  //desc
  public String baud;
}
