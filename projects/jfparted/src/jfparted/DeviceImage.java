package jfparted;

/**Image shows a devices partitions.
 *
 * Created : Feb 21, 2012
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;

import javaforce.*;
import javaforce.awt.*;

public class DeviceImage extends JFImage {
  private Data.Device device;
  public DeviceImage(Data.Device device) {
    this.device = device;
  }
  public void paint(Graphics dest) {
//    System.out.println("paint" + this.getWidth() + new java.util.Random().nextInt());
    int devicewidth = getWidth();
    setImageSize(devicewidth, 32);
    Graphics2D g = getGraphics2D();
    g.setColor(new Color(0xffffff));
    g.fillRect(0,0,devicewidth,32);
    g.setStroke(new BasicStroke(4));
    g.setColor(new Color(0x000000));
    g.drawRect(0,0,devicewidth,32);
    double devicesize = Data.getSize(device.size);
//    System.out.println("device width=" + devicewidth);
//    System.out.println("device size=" + devicesize);
    int xpos = 0;
    int partwidth;
    FontRenderContext frc = g.getFontRenderContext();
    Font font = g.getFont();
    for(int p=0;p<device.parts.size();p++) {
      Data.Partition part = device.parts.get(p);
      double partsize = Data.getSize(part.size);
//      System.out.println("part size=" + partsize);
      partwidth = (int)(partsize / devicesize * ((double)devicewidth));
//      System.out.println("part width=" + partwidth);
      if (xpos + partwidth >= devicewidth) partwidth = devicewidth - xpos;
      g.drawRect(xpos,0,xpos + partwidth,32);
      if (part.number != -1) {
        String name = part.device.dev + part.number;
        Rectangle2D rect = font.getStringBounds(name, frc);
        int fx = (int)rect.getWidth();
        int fy = (int)rect.getHeight();
        if (fx <= partwidth) {
          g.drawString(name, xpos + (partwidth - fx)/2, (32-fy)/2 + fy);
        }
      }
      xpos += partwidth;
    }
    super.paint(dest);
  }
}
