package javaforce.awt;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.undo.*;

/**
 * Extends JTextArea to provide some extra features such as overwrite mode, and
 * undo/redo
 */
public class JFTextArea extends JTextArea {

  private static Toolkit toolkit = Toolkit.getDefaultToolkit();
  private static boolean isOvertypeMode;
  private Caret defaultCaret;
  private Caret overtypeCaret;
  private UndoManager undo;

  public void clearHistory() {
    undo.discardAllEdits();
  }

  private void init() {
    //setup overwrite mode
    setCaretColor(Color.black);
    defaultCaret = getCaret();
    overtypeCaret = new OvertypeCaret();
    overtypeCaret.setBlinkRate(defaultCaret.getBlinkRate());
    setOvertypeMode(false);
    //setup undo manager
    undo = new UndoManager();
    // Listen for undo and redo events
    getDocument().addUndoableEditListener(new UndoableEditListener() {
      public void undoableEditHappened(UndoableEditEvent evt) {
        undo.addEdit(evt.getEdit());
      }
    });
    // Create an undo action and add it to the text component
    getActionMap().put("Undo", new AbstractAction("Undo") {
      public void actionPerformed(ActionEvent evt) {
        try {
          if (undo.canUndo()) {
            undo.undo();
          }
        } catch (CannotUndoException e) {
        }
      }
    });
    // Bind the undo action to ctl-Z
    getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
    // Create a redo action and add it to the text component
    getActionMap().put("Redo", new AbstractAction("Redo") {
      public void actionPerformed(ActionEvent evt) {
        try {
          if (undo.canRedo()) {
            undo.redo();
          }
        } catch (CannotRedoException e) {
        }
      }
    });
    // Bind the redo action to ctl-Y
    getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
  }

  public JFTextArea() {
    super();
    init();
  }

  public JFTextArea(int row, int column) {
    super(row, column);
    init();
  }

  /*
   *  Return the overtype/insert mode
   */
  public boolean isOvertypeMode() {
    return isOvertypeMode;
  }

  /*
   *  Set the caret to use depending on overtype/insert mode
   */
  public void setOvertypeMode(boolean isOvertypeMode) {
    this.isOvertypeMode = isOvertypeMode;
    int pos = getCaretPosition();

    if (isOvertypeMode()) {
      setCaret(overtypeCaret);
    } else {
      setCaret(defaultCaret);
    }

    setCaretPosition(pos);
  }

  /*
   *  Override method from JComponent
   */
  public void replaceSelection(String text) {
    //  Implement overtype mode by selecting the character at the current
    //  caret position

    if (isOvertypeMode()) {
      int pos = getCaretPosition();
      if ((getSelectedText() == null) && (pos < getDocument().getLength())) {
        moveCaretPosition(pos + 1);
      }
    }

    super.replaceSelection(text);
  }

  /*
   *  Override method from JComponent
   */
  protected void processKeyEvent(KeyEvent e) {
    super.processKeyEvent(e);

    //  Handle release of Insert key to toggle overtype/insert mode

    if (e.getID() == KeyEvent.KEY_RELEASED && e.getKeyCode() == KeyEvent.VK_INSERT) {
      setOvertypeMode(!isOvertypeMode());
    }
  }

  /*
   *  Paint a horizontal line the width of a column and 1 pixel high
   */
  private class OvertypeCaret extends DefaultCaret {
    /*
     *  The overtype caret will simply be a horizontal line one pixel high
     *  (once we determine where to paint it)
     */

    public void paint(Graphics g) {
      if (isVisible()) {
        try {
          JTextComponent component = getComponent();
          TextUI mapper = component.getUI();
          Rectangle2D r = mapper.modelToView2D(component, getDot(), Position.Bias.Forward);
          g.setColor(component.getCaretColor());
          int width = g.getFontMetrics().charWidth('w');
          int y = (int)(r.getY() + r.getHeight() - 2);
          g.drawLine((int)r.getX(), y, (int)(r.getX() + width - 2), y);
        } catch (BadLocationException e) {
        }
      }
    }

    /*
     *  Damage must be overridden whenever the paint method is overridden
     *  (The damaged area is the area the caret is painted in. We must
     *  consider the area for the default caret and this caret)
     */
    protected synchronized void damage(Rectangle r) {
      if (r != null) {
        JTextComponent component = getComponent();
        x = r.x;
        y = r.y;
        width = component.getFontMetrics(component.getFont()).charWidth('w');
        height = r.height;
        repaint();
      }
    }
  }
}
