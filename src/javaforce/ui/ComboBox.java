package javaforce.ui;

/** Combo Box - combines TextField / Button and a ListBox (in a ScrollBox)
 *
 * @author pquiring
 */

import javaforce.*;

public class ComboBox extends Container {
  private TextField text;
  private Button button;
  private ListBox list;
  private ScrollBox scroll;
  private ChangeListener change;

  private int rows = 3;  //max visible rows in ListBox

  public ComboBox(String txt) {
    text = new TextField(txt);
    super.add(text);
    button = new Button(Icons.getArrowDown());
    button.setFocusable(false);
    super.add(button);
    list = new ListBox();
    list.setLayer(1);
    scroll = new ScrollBox(list, Direction.VERTICAL);
    scroll.setLayer(1);
    scroll.setVisible(false);
    super.add(scroll);
    button.setActionListner(new ActionListener() {
      public void actionPerformed(Component cmp) {
        list.setSelectedIndex(-1);
        scroll.setVisible(!scroll.isVisible());
        scroll.resetOffset();
      }
    });
    list.setChangeListener(new ChangeListener() {
      public void changed(Component cmp) {
        String txt = list.getSelectedItem();
        if (txt != null) {
          text.setText(txt);
          text.setCursorPosition(0, 0);
          if (change != null) {
            change.changed(ComboBox.this);
          }
        }
        scroll.setVisible(false);
      }
    });
  }

  public Dimension getMinSize() {
    return new Dimension(getWidth(), text.getMinHeight());
  }

  public void layout(LayoutMetrics metrics) {
    int org_width = metrics.size.width;
    int org_height = metrics.size.height;
    int org_x = metrics.pos.x;
    int org_y = metrics.pos.y;
    setPosition(metrics.pos);
    metrics.size.height = getHeight() + rows * list.getItemHeight();
    setSize(metrics.size);
    metrics.size.height = getMinHeight();
    metrics.size.width -= 16;
    text.layout(metrics);
    metrics.pos.x += metrics.size.width;
    metrics.size.width = 16;
    button.layout(metrics);
    metrics.pos.x = org_x;
    metrics.pos.y += metrics.size.height;
    metrics.size.width = org_width;
    metrics.size.height = rows * list.getItemHeight();
    scroll.layout(metrics);
    metrics.pos.y = org_y;
    metrics.size.height = org_height;
  }

  /** ComboBox is not a real Container, do not add other Components. */
  public void add(Component child) {}

  public void addItem(String item) {
    list.addItem(item);
  }

  public void removeItem(String item) {
    list.removeItem(item);
  }

  public String getText() {
    return text.getText();
  }

  public void setText(String txt) {
    text.setText(txt);
  }

  public boolean isEditable() {
    return text.isEditable();
  }

  public void setEditable(boolean state) {
    text.setEditable(state);
  }

  /** Sets number of items visible in drop down list. */
  public void setItemsVisible(int items) {
    rows = items;
  }

  public void setForeColor(Color clr) {
    super.setForeColor(clr);
    if (text == null) return;
    text.setForeColor(clr);
    list.setForeColor(clr);
    button.setForeColor(clr);
  }

  public void setBackColor(Color clr) {
    super.setBackColor(clr);
    if (text == null) return;
    text.setBackColor(clr);
    list.setBackColor(clr);
    button.setBackColor(clr);
  }

  public void setChangeListener(ChangeListener listener) {
    change = listener;
  }

  public String toString() {
    return "ComboBox:" + getText();
  }
}
