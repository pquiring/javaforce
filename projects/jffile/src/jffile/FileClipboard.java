package jffile;

/** FileClipboard
 *
 * @author pquiring
 */

public interface FileClipboard {
  public void get();  //will invoke JFileBrowser.paste(String files[])
  public void set(String fileset);
  public void clear();
}
