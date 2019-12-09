/** BackupJob
 *
 * Performs the actual backup to tapes.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javaforce.*;

public class BackupJob extends Thread {
  private EntryJob job;
  private final static boolean verbose = false;
  //TODO : support other blocksize (are there others?)
  private final static int blocksize = 64 * 1024;
  private long backupid;
  private long retentionid;
  private MediaChanger changer = new MediaChanger();
  private TapeDrive tape = new TapeDrive();
  private Catalog cat = new Catalog();
  private CatalogInfo catnfo = new CatalogInfo();
  private EntryTape currentTape;
  private EntryJobVolume jobvol;
  private boolean haveChanger;
  private Element elements[];
  private int driveIdx = -1;
  private int emptySlotIdx = -1;
  private String barcode;
  private LinkedBlockingQueue<EntryFile> queue = new LinkedBlockingQueue<EntryFile>(16);
  private long queueSize = 0;
  private final static long maxQueueSize = 16 * 1024 * 1024 * 1024;  //16 GBs
  private Object qsLock = new Object();
  private FileReader reader = new FileReader();
  private int volCount = 1;

  public BackupJob(EntryJob job) {
    this.job = job;
  }
  public void run() {
    try {
      if (!doBackup()) throw new Exception("Backup failed");
      //save tapes to library
      Tapes.current.tapes.addAll(catnfo.tapes);
      Tapes.save();
      log("Saving catalog...");
      catnfo.name = job.name;
      catnfo.save();
      catnfo = null;
      cat.haveChanger = haveChanger;
      cat.save();
      cat = null;
      log("Backup complete at " + ConfigService.toDateTime(System.currentTimeMillis()));
      Status.running = false;
      Status.abort = false;
      Status.desc = "Backup Complete";
    } catch (Exception e) {
      if (e.getMessage().equals("Backup failed")) {
        log(e.toString());
      }
      log("Backup failed at " + ConfigService.toDateTime(System.currentTimeMillis()));
      Status.running = false;
      Status.abort = false;
      Status.desc = "Backup aborted, see logs.";
    }
    Status.job = null;
    JFLog.close(3);
  }
  private boolean doBackup() {
    //assign a unique backup id
    backupid = System.currentTimeMillis();
    //calc retention
    retentionid = calcRetention();
    //do we have a media changer?
    haveChanger = Config.current.changerDevice.length() > 0;
    //reset local file index
    ServerClient.resetLocalIndex();
    //create a log file
    JFLog.init(3, Paths.logsPath + "/backup-" + backupid + ".log", true);
    log("Backup job " + job.name + " started at " + ConfigService.toDateTime(backupid));
    //verify all remote hosts are connected
    log("Verifying remote hosts...");
    for(EntryJobVolume jobvol : job.backup) {
      if (Status.abort) return false;
      ServerClient serverclient = BackupService.server.getClient(jobvol.host);
      if (serverclient == null) {
        log("Error:Host offline:" + jobvol.host);
      }
      //TODO : ping client ?
    }
    //delete old backup jobs based on retention timestamp
    Catalog.deleteOld(backupid);
    //delete old tapes based on retention timestamp
    Tapes.removeOldTapes(backupid);
    //save backup id
    cat.backup = backupid;
    catnfo.backup = backupid;
    catnfo.retention = retentionid;
    DriveInfo info = tape.getDriveInfo();
    if (info == null) {
      log("Error:unable to get drive parameters");
      return false;
    }
    if (info.defaultBlockSize != blocksize) {
      log("Error:drive block size not supported:" + info.defaultBlockSize);
      log("Please open support ticket at jfbackup.sf.net (provide tape drive info)");
      return false;
    }
    if (!haveChanger) {
      //prep current tape in drive
      if (!prepNextTape()) return false;
    }
    reader.start();
    while (Status.active) {
      EntryFile file = get();
      if (file == null) {
        log("Error:no more files?");
        return false;
      }
      if (file.name.equals(":end:")) {
        break;
      }
      if (file.name.equals(":abort:")) {
        return false;
      }
      if (currentTape == null || currentTape.left < file.b) {
        if (!haveChanger) {
          log("Error:Tape full (no media changer)");
          return false;
        }
        if (!prepNextTape()) {
          return false;
        }
      }
      if (Status.abort) return false;
      if (!writeFile(file)) return false;
      deleteLocalFile(file);
      Status.copied += file.u;
      Status.files++;
      catnfo.files++;
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
  private EntryFile get() {
    EntryFile file;
    try {
      file = queue.take();
    } catch (Exception e) {
      log("Error:queue empty?");
      return null;
    }
    synchronized(qsLock) {
      queueSize -= file.c;
    }
    return file;
  }
  private boolean put(EntryFile file) {
    synchronized(qsLock) {
      queueSize += file.c;
    }
    try {
      queue.put(file);
    } catch (Exception e) {
      log("Error:queue full?");
      return false;
    }
    return true;
  }
  private boolean doFolder(EntryFolder parent, String path) {
    if (parent == null) return false;
    for(EntryFile file : parent.files) {
      if (Status.abort) return false;
      if (!doFile(file, path)) {
        deleteLocalFile(file);
        return false;
      }
    }
    for(EntryFolder child : parent.folders) {
      if (Status.abort) return false;
      if (!doFolder(child, path + "\\" + child.name)) return false;
    }
    return true;
  }
  private boolean doFile(EntryFile file, String path) {
    while (queueSize > maxQueueSize) {
      JF.sleep(100);
    }
    if (!readFile(file, path)) return false;
    return true;
  }
  private boolean prepNextTape() {
    //look for an empty tape
    if (haveChanger) {
      if (!loadEmptyTape()) return false;
    }
    //rewind tape
    if (!setpos(0)) {
      return false;
    }
    long pos = getpos();
    if (pos != 0) {
      log("Error:Tape rewind failed");
      return false;
    }
    currentTape = new EntryTape(barcode, backupid, retentionid, job.name, catnfo.tapes.size() + 1);
    //get tape info
    MediaInfo mediainfo = getMediaInfo();
    if (mediainfo == null) {
      log("Error:Tape get Media Info failed");
      return false;
    }
    currentTape.capacity = mediainfo.capacity / blocksize;
    currentTape.left = mediainfo.capacity / blocksize;
    if (mediainfo.readonly) {
      log("Error:Tape is read only");
      return false;
    }
    //add tape to catalog
    catnfo.tapes.add(currentTape);

    //write header
    writeHeader();
    pos = getpos();
    if (pos != 1) {
      log("Error:Tape write header failed");
      return false;
    }
    return true;
  }
  private boolean loadEmptyTape() {
    if (!updateList()) return false;
    for(int idx=0;idx<elements.length;idx++) {
      if (elements[idx].barcode.equals("<empty>")) continue;
      //check if tape is protected
      EntryTape tape = Tapes.findTape(elements[idx].barcode);
      if (tape != null) continue;
      //tape is not protected, use it
      if (elements[idx].name.startsWith("drive")) {
        //tape in drive is useable
        barcode = elements[idx].barcode;
        log("Using tape:" + barcode);
        return true;
      }
      //move this tape to drive
      //is drive full?
      if (!elements[driveIdx].barcode.equals("<empty>")) {
        //move tape out of drive to empty slot
        if (emptySlotIdx == -1) {
          log("Error:No empty slot to move tapes");
          return false;
        }
        log("Move Tape:" + elements[driveIdx].name + " to " + elements[emptySlotIdx].name);
        if (!changer.move(elements[driveIdx].name, elements[emptySlotIdx].name)) {
          log("Error:Move Tape failed:" + elements[driveIdx].name + " to " + elements[emptySlotIdx].name);
          return false;
        }
        return loadEmptyTape();
      }
      //move desired tape to drive
      log("Move Tape:" + elements[idx].name + " to " + elements[driveIdx].name);
      if (!changer.move(elements[idx].name, elements[driveIdx].name)) {
        log("Error:Move Tape failed:" + elements[idx].name + " to " + elements[driveIdx].name);
        return false;
      }
      return loadEmptyTape();
    }
    return false;
  }
  private boolean updateList() {
    elements = changer.list();
    if (elements == null) return false;
    driveIdx = -1;
    emptySlotIdx = -1;
    for(int idx=0;idx<elements.length;idx++) {
      if (driveIdx == -1 && elements[idx].name.startsWith("drive")) {
        driveIdx = idx;
      }
      if (emptySlotIdx == -1 && elements[idx].name.startsWith("slot") && elements[idx].barcode.equals("<empty>")) {
        emptySlotIdx = idx;
      }
    }
    if (driveIdx == -1) {
      log("Error:Unable to find drive in changer");
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
  private MediaInfo getMediaInfo() {
    //try 3 times
    for(int a=0;a<3;a++) {
      MediaInfo info = tape.getMediaInfo();
      if (info != null) return info;
      JF.sleep(60 * 1000);  //wait 1 min
    }
    return null;
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
  private boolean writeHeader() {
    StringBuilder sb = new StringBuilder();
    // Header = "jfBackup;version=V.V;timestamp=S;tape=T;blocksize=K;barcode=B" (zero filled to 64k)
    sb.append("jfBackup");
    sb.append(";version=" + Config.AppVersion);
    sb.append(";timestamp=" + backupid);
    sb.append(";tape=" + catnfo.tapes.size());
    sb.append(";blocksize=" + blocksize);
    sb.append(";barcode=" + barcode);

    byte data[] = new byte[blocksize];
    Arrays.fill(data, (byte)' ');
    byte src[] = sb.toString().getBytes();
    System.arraycopy(src, 0, data, 0, src.length);

    String file = Paths.tempPath + "/header.dat";

    log("WriteHeader to tape:" + barcode);

    try {
      FileOutputStream fos = new FileOutputStream(file);
      fos.write(data);
      fos.close();
    } catch (Exception e) {
      log(e.toString());
      return false;
    }

    if (!tape.write(file)) {
      log("Error:Tape write failed");
      return false;
    }

    new File(file).delete();

    currentTape.left--;
    currentTape.position = 1;

    return true;
  }
  private long calcRetention() {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(backupid);
    c.add(Calendar.YEAR, Config.current.retention_years);
    c.add(Calendar.MONTH, Config.current.retention_months);
    return c.getTimeInMillis();
  }
  private ServerClient getClient(EntryJobVolume jobvol) {
    ServerClient client = BackupService.server.getClient(jobvol.host);
    if (client == null) {
      log("Error:Client disconnected:" + jobvol.host);
    }
    return client;
  }
  private String reply;
  private boolean mountVolume(EntryJobVolume jobvol) {
    log("Mount:" + jobvol.host + ":" + jobvol.volume);
    ServerClient client = getClient(jobvol);
    if (client == null) return false;
    Object lock = new Object();
    reply = null;
    synchronized(lock) {
      client.addRequest(new Request("mount", jobvol.volume), (Request req) -> {
        reply = req.reply;
        synchronized(lock) {
          lock.notify();
        }
      });
      while (reply == null) {
        try {lock.wait();} catch (Exception e) {}
      }
    }
    return reply.equals("OKAY");
  }
  private boolean unmountVolume(EntryJobVolume jobvol) {
    log("Unmount:" + jobvol.host + ":" + jobvol.volume);
    ServerClient client = getClient(jobvol);
    Object lock = new Object();
    reply = null;
    synchronized(lock) {
      client.addRequest(new Request("unmount", jobvol.volume), (Request req) -> {
        reply = req.reply;
        synchronized(lock) {
          lock.notify();
        }
      });
      while (reply == null) {
        try {lock.wait();} catch (Exception e) {}
      }
    }
    return reply.equals("OKAY");
  }
  private EntryFolder listFolder(EntryJobVolume jobvol, String path) {
    if (verbose) log("ListFolder:" + jobvol.host + "\\" + path);
    ServerClient client = getClient(jobvol);
    Object lock = new Object();
    reply = null;
    synchronized(lock) {
      client.addRequest(new Request("listfolder", path), (Request req) -> {
        reply = req.reply;
        synchronized(lock) {
          lock.notify();
        }
      });
      while (reply == null) {
        try {lock.wait();} catch (Exception e) {}
      }
    }
    EntryFolder folder = new EntryFolder();
    if (reply.length() > 0) {
      String list[] = reply.split("\r\n");
      for(String ff : list) {
        if (Status.abort) return null;
        String f[] = ff.split("[|]");
        if (f[0].startsWith("\\")) {
          if (verbose) log("ListFolder:" + f[0]);
          EntryFolder subfolder = listFolder(jobvol, path + f[0]);
          if (subfolder == null) return null;
          subfolder.name = f[0].substring(1);
          folder.folders.add(subfolder);
        } else {
          if (f[0].length() == 0) continue;
          if (verbose) log("ListFile:" + f[0]);
          EntryFile file = new EntryFile();
          file.ct = 1;  //zip
          file.name = f[0];
          file.u = Long.valueOf(f[1]);
          folder.files.add(file);
        }
      }
    }
    return folder;
  }
  private boolean readFile(EntryFile file, String path) {
    ServerClient client = getClient(jobvol);
    Object lock = new Object();
    file.localfile = null;
    if (verbose) {
      log("ReadFile:" + file.name);
    }
    synchronized(lock) {
      client.addRequest(new Request("readfile", jobvol.path + path + "\\" + file.name), (Request req) -> {
        if (req.localfile == null) {
          file.localfile = ":abort:";
        } else {
          file.localfile = req.localfile;
          file.c = req.compressed;
        }
        synchronized(lock) {
          lock.notify();
        }
      });
      while (file.localfile == null) {
        try {lock.wait();} catch (Exception e) {}
      }
    }
    if (file.localfile.equals(":abort:")) {
      log("Error:Read file failed");
      return false;
    }
    long c = new File(file.localfile).length();
    if (c != file.c) {
      log("Error:temp file size error, disk full?");
      return false;
    }
    file.b = file.c / blocksize;
    if (file.c % blocksize > 0) {
      file.b++;
    }
    put(file);
    return true;
  }
  private boolean writeFile(EntryFile file) {
    file.o = currentTape.position;
    file.t = catnfo.tapes.size();
    currentTape.left -= file.b;
    currentTape.position += file.b;
    if (verbose) {
      File tmpfile = new File(file.localfile);
      log("writeFile:" + file.name + " blks=" + file.b + " compressed=" + file.c + " uncompressed=" + file.u + " tempfile.length=" + tmpfile.length());
    }
    return tape.write(file.localfile);
  }
  private void deleteLocalFile(EntryFile file) {
    if (file.localfile == null) return;
    new File(file.localfile).delete();
    file.localfile = null;
  }
  private class FileReader extends Thread {
    public void run() {
      if (!doReader()) {
        Status.abort = true;
        log("Error:File Reader aborted");
        EntryFile end = new EntryFile();
        end.name = ":abort:";
        put(end);
      }
    }
    public boolean doReader() {
      for(EntryJobVolume jobvol : job.backup) {
        if (jobvol.volume.length() == 0) {
          log("Warning:Skipping empty volume on host " + jobvol.host);
          continue;
        }
        if (Status.abort) return false;
        //mount/list volume on remote
        if (!mountVolume(jobvol)) {
          log("Error:mount failed");
          return false;
        }
        log("Mount successful");
        if (verbose) log("ListVolume:" + jobvol.host + "\\" + jobvol.volume);
        EntryFolder root = listFolder(jobvol, jobvol.path);
        if (root == null) {
          log("Error:ListVolume failed");
          return false;
        }
        if (verbose) log("ListVolume complete");
        root.name = "";
        //save to catalog
        EntryVolume volume = new EntryVolume();
        volume.host = jobvol.host;
        volume.root = root;
        volume.volume = jobvol.volume;
        volume.path = jobvol.path;
        volume.name = "vol-" + volCount++;
        cat.volumes.add(volume);
        BackupJob.this.jobvol = jobvol;
        //save files to tape
        boolean success = doFolder(root, "");
        //unmount
        if (!unmountVolume(jobvol)) {
          log("Error:Unable to unmount volume");
          return false;
        }
        log("Unmount successful");
        if (!success) return false;
      }
      EntryFile end = new EntryFile();
      end.name = ":end:";
      put(end);
      return true;
    }
  }
}
