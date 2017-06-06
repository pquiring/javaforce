package jfcontrols.logic;

/** Base class for all logic blocks.
 *
 * @author pquiring
 */

public abstract class Logic {
  /** Display as a block (else inline code) */
  public abstract boolean isBlock();
  public final String getName() {
    String name = getClass().getName();
    int idx = name.lastIndexOf('.');
    return name.substring(idx + 1);
  }
  /** Returns text for Block's only (not used for inline code) */
  public abstract String getDesc();
  public abstract String getCode(int tagTypes[]);
  public String getCode(String func) {return null;}

  /** Blocks can have 0 to 32 tags.  Inline can have either 1 or 0. */
  public abstract int getTagsCount();
  public abstract int getTagType(int idx);

  //inline members
  public String getImage() {
    return getName().toLowerCase();
  }

  //box members
  public String getTagName(int idx) {return null;}
}
