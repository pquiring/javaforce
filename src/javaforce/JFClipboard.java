package javaforce;

/** Clipboard
 *
 * NOTE : in readFiles() and writeFiles() the Action is ignored (Java does not support it)
 *   The Action is only valid in DND unfortunately.
 *
 * @author pquiring
 *
 * Created : Oct 9, 2013
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;
import javax.swing.*;

public class JFClipboard {
  public static class ImageTransferable implements Transferable
  {
    private Image image;

    public ImageTransferable (Image image)
    {
      this.image = image;
    }

    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException
    {
      if (isDataFlavorSupported(flavor))
      {
        return image;
      }
      else
      {
        throw new UnsupportedFlavorException(flavor);
      }
    }

    public boolean isDataFlavorSupported (DataFlavor flavor)
    {
      return flavor == DataFlavor.imageFlavor;
    }

    public DataFlavor[] getTransferDataFlavors ()
    {
      return new DataFlavor[] { DataFlavor.imageFlavor };
    }
  }

  public static DataFlavor[] getDataFlavors() {
    return Toolkit.getDefaultToolkit().getSystemClipboard().getAvailableDataFlavors();
  }

  public static Image readImage()
  {
    Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

    try
    {
      if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor))
      {
        Image image = (Image)t.getTransferData(DataFlavor.imageFlavor);
        return image;
      }
    }
    catch (Exception e) {
      JFLog.log(e);
    }

    return null;
  }

  /** Converts unknown Image format to JFImage. */
  public static JFImage convertImage(Image image) {
    int x = image.getWidth(null);
    int y = image.getHeight(null);
    JFImage jfimg = new JFImage(x,y);
    jfimg.getGraphics().drawImage(image, 0, 0, null);
    return jfimg;
  }

  public static void writeImage(Image image)
  {
    ImageTransferable transferable = new ImageTransferable(image);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
  }

  public static String readString() {
    Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
    try
    {
      if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor))
      {
        String str = (String)t.getTransferData(DataFlavor.stringFlavor);
        return str;
      }
    }
    catch (Exception e) {
      JFLog.log(e);
    }

    return null;
  }

  public static void writeString(String str) {
    StringSelection ss = new StringSelection(str);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
  }

  public static class FileList {
    public java.util.List<File> files;
    public int action;
  }

  public static final int NONE = 0;
  public static final int COPY = 1;
  public static final int MOVE = 2;  //cut
  public static final int COPY_OR_MOVE = 3;

  public static boolean useAWT = false;  //does not support action types

  public static class JComp extends JComponent {}

  public static class MyTransferHandler extends TransferHandler {
    public java.util.List<File> files;
    public int action;
    public boolean canImport(TransferHandler.TransferSupport info) {
      JFLog.log("canImport");
      if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        return false;
      }

      TransferHandler.DropLocation dl = (TransferHandler.DropLocation) info.getDropLocation();
      Point pt = dl.getDropPoint();
      return true;
    }

    @SuppressWarnings("unchecked")
    public boolean importData(TransferHandler.TransferSupport info) {
      JFLog.log("importData");
      // Check for file flavor
      if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        return false;
      }

      // Get the file(s) that are being dropped.
      Transferable t = info.getTransferable();
      try {
        files = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
      } catch (Exception e) {
        return false;
      }

      action = NONE;
      return true;
    }

    public int getSourceActions(JComponent c) {
      JFLog.log("getSourceActions:" + action);
      return action;
    }

    protected Transferable createTransferable(JComponent c) {
      JFLog.log("createTransferable");
      return new Transferable() {
        public DataFlavor[] getTransferDataFlavors() {
          return new DataFlavor[] {DataFlavor.javaFileListFlavor};
        }

        public boolean isDataFlavorSupported(DataFlavor df) {
          return (df == DataFlavor.javaFileListFlavor);
        }

        public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException {
          return files;
        }
      };
    }

    protected void exportDone(JComponent source, Transferable data, int action) {
      JFLog.log("exportDone:action=" + action);
    }
  }

  @SuppressWarnings("unchecked")
  public static FileList readFiles() {
    FileList fl = new FileList();
    if (useAWT) {
      Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
      try
      {
        if (t != null && t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        {
          java.util.List<File> files = (java.util.List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);
          fl.files.addAll(files);
          fl.action = NONE;
          return fl;
        }
      }
      catch (Exception e) {
        JFLog.log(e);
      }
    } else {
      JComp c = new JComp();
      MyTransferHandler th = new MyTransferHandler();
      c.setTransferHandler(th);
      Action a = TransferHandler.getPasteAction();
      a.actionPerformed(new ActionEvent(c,0,"paste"));
      fl.files = th.files;
      fl.action = th.action;
      return fl;
    }
    return null;
  }

  public static void writeFiles(final FileList fl) {
    if (useAWT) {
      Transferable t = new Transferable() {
        public DataFlavor[] getTransferDataFlavors() {
          return new DataFlavor[] {DataFlavor.javaFileListFlavor};
        }

        public boolean isDataFlavorSupported(DataFlavor df) {
          return (df == DataFlavor.javaFileListFlavor);
        }

        public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException {
          return fl.files;
        }
      };
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
    } else {
      //use swing
      if (fl.action != COPY && fl.action != MOVE) return;
      JComp c = new JComp();
      MyTransferHandler th = new MyTransferHandler();
      th.files = fl.files;
      th.action = fl.action;
      c.setTransferHandler(th);
      Action a;
      if (fl.action == COPY)
        a = TransferHandler.getCopyAction();
      else
        a = TransferHandler.getCutAction();
      a.actionPerformed(new ActionEvent(c,0,fl.action == COPY ? "copy" : "cut"));
    }
  }

  public static void clearClipboard() {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(null, null);
  }
};
