/**
 *
 * @author pquiring
 *
 * Created : Jan 20, 2014
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

public class SimpleTableCellRenderer implements TableCellRenderer {
  public Component getTableCellRendererComponent(JTable table, Object value,
     boolean isSelected, boolean hasFocus, int row, int column)
  {
    if (value instanceof String) {
      return new JLabel((String)value);
    }
    if (value instanceof TableCell) {
      TableCell cell = (TableCell)value;
      cell.setSelected(isSelected);
      return cell;
    }
    return (Component)value;
  }
}
