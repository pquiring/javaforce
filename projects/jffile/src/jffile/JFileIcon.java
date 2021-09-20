package jffile;

/**
 * Created : Aug 14, 2012
 *
 * @author pquiring
 */

import java.awt.*;

import javaforce.*;
import javaforce.awt.*;

public class JFileIcon extends javax.swing.JComponent {

  /**
   * Creates new form JFileIcon for Desktop icons
   */
  public JFileIcon(JFileBrowser browser, JFImage icon, FileEntry entry, boolean iconView) {
    setLayout(null);
    this.browser = browser;
    this.iconView = iconView;
    this.icon = icon;
    this.entry = entry;
    if (iconView) {
      setSize(browser.bx, browser.by);
      ix = 16;
      iy = 8;
      tx = 0;
      ty = 16 + 48 + 4;
    } else {
      int w = browser.ix + icon.getGraphics().getFontMetrics().stringWidth(entry.name);
      setSize(w, browser.by);
      tx = 18;
      ty = 14;
    }
  }

  private JFImage icon, iconTransparent;
  private int ix, iy;
  private int tx, ty;
  private boolean iconView, selected, transparent;
  private JFileBrowser browser;

  public int dragX, dragY;
  public FileEntry entry;

  public void paintComponent(Graphics g) {
    if (selected) {
      if (transparent)
        g.setColor(new Color(0x7f005599, true));
      else
        g.setColor(new Color(0x005599));
      g.fillRect(0,0,getWidth(),getHeight());
    }
    if (transparent) {
      if (iconTransparent == null) {
        iconTransparent = new JFImage();
        iconTransparent.setSize(icon.getSize());
        int px[] = icon.getPixels();
        for(int a=0;a<px.length;a++) {
          px[a] &= 0x7fffffff;  //remove half of alpha
        }
        iconTransparent.putPixels(px,0,0,icon.getWidth(),icon.getHeight(),0);
      }
      g.drawImage(iconTransparent.getImage(), ix, iy, null);
      g.setColor(browser.foreClr);
    } else {
      g.drawImage(icon.getImage(), ix, iy, null);
      g.setColor(browser.foreClr);
    }
    String txt = entry.name;
    if (iconView) {
      int tw = g.getFontMetrics().stringWidth(txt);
      while (tw > getWidth()) {
        txt = txt.substring(0, txt.length() - 4) + "...";
        tw = g.getFontMetrics().stringWidth(txt);
      }
      tx = getWidth() / 2 - tw / 2;
      g.drawBytes(txt.getBytes(), 0, txt.length(), tx, ty);
    } else {
      g.drawBytes(txt.getBytes(), 0, txt.length(), tx, ty);
    }
  }

  public void setSelected(boolean state) {
    selected = state;
    repaint();
  }

  public boolean isSelected() {return selected;}

  public void setTransparent(boolean state) {
    transparent = state;
    repaint();
  }

  public boolean isTransparent() {return transparent;}

  public String getText() {return entry.name;}

  public void setText(String txt) {entry.name = txt;}

  public void setIcon(JFImage icon) {this.icon = icon; iconTransparent = null;}

}
