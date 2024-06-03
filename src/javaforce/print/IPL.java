package javaforce.print;

/** Intermec Printing Language (IPL)
 *
 * v1.0
 *
 * @author pquiring
 * @date July 13, 2020
 */

import java.util.*;
import java.io.*;
import java.net.*;

import javaforce.*;

public class IPL {
  private StringBuilder sb = new StringBuilder();
  private int field;  //max 400 fields
  private boolean done;
  private ArrayList<String> fields = new ArrayList<String>();
  private boolean bin = true;
  private int program;

  private void start() {
    if (bin) {
      sb.append((char)0x02);
    } else {
      sb.append("<STX>");
    }
  }
  private void end() {
    if (bin) {
      sb.append((char)0x03);
    } else {
      sb.append("<ETX>");
    }
    sb.append("\r\r\n");
  }
  private void escape() {
    if (bin) {
      sb.append((char)0x1b);
    } else {
      sb.append("<ESC>");
    }
  }
  private void formfeed() {
    if (bin) {
      sb.append((char)0x0c);
    } else {
      sb.append("<FF>");
    }
  }
  private void can() {
    //clear all data
    if (bin) {
      sb.append((char)0x18);
    } else {
      sb.append("<CAN>");
    }
  }
  private void etb() {
    //End Transmission Block
    if (bin) {
      sb.append((char)0x17);
    } else {
      sb.append("<ETB>");
    }
  }
  private void rs() {
    //Record Separator
    if (bin) {
      sb.append((char)0x1e);
    } else {
      sb.append("<RS>");
    }
  }
  private void lf() {
    //linefeed \n
    if (bin) {
      sb.append((char)0x0a);
    } else {
      sb.append("<LF>");
    }
  }
  private void cr() {
    //carriage return \r
    if (bin) {
      sb.append((char)0x0d);
    } else {
      sb.append("<CR>");
    }
  }

  //<ESC>c=emulation mode (101 dpi)
  //<ESC>C=adv mode (203 dpi)
  //<ESC>C1=adv mode (300 dpi)
  //<ESC>P=programming mode
  //E#=erase program
  //F#=edit program
  //R=return to print mode
  //<ESC>E# = use program
  //<ESC>F#<LF>{text} = field select

  /** Creates an IPL file.
   * @param program=1-99
   */
  public IPL(int program) {
    this.program = program;
    start();
    escape();
    sb.append("c");
    end();
    start();
    escape();
    sb.append("P");
    end();
    start();
    sb.append("E" + program + ";F" + program);
    end();
  }

  //o=x,y = origin
  //f? = zero rotation (0=none 1=90 2=180 3=270 degrees)
  //c? = font/barcode type
  //d0,length = data included later (max length)
  //l,w,h = length,width,height
  //p{text} = prefix barcode with text

  /** Adds standard Bar Code (code 39).
   * Usually scaleX/scaleY are the same (try 1,1 or 2,2)
   */
  public void addBarCode(int x, int y, int scaleX, int scaleY, String code) {
    if (done) return;
    String str = String.format("B%d;o%d,%d;f0;h%d;w%d;r0;i0;c0,0;d3,%s;", field++, x, y, scaleY, scaleX, code);
    start();
    sb.append(str);
    end();
    fields.add(code);
  }

  /** Adds QR Code.
   * version = size of QR code (how many Alphanumeric codes max it can hold) (ECC level=H) (ie:V4=50, V10=174, V25=1852) (see https://www.qrcode.com/en/about/version.html)
   * scaleXY = scales QR in X/Y direction (try 1,2,3,4,5,...)
   */
  public void addQRCode(int x, int y, int version, int scaleXY, String code) {
    if (done) return;
    String str = String.format("B%d;o%d,%d;f0;h%d;w%d;r0;i0;c18,0;d3,%s;", field++, x, y, version, scaleXY, code);
    start();
    sb.append(str);
    end();
    fields.add(code);
  }

  public void addText(int x, int y, int scaleX, int scaleY, String text) {
    if (done) return;
    String str = String.format("H%d;o%d,%d;f0;h%d;w%d;c0;d3,%s;", field++, x, y, scaleY, scaleX, text);
    start();
    sb.append(str);
    end();
    fields.add(text);
  }

  public void addLine(int x, int y, int width, int height) {
    if (done) return;
    String str = String.format("L%d;o%d,%d;f0;l%d;w%d;", field++, x, y, width, height);
    start();
    sb.append(str);
    end();
    fields.add("-@-");
  }

  public void addBox(int x, int y, int width, int height, int lineWidth) {
    if (done) return;
    String str = String.format("W%d;o%d,%d;f0;l%d;h%d;w%d;", field++, x, y, width, height, lineWidth);
    start();
    sb.append(str);
    end();
    fields.add("-@-");
  }

  public void finish() {
    if (done) return;
    start();
    sb.append("R");
    end();
    start();
    escape();
    sb.append("E" + program);
    end();
    start();
    can();  //clear all fields
    end();
//    field = 0;
    //submit field data (d0)
    for(int f=0;f<fields.size();f++) {
      String str = fields.get(f);
      if (str.equals("-@-")) continue;
      start();
//      escape();
//      sb.append("F" + field++);
//      lf();
      sb.append(str);
      cr();
      end();
    }
    start();
    etb();
    formfeed();
    end();
    done = true;
  }

  public String generate() {
    if (!done) finish();
    return sb.toString();
  }

  public void print(String ip) {
    if (!done) finish();
    try {
      Socket s = new Socket(ip, 9100);
      String str = generate();
      s.getOutputStream().write(str.getBytes());
      s.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
