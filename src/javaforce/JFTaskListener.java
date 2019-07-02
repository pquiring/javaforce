package javaforce;

/**
 *
 * @author pquiring
 */

public interface JFTaskListener {
  public void setLabel(String label);
  public void setTitle(String title);
  public void setProgress(int progress);
  public void done();
  public void dispose();
}
