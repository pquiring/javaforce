package javaforce.ui;

/** List Box
 *
 * @author pquiring
 */

public class ListBox extends Column {
  private Font font;
  private boolean multiSelection;
  private ChangeListener change;

  public ListBox() {
    font = Font.getSystemFont();
    setBorderStyle(LineStyle.SOLID);
  }

  public Dimension getMinSize() {
    int width = 0;
    int height = 0;
    for(ListItem item : getChildren()) {
      Dimension size = item.getMinSize();
      if (size.width > width) {
        width = size.width;
      }
      height += size.height;
    }
    if (getBorderStyle() != LineStyle.NONE) {
      width += 2;
      height += 2;
    }
    return new Dimension(width, height);
  }

  public int getItemHeight() {
    return font.getMaxHeight();
  }

  public boolean isMultiSelection() {
    return multiSelection;
  }

  public void setMultiSelection(boolean state) {
    multiSelection = state;
    setSelectedIndex(-1);
  }

  private void clearSelectionExcept(ListItem selected) {
    for(ListItem item : getChildren()) {
      if (item == selected) continue;
      if (item.isSelected()) {
        item.setSelected(false);
      }
    }
  }

  public void addItem(String text) {
    removeItem(text);  //no duplicates
    ListItem item = new ListItem(text);
    item.setFont(font);
    item.setActionListener((Component cmp) -> {
      if ((!multiSelection) || (!getKeyState(KeyCode.VK_CONTROL_L) && !getKeyState(KeyCode.VK_CONTROL_R))) {
        clearSelectionExcept((ListItem)cmp);
      }
      if (change != null) {
        change.changed(this);
      }
    });
    add(item);
  }

  public void setChangeListener(ChangeListener listener) {
    change = listener;
  }

  public void removeItem(String text) {
    for(ListItem item : getChildren(new ListItem[getChildCount()])) {
      if (item.getText().equals(text)) {
        remove(item);
        return;
      }
    }
  }

  public ListItem[] getChildren() {
    return getChildren(new ListItem[getChildCount()]);
  }

  public Font getFont() {
    return font;
  }

  public void setFont(Font font) {
    this.font = font;
    for(ListItem item : getChildren()) {
      item.setFont(font);
    }
  }

  public int getSelectedIndex() {
    int idx = 0;
    for(ListItem item : getChildren()) {
      if (item.isSelected()) {
        return idx;
      }
      idx++;
    }
    return -1;
  }

  public int[] getSelectedIndexes() {
    int idx = 0;
    int cnt = 0;
    for(ListItem item : getChildren()) {
      if (item.isSelected()) {
        cnt++;
      }
    }
    int[] list = new int[cnt];
    int pos = 0;
    for(ListItem item : getChildren()) {
      if (item.isSelected()) {
        list[pos++] = idx;
      }
      idx++;
    }
    return list;
  }

  public String getSelectedItem() {
    int idx = getSelectedIndex();
    if (idx == -1) return null;
    ListItem item = (ListItem)getChild(idx);
    return item.getText();
  }

  public void setSelectedIndex(int index) {
    int idx = 0;
    for(ListItem item : getChildren()) {
      if (idx == index) {
        if (!item.isSelected()) {
          item.setSelected(true);
        }
      } else {
        if (item.isSelected()) {
          item.setSelected(false);
        }
      }
      idx++;
    }
  }

  public String toString() {
    return "ListBox";
  }
}
