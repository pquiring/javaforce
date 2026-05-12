package javax.servlet.http;

/** HTTP Part Implementation
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.service.*;

public class PartImpl implements Part {

  private WebFile file;

  public PartImpl(WebFile file) {
    this.file = file;
  }

  public String getContentType() {
    return file.getContentType();
  }

  public InputStream getInputStream() {
    return file.getInputStream();
  }
}
