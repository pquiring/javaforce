package viewer;

/** Video Panel
 *
 * @author pquiring
 */

import java.awt.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;

public class VideoPanel extends javax.swing.JPanel {

  /**
   * Creates new form VideoPanel
   */
  public VideoPanel() {
    initComponents();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    setPreferredSize(new java.awt.Dimension(1280, 720));
    addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
      public void mouseMoved(java.awt.event.MouseEvent evt) {
        formMouseMoved(evt);
      }
    });
    addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        formMouseClicked(evt);
      }
    });
    addComponentListener(new java.awt.event.ComponentAdapter() {
      public void componentResized(java.awt.event.ComponentEvent evt) {
        formComponentResized(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 485, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 271, Short.MAX_VALUE)
    );
  }// </editor-fold>//GEN-END:initComponents

  private void formMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseMoved
  }//GEN-LAST:event_formMouseMoved

  private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
    ViewerApp.panel.setVideoSize(getWidth(), getHeight());
  }//GEN-LAST:event_formComponentResized

  private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
    zoom(evt.getX(), evt.getY());
  }//GEN-LAST:event_formMouseClicked


  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables

  private JFImage img;
  private boolean needPainting = false;
  private boolean grid;
  private int gx, gy;
  private boolean zoom;
  private int zx, zy;

  private void init() {
    if (img != null) return;
    int x = getWidth();
    int y = getHeight();
    if (x > 0 && y > 0) {
      img = new JFImage(getWidth(), getHeight());
    }
  }

  public void start() {
    init();
  }

  public void resize() {
    img = new JFImage(getWidth(), getHeight());
  }

  public void stop() {
    if (ViewerApp.frame.isFullScreen()) {
      ViewerApp.frame.toggleFullScreen();
    }
  }

  public void setImage(JFImage src) {
    init();
    if (src.getWidth() == getWidth() && src.getHeight() == getHeight()) {
      img.putJFImage(src, 0, 0);
    } else {
      img.putJFImageScale(src, 0, 0, getWidth(), getHeight());
    }
    update();
  }

  public void setImage(JFImage src, int px, int py) {
    init();
    if (zoom) {
      if (px != zx || py != zy) return;
      setImage(src);
      return;
    }
    int img_w = getWidth();
    int img_h = getHeight();
    int w = img_w / gx;
    int h = img_h / gy;
    int x = w * px;
    int y = h * py;
    setImageRect(src, x, y, w, h);
    update();
  }

  private void setImageRect(JFImage src, int x, int y, int w, int h) {
    img.putJFImageScale(src, x, y, w, h);
  }

  private void update() {
    if (needPainting) {
      JFLog.log("Video updating too slow");
      return;
    }
    needPainting = true;
    try {
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          repaint();
        }
      });
    } catch (Exception e) {}
  }

  public void paintComponent(Graphics g) {
    int w = getWidth();
    int h = getHeight();
    //paint controls
    if (img == null) {
      g.fillRect(0, 0, w, h);
    } else {
      int iw = img.getWidth();
      int ih = img.getHeight();
      if (((iw != w) || (ih != h))) {
        JFLog.log("VideoPanel:image scaled");
        JFImage scaled = new JFImage();
        scaled.setImageSize(w, h);
        scaled.getGraphics().drawImage(img.getImage(), 0,0, w,h, 0,0, iw,ih, null);
        img = scaled;
      }
      g.drawImage(img.getImage(), 0, 0, null);
    }
    needPainting = false;
  }

  public void setGrid(int gx, int gy) {
    grid = true;
    this.gx = gx;
    this.gy = gy;
  }

  private void zoom(int x, int y) {
    if (zoom) {
      zoom = false;
    } else {
      int img_w = getWidth();
      int img_h = getHeight();
      int w = img_w / gx;
      int h = img_h / gy;
      zx = x / w;
      zy = y / h;
      zoom = true;
    }
  }
}
