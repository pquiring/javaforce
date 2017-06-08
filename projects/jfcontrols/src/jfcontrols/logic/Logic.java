package jfcontrols.logic;

/** Base class for all logic blocks.
 *
 * @author pquiring
 */

public abstract class Logic {
  /** Display as a block (else inline code) */
  public abstract boolean isBlock();
  public String getName() {
    String name = getClass().getName();
    int idx = name.lastIndexOf('.');
    return name.substring(idx + 1);
  }

  /** Block must be only one in rung. */
  public boolean isSolo() {return false;}

  /** Block must end a rung (no forking under it either)*/
  public boolean isLast() {return false;}

  /** Indicates block controls function flow and must be paired with an _END block. */
  public boolean isFlowControl() {return false;}
  /** Indicates block can end start block */
  public boolean canClose(String start) {return false;}

  /** Returns text for Block's only (not used for inline code) */
  public abstract String getDesc();
  public abstract String getCode(int[] tagTypes, boolean[] array, boolean[] unsigned);
  public String getCode(String func) {return null;}

  /** Returns number of tags logic needs.
   * Blocks can have 0 to 32 tags.
   * Inline can have either 1 or 0. */
  public abstract int getTagsCount();
  /** Returns the TagType expected for each tag.
   */
  public abstract int getTagType(int idx);

  //inline members
  public String getImage() {
    return getName().toLowerCase();
  }

  //box members
  public String getTagName(int idx) {return null;}
}
