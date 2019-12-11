/** RestoreJob
 *
 * @author pquiring
 */

//TODO : when restore in place is implemented a seperate thread would be required

import java.util.*;
import java.io.*;

import javaforce.*;

public class RestoreJob extends Thread {
  private long restoreid;
  private MediaChanger changer = new MediaChanger();
  private TapeDrive tape = new TapeDrive();
  private boolean haveChanger;
  private Element elements[];
  private int driveIdx = -1;
  private int emptySlotIdx = -1;
  private int desiredSlotIdx = -1;
  private RestoreInfo info;
  private Catalog cat;
  private CatalogInfo catInfo;
  private ArrayList<String> tapes = new ArrayList();
  private EntryVolume currentVolume;
  private EntryFolder currentFolder;
  private EntryTape currentTape;
  private EntryTape desiredTape;

  public RestoreJob(Catalog cat, CatalogInfo catInfo, RestoreInfo info) {
    this.cat = cat;
    this.catInfo = catInfo;
    this.info = info;
  }
  public void run() {
    try {
      if (!doRestore()) throw new Exception("Restore failed");
      log("Restored files to " + Config.current.restorePath);
      log("Restore complete");
      Status.running = false;
      Status.abort = false;
      Status.desc = "Restore Complete";
    } catch(Exception e) {
      String msg = e.getMessage();
      if (msg == null || !msg.equals("Restore failed")) {
        log(e);
      }
      log("Restore failed");
      Status.running = false;
      Status.abort = false;
      Status.desc = "Restore aborted, see logs.";
    }
    JFLog.close(3);
    tape.close();
    if (haveChanger) {
      changer.close();
    }
    Status.job = null;
  }
  public boolean doRestore() {
    //assign a unique backup id
    restoreid = System.currentTimeMillis();
    //do we have a media changer?
    haveChanger = Config.current.changerDevice.length() > 0;
    //create a log file
    JFLog.init(3, Paths.logsPath + "/restore-" + restoreid + ".log", true);
    log("Restore job started at " + ConfigService.toDateTime(restoreid));
    //open devices
    if (!tape.open(Config.current.tapeDevice)) {
      log("Error:Failed to open tape device");
      return false;
    }
    if (haveChanger) {
      if (!changer.open(Config.current.changerDevice)) {
        log("Error:Failed to open changer device");
        return false;
      }
    }
    if (haveChanger != cat.haveChanger) {
      log("Error:Media changer mismatch");
      return false;
    }
    if (haveChanger) {
      if (!checkTapesPresent()) return false;
    } else {
      if (!verifyHeader()) return false;
    }
    for(EntryVolume vol : cat.volumes) {
      currentVolume = vol;
      if (!doFolder(vol.root, isVolumeSelected(vol), Config.current.restorePath + "\\" + vol.host + "_" + vol.volume.replace(':', '_'))) return false;
    }
    return true;
  }
  private void log(Exception e) {
    JFLog.log(3, e);
  }
  private void log(String msg) {
    JFLog.log(3, msg);
    Status.log.append(msg + "\r\n");
  }
  private boolean doFolder(EntryFolder folder, boolean restore, String path) {
    currentFolder = folder;
    for(EntryFolder childFolder : folder.folders) {
      doFolder(childFolder, restore ? true : isFolderSelected(childFolder), path + "\\" + childFolder.name);
    }
    for(EntryFile file : folder.files) {
      if (restore || isFileSelected(file)) {
        if (!doFile(file, path)) return false;
      }
    }
    return true;
  }
  private boolean doFile(EntryFile file, String path) {
    if (haveChanger) {
      if (currentTape == null || currentTape.number != file.t) {
        if (!loadTape(file.t - 1)) return false;
        if (!verifyHeader()) return false;
      }
    }
    if (!setpos(file.o)) {
      log("Error:Failed to set tape position:" + file.o);
      return false;
    }
    long tapepos = getpos();
    if (tapepos != file.o) {
      log("Error:Failed to get tape position:Expected:" + file.o + " Returned:" + tapepos);
      return false;
    }
    new File(path).mkdirs();
    String resfile = path + "\\" + file.name;
    try {
      FileOutputStream fos = new FileOutputStream(resfile);
      PipedInputStream pis = new PipedInputStream();
      PipedOutputStream pos = new PipedOutputStream(pis);
      Transfer transfer = new Transfer(pos, file.c);
      transfer.start();
      long uncompressed = Compression.decompress(pis, fos, file.c);
      transfer.join();
      if (uncompressed != file.u) return false;
    } catch (Exception e) {
      log(e);
      return false;
    }
    return true;
  }
  private static byte buffer[] = new byte[64 * 1024];
  private static final int buffersize = 64 * 1024;
  public class Transfer extends Thread {
    private OutputStream os;
    private boolean active = true;
    public long copied;
    public long compressed;
    public Transfer(OutputStream os, long compressed) {
      this.os = os;
      this.compressed = compressed;
    }
    public void run() {
      long left = compressed;
      try {
        while (active) {
          if (copied == compressed) break;
          int toread = left < buffersize ? (int)left : buffersize;
          int read = tape.read(buffer, 0, toread);
          if (read == -1) break;
          if (read == 0) continue;
          os.write(buffer, 0, read);
          copied += read;
          left -= read;
        }
        os.flush();
        os.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
  private boolean isVolumeSelected(EntryVolume volume) {
    return info.restoreVolumes.contains(volume);
  }
  private boolean isFolderSelected(EntryFolder folder) {
    return info.restoreFolders.contains(folder);
  }
  private boolean isFileSelected(EntryFile file) {
    return info.restoreFiles.contains(file);
  }
  private boolean loadTape(int index) {
    if (index < 0 || index >= catInfo.tapes.size()) {
      log("Error:Invalid tape number " + (index+1));
      return false;
    }
    desiredTape = catInfo.tapes.get(index);
    if (currentTape == desiredTape) return true;
    //move this tape into position
    if (!updateList()) return false;
    if (elements[driveIdx].barcode.equals(desiredTape.barcode)) {
      //desired tape in drive
      currentTape = desiredTape;
      desiredTape = null;
      log("Using tape:" + currentTape.barcode);
      return true;
    }
    if (elements[driveIdx].barcode.equals("<empty>")) {
      //move tape out of drive
      if (emptySlotIdx == -1) {
        log("Error:No empty slot to move tape from drive");
        return false;
      }
      log("Move Tape:" + elements[driveIdx].name + " to " + elements[emptySlotIdx].name);
      if (!changer.move(elements[driveIdx].name, elements[emptySlotIdx].name)) {
        log("Error:failed to move tape out of drive");
        return false;
      }
      return loadTape(index);
    }
    //move desired tape into drive
    log("Move Tape:" + elements[desiredSlotIdx].name + " to " + elements[driveIdx].name);
    if (!changer.move(elements[desiredSlotIdx].name, elements[driveIdx].name)) {
      log("Error:failed to move tape into drive");
      return false;
    }
    return loadTape(index);
  }
  private boolean checkTapesPresent() {
    updateList();
    boolean needTape = false;
    for(EntryTape tape : catInfo.tapes) {
      boolean found = false;
      for(Element element : elements) {
        if (element.barcode.equals(tape.barcode)) {
          found = true;
          break;
        }
      }
      if (!found) {
        needTape = true;
        log("Please insert tape:" + tape.barcode);
      }
    }
    return !needTape;
  }
  private boolean updateList() {
    elements = changer.list();
    if (elements == null) return false;
    driveIdx = -1;
    emptySlotIdx = -1;
    desiredSlotIdx = -1;
    for(int idx=0;idx<elements.length;idx++) {
      if (driveIdx == -1 && elements[idx].name.startsWith("drive")) {
        driveIdx = idx;
      }
      if (emptySlotIdx == -1 && elements[idx].name.startsWith("slot") && elements[idx].barcode.equals("<empty>")) {
        emptySlotIdx = idx;
      }
      if (desiredSlotIdx == -1 && desiredTape != null && elements[idx].barcode.equals(desiredTape.barcode)) {
        desiredSlotIdx = idx;
      }
    }
    if (driveIdx == -1) {
      log("Error:Unable to find drive in changer");
      return false;
    }
    if (desiredTape != null && desiredSlotIdx == -1) {
      log("Error:Unable to find desired tape:" + desiredTape.barcode);
      return false;
    }
    return true;
  }
/**
 * Each tape has a one block header stored in block 0.
 * Header = "jfBackup;version=V.V;timestamp=S;tape=T;blocksize=K;barcode=B" (zero filled to 64k)
 *   V.V = version of jfBackup used to create backup
 *   T = tape # (in multi-tape backup) (1 based)
 *   S = timestamp of backup (same on all tapes of multi-tape backup)
 *   K = blocksize (default = 65536)
 *   B = barcode of tape (if available)
 */
  private boolean verifyHeader() {
    log("Verify tape:" + currentTape.barcode);
    if (!setpos(0)) {
      log("Error:Unable to rewind tape");
      return false;
    }
    long pos = getpos();
    if (pos != 0) {
      log("Error:Failed to rewind tape");
      return false;
    }
    byte data[] = new byte[64 * 1024];
    if (tape.read(data, 0, data.length) != data.length) {
      log("Error:Failed to read tape header");
      return false;
    }
    try {
      int idx = 0;
      while (data[idx] != ' ' && idx < data.length) idx++;
      if (idx == data.length) throw new Exception("Invalid header on tape");
      String header = new String(data, 0, idx);
      String fs[] = header.split(";");
      //compare to currentTape
      for(String f : fs) {
        idx = f.indexOf("=");
        if (idx == -1) continue;
        String key = f.substring(0, idx);
        String value = f.substring(idx + 1);
        switch (key) {
          case "timestamp": {
            long tapeBackup = Long.valueOf(value);
            if (currentTape.backup != tapeBackup) {
              throw new Exception("Invalid header on tape, backup id mismatch, expected:" + currentTape.backup + " found:" + tapeBackup);
            }
            break;
          }
          case "barcode": {
            String tapeBarcode = value;
            if (!currentTape.barcode.equals(tapeBarcode)) {
              throw new Exception("Invalid header on tape, barcode mismatch, expected:" + currentTape.barcode + " found:" + tapeBarcode);
            }
            break;
          }
          case "tape": {
            int tapeNumber = Integer.valueOf(value);
            if (currentTape.number != tapeNumber) {
              throw new Exception("Invalid header on tape, tape number mismatch, expected:" + currentTape.number + " found:" + tapeNumber);
            }
            break;
          }
        }
      }
    } catch (Exception e) {
      log("Error:" + e.toString());
      return false;
    }
    return true;
  }
  private boolean setpos(long pos) {
    //try 3 times
    for(int a=0;a<3;a++) {
      if (tape.setpos(pos, a+1)) return true;
      JF.sleep(60 * 1000);  //wait 1 min
    }
    return false;
  }
  private long getpos() {
    //try 3 times
    for(int a=0;a<3;a++) {
      long pos = tape.getpos(a+1);
      if (pos >= 0) return pos;
      JF.sleep(60 * 1000);  //wait 1 min
    }
    return -1;
  }
}
