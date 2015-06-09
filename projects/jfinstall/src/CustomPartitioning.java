/**
 *
 * Created : Feb 18, 2012
 *
 * @author pquiring
 */

import jfparted.*;

import javaforce.*;

public class CustomPartitioning extends PartitionEditorPanel {

  /**
   * Creates new form Partition
   */
  public CustomPartitioning(boolean mountNow) {
    super(mountNow);
    Data.guidedTarget = null;
  }

  public IPanel next() {
    Data.root = null;
    Data.swap = null;
    Data.clearfstab();

    //generate fstab records
    for(int d=0;d<Data.devices.size();d++) {
      Data.Device device = Data.devices.get(d);
      for(int p=0;p<device.parts.size();p++) {
        Data.Partition part = device.parts.get(p);
        if (part.number != -1) {
          if (part.mount.length() == 0) continue;
          if (part.mount.equals("/")) {
            Data.root = part;
            Data.addfstab(part.device.dev + part.number, part.mount, part.filesys,
              "errors=remount-ro", 0, 1);
          } else if (part.mount.equals("swap")) {
            Data.swap = part;
            part.mount = "none";
            Data.addfstab(part.device.dev + part.number, part.mount, part.filesys,
              "sw", 0, 0);
          } else {
            Data.addfstab(part.device.dev + part.number, part.mount, part.filesys,
              "rw", 0, 0);
          }
        }
      }
    }

    if (Data.root == null) {
      JF.showError("Error", "You must define a root partition.");
      return null;
    }
    if (Data.swap == null) {
      if (!JF.showConfirm("Warning", "You have not defined a swap partition.\nAre you sure you do NOT want one?")) return null;
    }
    return new Install();
  }
  public IPanel prev() {return new InstallTypes();}
}
