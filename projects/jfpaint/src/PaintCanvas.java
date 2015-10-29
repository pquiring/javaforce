import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

import javaforce.*;

import com.jhlabs.image.*;

public class PaintCanvas extends JComponent implements MouseListener, MouseMotionListener, KeyListener {

  public static MainPanel mainPanel;
  public int mx=0, my=0;  //current mouse pos
  public int sx=0, sy=0;  //starting draw pos
  public boolean drag = false;  //a draggable selection/text box is visible
  public JFImage img[], bimg, fimg;  //the "real" image / background / foreground images
  public JFImage limg;  //alpha, color layers
  public JFImage cimg;  //current color layer
  public boolean dirty = false, undoDirty = false;
  public ArrayList<Undo> undos = new ArrayList<Undo>();
  public int undoPos = 0;
  public static int MAX_UNDO_SIZE = 7;
  public Border border_east, border_south, border_corner;
  public JPanel parentPanel;
  public JScrollPane scroll;
  public int button = -1;
  public boolean textCursor;
  public int selBoxIdx = 0;  //dashed line
  public float scale = 100;
  public boolean disableScale = false;  //for file operations
  public boolean show[];
  public String name[];

  private int colorLayer = 0;
  private int imageLayer = 0;
  private int imageLayers = 1;

  private void init() {
    addMouseListener(this);
    addMouseMotionListener(this);
    addKeyListener(this);
    setFocusable(true);
  }

  public PaintCanvas(JPanel parent, JScrollPane scroll) {
    init();
    parentPanel = parent;
    this.scroll = scroll;
    img = new JFImage[1];
    img[imageLayer] = new JFImage();
    show = new boolean[1];
    show[imageLayer] = true;
    name = new String[1];
    name[imageLayer] = "Background";
    bimg = new JFImage();
    fimg = new JFImage();
    limg = new JFImage();
    cimg = img[imageLayer];
    setName("image");
  }

  public void setImageSize(int x,int y) {
    if (x <= 0) x = 1;
    if (y <= 0) y = 1;

    for(int a=0;a<imageLayers;a++) {
      img[a].setImageSize(x, y);
    }
    limg.setImageSize(x, y);

    bimg.setImageSize(x, y);

    fimg.setImageSize(x, y);

    setSize(x,y);

    setPreferredSize(new Dimension(x,y));
    backClear();
    foreClear();
  }

  public void paint(Graphics g) {
    if (img == null) return;
    if (img[imageLayer].getImage() == null) return;
//    JFLog.log("PaintCanvas:paint():layers=" + imageLayers);
//    JFLog.log("\nscale=" + scale);
    Rectangle clip = g.getClipBounds();
//    JFLog.log("clipA=" + clip);
    //align clip to max scaled pixel size
    int t;
    t = clip.x & 0x7;
    if (t > 0) {
      clip.x &= 0xfffffff8;
      clip.width += t;
    }
    t = clip.width & 0x7;
    if (t > 0) {
      clip.width &= 0xfffffff8;
      clip.width += 8;
    }
    t = clip.y & 0x7;
    if (t > 0) {
      clip.y &= 0xfffffff8;
      clip.height += t;
    }
    t = clip.height & 0x7;
    if (t > 0) {
      clip.height &= 0xfffffff8;
      clip.height += 8;
    }
//    JFLog.log("clipB=" + clip);
    int dx1 = clip.x;
    int dy1 = clip.y;
    int dx2 = dx1 + clip.width;
    int dy2 = dy1 + clip.height;
    int sx1 = clip.x;
    int sy1 = clip.y;
    int sx2 = sx1 + clip.width;
    int sy2 = sy1 + clip.height;
    float div = 100f / scale;
    sx1 *= div;
    sy1 *= div;
    sx2 *= div;
    sy2 *= div;
//    JFLog.log("src=" + sx1 + "," + sy1 + "," + sx2 + "," + sy2);
//    JFLog.log("dst=" + dx1 + "," + dy1 + "," + dx2 + "," + dy2);
    if (colorLayer == 0) {
      g.drawImage(bimg.getImage()
        , dx1, dy1, dx2, dy2
        , sx1, sy1, sx2, sy2
        , null);
      for(int a=0;a<imageLayers;a++) {
//        JFLog.log("layer=" + name[a]);
        if (a == imageLayer) {
          g.drawImage(cimg.getImage()
            , dx1, dy1, dx2, dy2
            , sx1, sy1, sx2, sy2
            , null);
        } else {
          if (show[a]) g.drawImage(img[a].getImage()
            , dx1, dy1, dx2, dy2
            , sx1, sy1, sx2, sy2
            , null);
        }
      }
    } else {
      g.drawImage(cimg.getImage()
        , dx1, dy1, dx2, dy2
        , sx1, sy1, sx2, sy2
        , null);
    }
    g.drawImage(fimg.getImage()
      , dx1, dy1, dx2, dy2
      , sx1, sy1, sx2, sy2
      , null);
  }

  public void setSize(int x, int y) {
//    System.out.println("PaintCanvas.setSize:" + x + "," + y + ":" + getName());
    super.setSize(x,y);  //calls setBounds()
  }

