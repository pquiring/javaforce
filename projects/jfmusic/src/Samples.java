/**
 *
 * @author pquiring
 *
 * Created : Jan 19, 2014
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.media.*;

public class Samples extends JComponent implements MouseListener, MouseMotionListener{
  private SongPanel panel;
  private JFImage img;
  private Stroke normal, dashed;

  public Music.Sample sample;
  public int loopStart, loopEnd;
  public int sustainStart, sustainEnd;
  public float attenuation;
  public boolean showAttenuation, showLoop, showSustain;

  private int loopStartPos, loopEndPos;
  private int sustainStartPos, sustainEndPos;
  private int samplesPerPixel;
//  private float maxAttenuation;
  private int attenuationX, attenuationY;

  public Samples(SongPanel panel) {
    this.panel = panel;
    this.addMouseListener(this);
    this.addMouseMotionListener(this);
  }
  public void paint(Graphics g) {
    try {
      int width = getWidth();
      int height = getHeight();
      if (img == null) {
        img = new JFImage(width, height);
        normal = img.getGraphics2D().getStroke();
        dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f
          , new float[] {10.0f}, 0.0f);
      }
      if (img.getWidth() != width || img.getHeight() != height) {
        img.setImageSize(width, height);
      }
      img.fill(0, 0, width, height, 0);
      if (sample == null) return;
      short samples[] = sample.samples;
      samplesPerPixel = panel.getZoom();
      int pos = 0;
      int cnt = samples.length / samplesPerPixel;
      if (samples.length % samplesPerPixel != 0) cnt++;
      int hScale = 32768 / height;
      int height2 = height / 2;
      int last = height2;
      for(int a=0;a<cnt;a++) {
        if (a >= width) break;
        boolean drawLine = false;
        boolean dashedLine = false;
        int val = 0;
        for(int s=0;s<samplesPerPixel;s++) {
          int poss = pos + s;
          if (poss == samples.length) break;
          val += samples[poss];
          if (showLoop) {
            if (poss == loopStart) {drawLine = true; loopStartPos = a; dashedLine = false;}
            if (poss == loopEnd) {drawLine = true; loopEndPos = a; dashedLine = false;}
          }
          if (showSustain) {
            if (poss == sustainStart) {drawLine = true; sustainStartPos = a; dashedLine = true;}
            if (poss == sustainEnd) {drawLine = true; sustainEndPos = a; dashedLine = true;}
          }
        }
        val /= samplesPerPixel;
        val /= hScale;
        val += height2;
        if (val < 0) val = 0;
        if (val >= height) val = height-1;
        pos += samplesPerPixel;
        img.line(a-1,last,a,val,Color.red.getRGB());
        if (drawLine) {
          if (dashedLine) img.getGraphics2D().setStroke(dashed);
          img.line(a, 0, a, height-1, Color.white.getRGB());
          if (dashedLine) img.getGraphics2D().setStroke(normal);
        }
        last = val;
      }
      if (showAttenuation) {
        attenuationX = sample.samples.length / samplesPerPixel;
        attenuationY = (int)(attenuation * sample.samples.length * height);
        if (attenuationY >= height) attenuationY = height;
        img.line(0,0,attenuationX,attenuationY,Color.blue.getRGB());
        img.box(attenuationX-5,attenuationY-5,11,11,Color.blue.getRGB());
      }
      g.drawImage(img.getImage(), 0, 0, null);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void setSample(Music.Sample i) {
    sample = i;
//    maxAttenuation = 1.0f / ((float)instrument.samples.samples.length);
  }
  public Dimension getPreferredSize() {
    Dimension size = panel.getSamplesPaneSize();
    if (sample == null) return size;
    size.width = sample.samples.length / panel.getZoom();
    return size;
  }

  public void mouseClicked(MouseEvent e) {
  }

  private int drag = -1;

  public void mousePressed(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    int tx, ty;
    drag = -1;
    if (showAttenuation) {
      tx = x - attenuationX;
      ty = y - attenuationY;
      if (tx > -5 && tx < 5 && ty > -5 && ty < 5) {
        drag = 5;
        return;
      }
    }
    if (loopStart != -1) {
      tx = x - loopStartPos;
      if (tx > -5 && tx < 5) {
        drag = 1;
        return;
      }
      tx = x - loopEndPos;
      if (tx > -5 && tx < 5) {
        drag = 2;
        return;
      }
    }
    if (sustainStart != -1) {
      tx = x - sustainStartPos;
      if (tx > -5 && tx < 5) {
        drag = 3;
        return;
      }
      tx = x - sustainEndPos;
      if (tx > -5 && tx < 5) {
        drag = 4;
        return;
      }
    }
  }

  public void mouseReleased(MouseEvent e) {
    if (drag != -1) {
      panel.saveSample();
    }
    drag = -1;
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void mouseDragged(MouseEvent e) {
    if (drag == -1) return;
    int x = e.getX() * samplesPerPixel;
    if (x < 0) x = 0;
    if (x >= sample.samples.length) x = sample.samples.length - 1;
    int y = e.getY();
    int height = getHeight();
    if (y < 0) y = 0;
    if (y >= height) y = height - 1;
    switch (drag) {
      case 1: loopStart = x; break;
      case 2: loopEnd = x; break;
      case 3: sustainStart = x; break;
      case 4: sustainEnd = x; break;
      case 5:
        attenuation = (float)y;
        attenuation /= (float)height;
        attenuation /= (float)sample.samples.length;
        break;
    }
    repaint();
  }

  public void mouseMoved(MouseEvent e) {
  }
}
