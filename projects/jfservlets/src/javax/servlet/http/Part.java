package javax.servlet.http;

/** HTTP Request Part
 *
 * @author pquiring
 */

import java.io.*;

public interface Part {
  public String getContentType();
  public InputStream getInputStream();
}
