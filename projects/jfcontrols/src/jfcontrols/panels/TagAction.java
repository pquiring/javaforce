package jfcontrols.panels;

/** Tag change action
 *
 * @author pquiring
 */

import javaforce.webui.*;

import jfcontrols.tags.*;

public interface TagAction {
  public void tagChanged(TagBase tag, String oldValue, String newValue, Component cmp);
}
