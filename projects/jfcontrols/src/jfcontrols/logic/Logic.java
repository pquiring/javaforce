package jfcontrols.logic;

/** Base class for all logic blocks.
 *
 * @author pquiring
 */

public abstract class Logic {
  public enum Type {inline, box};

  public abstract Type getType();
  public abstract String getName();
  public abstract String getCode();

  public abstract int getTagsCount();
  public abstract int getTagType(int idx);

  //inline members
  public String getImage() {
    return getName().replaceAll(" ", "_").toLowerCase();
  }

  //box members
  public String getTagName(int idx) {return null;}
}