  public void setSize(Dimension d) {
//    System.out.println("PaintCanvas.setSize:" + d + ":" + getName());
    super.setSize(d);  //calls setBounds()
  }

  public void setBounds(int x,int y,int w,int h) {
//    System.out.println("PaintCanvas.setBounds:" + x + "," + y + "," + w + "," + h + ":" + getName());
    super.setBounds(x,y,w,h);
  }

  public void createBorders() {
    border_east = new Border(this, Border.Types.east) {
      public void paint(Graphics g) {
        int x = getScaledWidth();
        int y = getScaledHeight();
        g.setColor(new Color(0x888888));
        g.fillRect(0,0, 10,y);
        g.setColor(new Color(0x000000));
        g.fillRect(0,y/2-5, 10,10);
      }
    };
    border_south = new Border(this, Border.Types.south) {
      public void paint(Graphics g) {
        int x = getScaledWidth();
        int y = getScaledHeight();
        g.setColor(new Color(0x888888));
        g.fillRect(0,0, x,10);
        g.setColor(new Color(0x000000));
        g.fillRect(x/2-5,0, 10,10);
      }
    };
    border_corner = new Border(this, Border.Types.corner) {
      public void paint(Graphics g) {
        g.setColor(new Color(0x000000));
        g.fillRect(0,0, 10,10);
      }
    };
  }

  public void resizeBorder() {
//    System.out.println("doLayout");
    parentPanel.doLayout();
  }

  public Dimension getPreferredSize() {
    Dimension ps = super.getPreferredSize();
//    System.out.println("PaintCanvas.getPreferredSize()" + ps);
    return ps;
  }

  public Dimension getMinimumSize() {
//    System.out.println("PaintCanvas.getMinimumSize()" + getPreferredSize());
    return getPreferredSize();
  }

  public Dimension getMaximumSize() {
//    System.out.println("PaintCanvas.getMaximumSize()" + getPreferredSize());
    return getPreferredSize();
  }

  public void setScale(float newScale) {
//    System.out.println("     setScale:" + newScale);
    int x = (int)(img[imageLayer].getWidth() * newScale / 100f);
    int y = (int)(img[imageLayer].getHeight() * newScale / 100f);
    setPreferredSize(new Dimension(x,y));
    scale = newScale;
  }

  public int getWidth() {
//    System.out.println("getWidth=" + super.getWidth());
    if (disableScale) return getUnscaledWidth();
    return getScaledWidth();
  }

  public int getHeight() {
//    System.out.println("getHeight=" + super.getHeight());
    if (disableScale) return getUnscaledHeight();
    return getScaledHeight();
  }

  public int getScaledWidth() {
//    System.out.println("getScaledWidth=" + super.getWidth());
    return (int)(img[imageLayer].getWidth() * scale / 100f);
  }

  public int getScaledHeight() {
//    System.out.println("getScaledHeight=" + super.getHeight());
    return (int)(img[imageLayer].getHeight() * scale / 100f);
  }

  public int getUnscaledWidth() {
    return img[imageLayer].getWidth();
  }

  public int getUnscaledHeight() {
    return img[imageLayer].getHeight();
  }

