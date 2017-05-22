package jfcontrols.logic;

/** Base class for all logic blocks.
 *
 * @author pquiring
 */

import javaforce.*;

import jfcontrols.tags.*;

public abstract class Logic {
  public abstract boolean isBlock();
  public abstract String getName();
  public abstract String getCode();
  public String getCode(String func) {return null;}

  public abstract int getTagsCount();
  public abstract int getTagType(int idx);

  //inline members
  public String getImage() {
    return getName().replaceAll(" ", "_").toLowerCase();
  }

  //box members
  public String getTagName(int idx) {return null;}
}
