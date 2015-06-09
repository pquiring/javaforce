package jffile;

/**
 * Created : Aug 14, 2012
 *
 * @author pquiring
 */

public interface JFileBrowserListener {
  public void browserResized(JFileBrowser browser);
  public void browserChangedPath(JFileBrowser browser, String newpath);
}