  public void setLineWidth(int w) {
    cimg.getGraphics2D().setStroke(new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
  }
  public void drawLine(int x1,int y1,int x2,int y2) {
    cimg.getGraphics().drawLine(x1,y1,x2,y2);
  }
  public void drawBox(int x1,int y1,int x2,int y2) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    cimg.getGraphics().drawRect(x1,y1,x2-x1,y2-y1);  //NOTE : left=x1 right=x1+width (strange)
  }
  public void drawRoundBox(int x1,int y1,int x2,int y2,int ax,int ay) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    cimg.getGraphics().drawRoundRect(x1,y1,x2-x1,y2-y1,ax,ay);  //NOTE : left=x1 right=x1+width (strange)
  }
  public void drawCircle(int x1,int y1,int x2,int y2) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    cimg.getGraphics().drawOval(x1,y1,x2-x1+1,y2-y1+1);
  }
  public void drawCurve(int cx[], int cy[]) {
    cimg.getGraphics2D().draw(new CubicCurve2D.Double(cx[0],cy[0], cx[2],cy[2], cx[3],cy[3], cx[1],cy[1]));
  }
  public void fillBox(int x1,int y1,int x2,int y2) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    cimg.getGraphics().fillRect(x1,y1,x2-x1+1,y2-y1+1);
  }
  public void fillRoundBox(int x1,int y1,int x2,int y2,int ax,int ay) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    cimg.getGraphics().fillRoundRect(x1,y1,x2-x1+1,y2-y1+1,ax,ay);
  }
  public void fillCircle(int x1,int y1,int x2,int y2) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    cimg.getGraphics().fillOval(x1,y1,x2-x1+1,y2-y1+1);
  }
  private class Point {
    public int x,y,dir;
    public Point(int x,int y,int dir) {this.x=x; this.y=y; this.dir=dir;}
  }
  private static final byte TODO = 0;
  private static final byte DONE = 1;
  private static final byte PAINT = 2;
  private int match_threshold;
  private int match_target;
  private boolean match(int px) {
    if (match_threshold > 0) {
      //check RGB values against threshold
      int c1, c2, diff;
      c1 = (px & 0xff0000) >> 16;
      c2 = (match_target & 0xff0000) >> 16;
      diff = Math.abs(c1-c2);
      c1 = (px & 0xff00) >> 8;
      c2 = (match_target & 0xff00) >> 8;
      diff += Math.abs(c1-c2);
      c1 = px & 0xff;
      c2 = match_target & 0xff;
      diff += Math.abs(c1-c2);
      diff /= 3;
      return (diff <= match_threshold);
    } else {
      //must be exact match
      return (px & JFImage.RGB_MASK) == match_target;
    }
  }
  public void fillFast(int x1,int y1,int clr, boolean hasAlpha, boolean edge, int threshold) {
    //threshold : 0=exact 255=everything
    this.match_threshold = threshold;
    match_target = cimg.getPixel(x1,y1) & JFImage.RGB_MASK;
    if (match_target == (clr & JFImage.RGB_MASK)) return;
    int w = cimg.getWidth();
    int h = cimg.getHeight();
    int px[] = cimg.getBuffer();
    byte done[] = new byte[w * h];  //to keep track of what has been filled in already
    Vector<Point> pts = new Vector<Point>();
    pts.add(new Point(x1,y1,1));
    if (x1 > 0) pts.add(new Point(x1-1,y1,-1));
    int x,y,dir,p;
    boolean top,bottom;
    while (pts.size() > 0) {
      Point pt = pts.remove(0);
      x = pt.x;
      y = pt.y;
      dir = pt.dir;
      top = false;
      bottom = false;
      while ((x >= 0) && (x < w)) {
        p = y * w + x;
        if (done[p] != TODO || (!match(px[p]))) {
          break;
        }
        if (edge) {
          if ( (x > 0 && (!match(px[p-1])))
          || (x < w-1 && (!match(px[p+1])))
          || (y > 0 && (!match(px[p-w])))
          || (y < h-1 && (!match(px[p+w]))) )
          {
            done[p] = PAINT;
          } else {
            done[p] = DONE;
          }
        } else {
          if (hasAlpha) {
            px[p] = clr;
          } else {
            px[p] &= JFImage.ALPHA_MASK;
            px[p] |= clr;
          }
          done[p] = DONE;  //not needed
        }
        if (y > 0) {
          if (!top) {
            if (done[p-w] == TODO && (match(px[p-w]))) {
              top=true;
              pts.add(new Point(x,y-1,1));
              if (x > 0) pts.add(new Point(x-1,y-1,-1));
            }
          } else {
            if (done[p-w] == TODO && (!match(px[p-w]))) top=false;
          }
        }
        if (y < h-1) {
          if (!bottom) {
            if (done[p+w] == TODO && (match(px[p+w]))) {
              bottom=true;
              pts.add(new Point(x,y+1,1));
              if (x > 0) pts.add(new Point(x-1,y+1,-1));
            }
          } else {
            if (done[p+w] == TODO && (!match(px[p+w]))) bottom=false;
          }
        }
        x += dir;
      }
    }
    if (edge) {
      //do all edge painting after
      int wh = w * h;
      for(p=0;p<wh;p++) {
        if (done[p] == PAINT) {
          if (hasAlpha) {
            px[p] = clr;
          } else {
            px[p] &= JFImage.ALPHA_MASK;
            px[p] |= clr;
          }
        }
      }
    }
  }
  public void fillSlow(int x1,int y1, boolean edge, int threshold) {
    int w = getUnscaledWidth();
    int h = getUnscaledHeight();
    Graphics2D g = img[imageLayer].getGraphics2D();
    match_target = img[imageLayer].getPixel(x1,y1) & JFImage.RGB_MASK;
    match_threshold = threshold;
    int px[] = cimg.getBuffer();
    byte done[] = new byte[w * h];  //to keep track of what has been filled in already
    Vector<Point> pts = new Vector<Point>();
    pts.add(new Point(x1,y1,1));
    if (x1 > 0) pts.add(new Point(x1-1,y1,-1));
    int x,y,dir,p;
    boolean top,bottom;
    while (pts.size() > 0) {
      Point pt = pts.remove(0);
      x = pt.x;
      y = pt.y;
      dir = pt.dir;
      top = false;
      bottom = false;
      while ((x >= 0) && (x < w)) {
        p = y * w + x;
        if ((!match(px[p])) || (done[p] != TODO)) {
          break;
        }
        if (edge) {
          if ( (x > 0 && (!match(px[p-1])))
          || (x < w-1 && (!match(px[p+1])))
          || (y > 0 && (!match(px[p-w])))
          || (y < h-1 && (!match(px[p+w]))) )
          {
            done[p] = PAINT;
          } else {
            done[p] = DONE;
          }
        } else {
          g.fillRect(x,y,1,1);  //slow
          done[p] = DONE;
        }
        if (y > 0) {
          if (!top) {
            if ((match(px[p-w])) && (done[p-w] == TODO)) {
              top=true;
              pts.add(new Point(x,y-1,1));
              if (x > 0) pts.add(new Point(x-1,y-1,-1));
            }
          } else {
            if (!match(px[p-w])) top=false;
          }
        }
        if (y < h-1) {
          if (!bottom) {
            if ((match(px[p+w])) && (done[p+w] == TODO)) {
              bottom=true;
              pts.add(new Point(x,y+1,1));
              if (x > 0) pts.add(new Point(x-1,y+1,-1));
            }
          } else {
            if (!match(px[p+w])) bottom=false;
          }
        }
        x += dir;
      }
    }
    if (edge) {
      //do all edge painting after
      p = 0;
      for(y=0;y<h;y++) {
        for(x=0;x<w;x++) {
          if (done[p] == PAINT) {
            g.fillRect(x,y,1,1);  //slow
          }
          p++;
        }
      }
    }
  }
  //changes clr1 -> clr2
  public void subBoxFast(int x1, int y1, int x2, int y2, int clr1, int clr2, int threshold) {
    int clr = clr1 & JFImage.RGB_MASK;
    int w = getUnscaledWidth();
    int h = getUnscaledHeight();
    int px[] = img[imageLayer].getBuffer();
    int x,y,p;
    //do clipping
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    if (x1 < 0) x1 = 0;
    if (y1 < 0) y1 = 0;
    if (x2 < 0) x2 = 0;
    if (y2 < 0) y2 = 0;
    if (x1 >= w) x1 = w-1;
    if (y1 >= h) y1 = h-1;
    if (x2 >= w) x2 = w-1;
    if (y2 >= h) y2 = h-1;
    match_threshold = threshold;
    match_target = clr;
    for(y=y1;y<=y2;y++) {
      p = y * w + x1;
      for(x=x1;x<=x2;x++,p++) {
        if (!match(px[p])) continue;
        px[p] &= JFImage.ALPHA_MASK;
        px[p] |= clr2;
      }
    }
  }
  public void subBoxSlow(int x1, int y1, int x2, int y2, int clr1, int threshold) {
    int clr = clr1 & JFImage.RGB_MASK;
    match_threshold = threshold;
    match_target = clr;
    int w = getUnscaledWidth();
    int h = getUnscaledHeight();
    int px[] = img[imageLayer].getPixels();
    Graphics2D g = img[imageLayer].getGraphics2D();
    int x,y,p;
    //do clipping
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    if (x1 < 0) x1 = 0;
    if (y1 < 0) y1 = 0;
    if (x2 < 0) x2 = 0;
    if (y2 < 0) y2 = 0;
    if (x1 >= w) x1 = w-1;
    if (y1 >= h) y1 = h-1;
    if (x2 >= w) x2 = w-1;
    if (y2 >= h) y2 = h-1;
    for(y=y1;y<=y2;y++) {
      p = y * w + x1;
      for(x=x1;x<=x2;x++,p++) {
        if (!match(px[p])) continue;
        g.fillRect(x,y,1,1);  //slow
      }
    }
  }
  public void setFont(Font font) {
    cimg.setFont(font);
  }
  public void drawText(String txt[], int x, int y, int dy, float sx, float sy, float rotate) {
    Graphics2D g = cimg.getGraphics2D();
    AffineTransform org = g.getTransform();
    g.scale(sx, sy);
    g.translate(x,y);
    x = 0;
    y = dy;
    if (rotate != 0) {
      g.rotate(rotate);
    }
    for(int a=0;a<txt.length;a++) {
      g.drawString(txt[a], x, y);
      y += dy;
    }
    g.setTransform(org);
  }
  public static boolean swap;
  public void backClear() {
    //create a checkered board on the back image (only visible if there is any transparent parts)
    int x = getUnscaledWidth();
    int y = getUnscaledHeight();
//System.out.println("backclear:"+x+","+y);
    bimg.fill(0,0,x,y,0xffffff);
    JFImage tmp = new JFImage();
    tmp.setImageSize(16,16);
    Graphics g = tmp.getGraphics();
    g.setColor(new Color(0x888888));
    if (swap) {
      g.fillRect(0,0,8,8);
      g.fillRect(8,8,8,8);
    } else {
      g.fillRect(8,0,8,8);
      g.fillRect(0,8,8,8);
    }
    Graphics2D g2d = bimg.getGraphics2D();
    g2d.setPaint(new TexturePaint(tmp.getBufferedImage(), new Rectangle2D.Double(0,0,16,16) ));
    g2d.fillRect(0,0,x,y);
  }
  public void foreClear() {
    fimg.fillAlpha(0,0,img[imageLayer].getWidth(),img[imageLayer].getHeight(),0x00);  //transparent
    repaint();
  }
  public void foreDrawLine(int x1,int y1,int x2,int y2) {
    fimg.getGraphics().drawLine(x1,y1,x2,y2);
  }
  public void foreDrawBox(int x1,int y1,int x2,int y2) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    fimg.getGraphics().drawRect(x1,y1,x2-x1,y2-y1);  //NOTE : left=x1 right=x1+width (strange)
  }
  public void foreDrawRoundBox(int x1,int y1,int x2,int y2,int ax, int ay) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    fimg.getGraphics().drawRoundRect(x1,y1,x2-x1,y2-y1,ax,ay);  //NOTE : left=x1 right=x1+width (strange)
  }
  public void foreDrawCircle(int x1,int y1,int x2,int y2) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    fimg.getGraphics().drawOval(x1,y1,x2-x1+1,y2-y1+1);
  }
  public void foreDrawSelBox(int x1,int y1,int x2,int y2, float rotate) {
    int tmp;
    Graphics2D fg = fimg.getGraphics2D();
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    fg.setColor(new Color(0x00));
    AffineTransform org = fg.getTransform();
    fg.translate(x1, y1);
    if (rotate != 0) {
      fg.rotate(rotate);
    }
    int width = x2-x1;
    int height = y2-y1;
    x2 -= x1;
    y2 -= y1;
    x1 = 0;
    y1 = 0;
    fg.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,0, new float[]{9}, selBoxIdx++));
    if (selBoxIdx == 9) selBoxIdx = 0;
    //NOTE : left=x1 right=x1+width (strange)
    int width2 = width / 2 - 2;
    int height2 = height / 2 - 2;
    fg.drawRect(x1, y1, width, height);
    fg.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
    fg.fillRect(x1-5,y1-5,6,6);  //NW box
    fg.fillRect(x1+width2,y1-5,6,6);  //N box
    fg.fillRect(x1+width,y1-5,6,6);  //NE box
    fg.fillRect(x1+width,y1+height2,6,6);  //E box
    fg.fillRect(x1+width,y1+height,6,6);  //SE box
    fg.fillRect(x1+width2,y1+height,6,6);  //S  box
    fg.fillRect(x1-5,y1+height,6,6);  //SW box
    fg.fillRect(x1-5,y1+height2,6,6);  //W box
    fg.drawImage(mainPanel.rotateImg.getImage(),x1+width+5,y1+height+5,null);  //R box
    fg.setTransform(org);
  }
  public void forePutPixels(int px[], int x, int y, int w, int h, int iw, int ih, float rotate) {
    Graphics2D fg = fimg.getGraphics2D();
    BufferedImage bi = new BufferedImage(iw,ih,BufferedImage.TYPE_INT_ARGB);
    bi.setRGB(0, 0, iw, ih, px, 0, iw);
    AffineTransform transform = new AffineTransform();
    transform.translate(x, y);
    if (rotate != 0) {
      transform.rotate(rotate);
    }
    transform.scale((float)w / (float)iw, (float)h / (float)ih);
    fg.drawImage(bi, transform, null);
  }
  public void putPixels(int px[], int x, int y, int w, int h, int iw, int ih, float rotate) {
    Graphics2D fg = img[getImageLayer()].getGraphics2D();
    BufferedImage bi = new BufferedImage(iw,ih,BufferedImage.TYPE_INT_ARGB);
    bi.setRGB(0, 0, iw, ih, px, 0, iw);
    AffineTransform transform = new AffineTransform();
    transform.translate(x, y);
    if (rotate != 0) {
      transform.rotate(rotate);
    }
    transform.scale((float)w / (float)iw, (float)h / (float)ih);
    fg.drawImage(bi, transform, null);
  }
  public void forePutPixelsKeyClr(int px[], int x, int y, int w, int h, int offset, int keyclr) {
    fimg.putPixelsKeyClr(px, x, y, w, h, offset, keyclr);
  }
  public void forePutPixelsBlend(int px[], int x, int y, int w, int h, int offset) {
    fimg.putPixelsBlend(px, x, y, w, h, offset, true);
  }
  public void forePutPixelsBlendKeyClr(int px[], int x, int y, int w, int h, int offset, int keyclr) {
    fimg.putPixelsBlendKeyClr(px, x, y, w, h, offset, true, keyclr);
  }
  public void foreDrawCurve(int cx[], int cy[]) {
    fimg.getGraphics2D().draw(new CubicCurve2D.Double(cx[0],cy[0], cx[2],cy[2], cx[3],cy[3], cx[1],cy[1]));
  }
  public void foreFillBox(int x1,int y1,int x2,int y2) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    fimg.getGraphics().fillRect(x1,y1,x2-x1+1,y2-y1+1);
  }
  public void foreFillRoundBox(int x1,int y1,int x2,int y2,int ax,int ay) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    fimg.getGraphics().fillRoundRect(x1,y1,x2-x1+1,y2-y1+1,ax,ay);
  }
  public void foreFillCircle(int x1,int y1,int x2,int y2) {
    int tmp;
    if (x1 > x2) {tmp=x1; x1=x2; x2=tmp;}
    if (y1 > y2) {tmp=y1; y1=y2; y2=tmp;}
    fimg.getGraphics().fillOval(x1,y1,x2-x1+1,y2-y1+1);
  }
  public void foreSetFont(Font font) {
    fimg.setFont(font);
  }
  public void foreDrawText(String txt[], int x, int y, int dy, float sx, float sy, float rotate) {
    Graphics2D g = fimg.getGraphics2D();
    AffineTransform org = g.getTransform();
    g.scale(sx, sy);
    g.translate(x,y);
    x = 0;
    y = dy;
    if (rotate != 0) {
      g.rotate(rotate);
    }
    for(int a=0;a<txt.length;a++) {
      g.drawString(txt[a], x, y);
      y += dy;
    }
    g.setTransform(org);
  }
  public void rotateCW() {
    int w = getUnscaledWidth();
    int h = getUnscaledHeight();
    int px1[] = img[imageLayer].getPixels();
    int p1;
    int px2[] = new int[w*h];
    int p2;
    for(int y=0;y<h;y++) {
      p1 = y * w;
      p2 = h - 1 - y;
      for(int x=0;x<w;x++) {
        px2[p2] = px1[p1++];
        p2 += h;
      }
    }
    setImageSize(h,w);
    img[imageLayer].putPixels(px2,0,0,h,w,0);
  }
  public void rotateCCW() {
    int w = getUnscaledWidth();
    int h = getUnscaledHeight();
    int px1[] = img[imageLayer].getPixels();
    int p1;
    int px2[] = new int[w*h];
    int p2;
    for(int y=0;y<h;y++) {
      p1 = y * w;
      p2 = h * (w-1) + y;
      for(int x=0;x<w;x++) {
        px2[p2] = px1[p1++];
        p2 -= h;
      }
    }
    setImageSize(h,w);
    img[imageLayer].putPixels(px2,0,0,h,w,0);
  }
  public void flipVert() {
    int w = getUnscaledWidth();
    int h = getUnscaledHeight();
    int px1[] = img[imageLayer].getPixels();
    int p1;
    int px2[] = new int[w*h];
    int p2;
    for(int y=0;y<h;y++) {
      p1 = y * w;
      p2 = p1 + w - 1;
      for(int x=0;x<w;x++) {
        px2[p2--] = px1[p1++];
      }
    }
    img[imageLayer].putPixels(px2,0,0,w,h,0);
  }
  public void flipHorz() {
    int w = getUnscaledWidth();
    int h = getUnscaledHeight();
    int px1[] = img[imageLayer].getPixels();
    int p1 = 0;
    int px2[] = new int[w*h];
    int p2 = w * (h-1);
    for(int y=0;y<h;y++) {
      System.arraycopy(px1, p1, px2, p2, w);
      p1 += w;
      p2 -= w;
    }
    img[imageLayer].putPixels(px2,0,0,w,h,0);
  }
  public void scaleImage(int ws, int hs) {
    int w = getUnscaledWidth();
    int h = getUnscaledHeight();
    if ((ws == 100) && (hs == 100)) return;
    int neww = w * ws / 100;
    int newh = h * hs / 100;
    if (neww < 1) neww = 1;
    if (newh < 1) newh = 1;
    JFImage tmp[] = new JFImage[imageLayers];
    for(int a=0;a<imageLayers;a++) {
      tmp[a] = new JFImage();
      tmp[a].setImageSize(neww, newh);
      tmp[a].getGraphics().drawImage(img[imageLayer].getImage(),0, 0, neww, newh, 0, 0, w, h, null);
    }
    setImageSize(neww, newh);
    for(int a=0;a<imageLayers;a++) {
      img[a].getGraphics().drawImage(tmp[a].getImage(), 0, 0, null);
    }
  }
  public void createUndo() {
    createUndo("undo");
  }
  public void createUndo(String type) {
//    JFLog.log("create:" + type);
    int w = getUnscaledWidth();
    int h = getUnscaledHeight();
    try {
      Undo undo = new Undo();
      undo.img.setImageSize(w, h);
      applyColorLayer();
      int px[] = img[imageLayer].getPixels();
      undo.img.putPixels(px, 0, 0, w, h, 0);
      undo.imageLayer = imageLayer;
      undoPos++;
      undos.add(undo);
      while(undos.size() > undoPos) {
        undos.remove(undos.size()-1);  //remove redos
      }
      if (undos.size() > MAX_UNDO_SIZE) {
        undos.remove(0);
        undoPos--;
      }
      JFLog.log("undoSize=" + undos.size() + ",undoPos=" + undoPos);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void clearUndo() {
    undoPos = 0;
    undos.clear();
  }
  public void undo() {
    if (undoPos <= 0) return;
//    JFLog.log("undo");
    try {
      if (undoDirty) {
        //createRedo
        createUndo("redo");
        undoDirty = false;
        undoPos--;
      }
      undoPos--;
      Undo undo = undos.get(undoPos);
      int orgLayer = colorLayer;
      changeColorLayer(0);
      int w = undo.img.getWidth();
      int h = undo.img.getHeight();
      setImageSize(w, h);
      backClear();
      foreClear();
      int px[] = undo.img.getPixels();
      img[undo.imageLayer].putPixels(px, 0, 0, w, h, 0);
      resizeBorder();
      changeColorLayer(orgLayer);
      repaint();
      JFLog.log("undoSize=" + undos.size() + ",undoPos=" + undoPos);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void redo() {
    if (undos.isEmpty()) return;
    if (undoPos == undos.size()-1) return;
    if (undoDirty) {
      while(undos.size() > undoPos) {
        undos.remove(undos.size()-1);  //remove redos
      }
      return;
    }
//    JFLog.log("redo");
    try {
      undoPos++;
      Undo undo = undos.get(undoPos);
      int w = undo.img.getWidth();
      int h = undo.img.getHeight();
      setImageSize(w, h);
      backClear();
      foreClear();
      int px[] = undo.img.getPixels();
      img[undo.imageLayer].putPixels(px, 0, 0, w, h, 0);
      resizeBorder();
      repaint();
//      JFLog.log("undoSize=" + undos.size() + ",undoPos=" + undoPos);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void setDirty() {
    dirty = true;
    undoDirty = true;
  }

  public int getColorLayer() {
    return colorLayer;
  }

  public void changeColorLayer(int newLayer) {
    //merge current layer back to img
    applyColorLayer();
    colorLayer = newLayer;
    int px[];
    int w = getUnscaledWidth();
    int h = getUnscaledHeight();
    switch (colorLayer) {
      case 0:  //ARGB
        cimg = img[imageLayer];
        break;
      case 1:  //A---
        px = img[imageLayer].getAlphaLayer();
        limg.putPixels(px, 0, 0, w, h, 0);
        cimg = limg;
        break;
      case 2:  //-R--
        px = img[imageLayer].getLayer(0x00ff0000);
        limg.putPixels(px, 0, 0, w, h, 0);
        cimg = limg;
        break;
      case 3:  //--G-
        px = img[imageLayer].getLayer(0x0000ff00);
        limg.putPixels(px, 0, 0, w, h, 0);
        cimg = limg;
        break;
      case 4:  //---B
        px = img[imageLayer].getLayer(0x000000ff);
        limg.putPixels(px, 0, 0, w, h, 0);
        cimg = limg;
        break;
    }
  }

  public void applyColorLayer() {
    int px[];
    switch (colorLayer) {
      case 0:  //ARGB : nothing to do
        break;
      case 1:  //A---
        px = limg.getBuffer();
        img[imageLayer].putAlphaLayer(px);
        break;
      case 2:  //-R--
        px = limg.getBuffer();
        img[imageLayer].putLayer(px, 0x00ff0000);
        break;
      case 3:  //--G-
        px = limg.getBuffer();
        img[imageLayer].putLayer(px, 0x0000ff00);
        break;
      case 4:  //---B
        px = limg.getBuffer();
        img[imageLayer].putLayer(px, 0x000000ff);
        break;
    }
  }

  public void blur(int x1,int y1,int x2,int y2,int radius) {
    int tmp;
    int width = img[imageLayer].getWidth(), height = img[imageLayer].getHeight();
    if (x1 < 0) x1 = 0;
    if (x2 < 0) x2 = 0;
    if (y1 < 0) y1 = 0;
    if (y2 < 0) y2 = 0;
    if (x1 >= width) x1 = width-1;
    if (x2 >= width) x2 = width-1;
    if (y1 >= height) y1 = height-1;
    if (y2 >= height) y2 = height-1;
    if (x1 > x2) {
      tmp = x1;
      x1 = x2;
      x2 = tmp;
    }
    if (y1 > y2) {
      tmp = y1;
      y1 = y2;
      y2 = tmp;
    }
    width = x2-x1+1;
    height = y2-y1+1;
    Kernel kernel = GaussianFilter.makeKernel(radius);
    int dst[] = new int[width * height];
    int inPixels[] = img[imageLayer].getPixels(x1,y1,width,height);
    int outPixels[] = dst;
    boolean alpha = false, premultiplyAlpha = false;
    if (radius > 0) {
      GaussianFilter.convolveAndTranspose(kernel, inPixels, outPixels, width, height, alpha, alpha && premultiplyAlpha, false, GaussianFilter.CLAMP_EDGES);
      GaussianFilter.convolveAndTranspose(kernel, outPixels, inPixels, height, width, alpha, false, alpha && premultiplyAlpha, GaussianFilter.CLAMP_EDGES);
    }
    img[imageLayer].putPixels(inPixels, x1, y1, width, height, 0);
  }

  public void pixelate(int x1,int y1,int x2,int y2,int pixelSize) {
    int tmp;
    int width = img[imageLayer].getWidth(), height = img[imageLayer].getHeight();
    if (x1 < 0) x1 = 0;
    if (x2 < 0) x2 = 0;
    if (y1 < 0) y1 = 0;
    if (y2 < 0) y2 = 0;
    if (x1 >= width) x1 = width-1;
    if (x2 >= width) x2 = width-1;
    if (y1 >= height) y1 = height-1;
    if (y2 >= height) y2 = height-1;
    if (x1 > x2) {
      tmp = x1;
      x1 = x2;
      x2 = tmp;
    }
    if (y1 > y2) {
      tmp = y1;
      y1 = y2;
      y2 = tmp;
    }
    width = x2-x1+1;
    height = y2-y1+1;
    int px[] = img[imageLayer].getPixels(x1,y1,width,height);
    BlockFilter filter = new BlockFilter(pixelSize);
    BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    src.setRGB(0, 0, width, height, px, 0, width);
    BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    filter.filter(src, dst);
    dst.getRGB(0, 0, width, height, px, 0, width);
    img[imageLayer].putPixels(px, x1, y1, width, height, 0);
  }

  public void chrome(int x1,int y1,int x2,int y2,float amount, float exposure) {
    int tmp;
    int width = img[imageLayer].getWidth(), height = img[imageLayer].getHeight();
    if (x1 < 0) x1 = 0;
    if (x2 < 0) x2 = 0;
    if (y1 < 0) y1 = 0;
    if (y2 < 0) y2 = 0;
    if (x1 >= width) x1 = width-1;
    if (x2 >= width) x2 = width-1;
    if (y1 >= height) y1 = height-1;
    if (y2 >= height) y2 = height-1;
    if (x1 > x2) {
      tmp = x1;
      x1 = x2;
      x2 = tmp;
    }
    if (y1 > y2) {
      tmp = y1;
      y1 = y2;
      y2 = tmp;
    }
    width = x2-x1+1;
    height = y2-y1+1;
    int px[] = img[imageLayer].getPixels(x1,y1,width,height);
    ChromeFilter filter = new ChromeFilter();
    filter.setAmount(amount);
    filter.setExposure(exposure);
    BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    src.setRGB(0, 0, width, height, px, 0, width);
    BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    filter.filter(src, dst);
    dst.getRGB(0, 0, width, height, px, 0, width);
    img[imageLayer].putPixels(px, x1, y1, width, height, 0);
  }

  public int getImageLayers() {
    return imageLayers;
  }

  private String uniqueName() {
    int n = imageLayers;
    do {
      String str = "Layer " + n;
      boolean dup = false;
      for(int a=0;a<imageLayers;a++) {
        if (name[a].equals(str)) {
          dup = true;
          break;
        }
      }
      if (dup) {
        n++;
        continue;
      } else {
        return str;
      }
    } while (true);
  }

  public void addImageLayer() {
    img = Arrays.copyOf(img, imageLayers+1);
    img[imageLayers] = new JFImage(getUnscaledWidth(), getUnscaledHeight());
    //fills with back color but 100% transparent
    img[imageLayers].fill(0, 0, getUnscaledWidth(), getUnscaledHeight(), mainPanel.backClr, true);
    show = Arrays.copyOf(show, imageLayers+1);
    show[imageLayers] = true;
    name = Arrays.copyOf(name, imageLayers+1);
    name[imageLayers] = uniqueName();
    imageLayers++;
  }

  public void swapLayers(int i1, int i2) {
    if (i1 == i2) return;
    JFImage tmp = img[i2];
    img[i2] = img[i1];
    img[i1] = tmp;
    boolean tmp2 = show[i2];
    show[i2] = show[i1];
    show[i1] = tmp2;
    String tmp3 = name[i2];
    name[i2] = name[i1];
    name[i1] = tmp3;
  }

  public void removeImageLayer(int layer) {
    if (layer >= imageLayers) return;
    if (layer == 0 && imageLayers == 1) {
      //fills with back color (0% transparent)
      img[0].fill(0, 0, getUnscaledWidth(), getUnscaledHeight(), mainPanel.backClr);
      return;
    }
    img = (JFImage[])JF.copyOfExcluding(img, layer);
    show = JF.copyOfExcluding(show, layer);
    name = (String[])JF.copyOfExcluding(name, layer);
    imageLayers--;
    if (imageLayer >= imageLayers) imageLayer--;
    if (!show[imageLayer]) show[imageLayer] = true;
    changeColorLayer(colorLayer);
  }

  public void setImageLayer(int layer) {
    clearUndo();
    int orgColorLayer = colorLayer;
    if (orgColorLayer != 0) {
      changeColorLayer(0);
    }
    imageLayer = layer;
    changeColorLayer(orgColorLayer);
  }

  public int getImageLayer() {
    return imageLayer;
  }

  public JFImage combineImageLayers() {
    if (img.length == 1) return img[0];
    JFImage c = new JFImage();
    c.setSize(img[0].getWidth(), img[0].getHeight());
    Graphics g = c.getGraphics();
    for(int a=0;a<img.length;a++) {
      g.drawImage(img[a].getImage(), 0, 0, null);
    }
    return c;
  }

  public void mouseClicked(MouseEvent e) { mainPanel.mouseClicked(e); }
  public void mouseEntered(MouseEvent e) { mainPanel.mouseEntered(e); }
  public void mouseExited(MouseEvent e) { mainPanel.mouseExited(e); }
  public void mousePressed(MouseEvent e) { mainPanel.mousePressed(e); }
  public void mouseReleased(MouseEvent e) { mainPanel.mouseReleased(e); }

  public void mouseDragged(MouseEvent e) { mx = (int)(e.getX() / (scale / 100f)); my = (int)(e.getY() / (scale / 100f)); mainPanel.mouseDragged(e); }
  public void mouseMoved(MouseEvent e) { mx = (int)(e.getX() / (scale / 100f)); my = (int)(e.getY() / (scale / 100f)); mainPanel.mouseMoved(e); }

  public void keyPressed(KeyEvent e) {}
  public void keyReleased(KeyEvent e) {}
  public void keyTyped(KeyEvent e) { mainPanel.keyTypedOnImage(e.getKeyChar()); }
}
