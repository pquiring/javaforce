/** BackupJob
 *
 * Performs the actual backup to tapes.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class BackupJob extends Thread {
  private EntryJob job;
  //TODO : support other blocksize (are there others?)
  private final static int blocksize = 64 * 1024;
  private long backupid;
  private long retentionid;
  private MediaChanger changer = new MediaChanger();
  private TapeDrive tape = new TapeDrive();
  private Catalog cat = new Catalog();
  private EntryJobVolume currentjobvol;
  private EntryVolume currentvolume;
  private Element elements[];
  private int driveIdx = -1;
  private int emptySlotIdx = -1;
  private String barcode;
  private int volCount = 1;

  public static EntryTape currentTape;
  public boolean haveChanger;
  public CatalogInfo catnfo = new CatalogInfo();

  public BackupJob(EntryJob job) {
    this.job = job;
  }
  public void run() {
    currentTape = null;
    try {
      if (!doBackup()) throw new Exception("Backup failed");
      //save tapes to library
      Tapes.current.tapes.addAll(catnfo.tapes);
      Tapes.save();
      log("Saving catalog...");
      catnfo.name = job.name;
      catnfo.save();
      cat.haveChanger = haveChanger;
      cat.save();
      log("Backup complete at " + ConfigService.toDateTime(System.currentTimeMillis()));
      Status.running = false;
      Status.abort = false;
      Status.desc = "Backup Complete";
      email_notify(true);
      catnfo = null;
      cat = null;
    } catch (Exception e) {
      String msg = e.getMessage();
      if (msg == null || !msg.equals("Backup failed")) {
        log(e);
      }
      log("Backup failed at " + ConfigService.toDateTime(System.currentTimeMillis()));
      Status.running = false;
      Status.abort = false;
      Status.desc = "Backup aborted, see logs.";
      //clients may be deadlocked, drop carrier on them to reset them, they will reconnect in 3 seconds
      BackupService.server.dropClients();
      email_notify(false);
    }
    currentTape = null;
    tape.close();
    if (haveChanger) {
      changer.close();
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
    //create a log file
    JFLog.init(3, Paths.logsPath + "/backup-" + backupid + ".log", true);
    log("Backup job " + job.name + " started at " + ConfigService.toDateTime(backupid));
    log("Backup id = " + backupid);
    //open devices
    if (!tape.open(Config.current.tapeDevice)) {
      log("Error:Failed to open tape device" + ":Error=" + tape.lastError());
      return false;
    }
    if (haveChanger) {
      if (!changer.open(Config.current.changerDevice)) {
        log("Error:Failed to open changer device" + ":Error=" + changer.lastError());
        return false;
      }
    }
    //verify all remote hosts are connected
    log("Verifying remote hosts...");
    for(EntryJobVolume jobvol : job.backup) {
      if (Status.abort) return false;
      ServerClient serverclient = BackupService.server.getClient(jobvol.host);
      if (serverclient == null) {
        log("Error:Host offline:" + jobvol.host);
        return false;
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
      log("Error:unable to get drive parameters" + ":Error=" + tape.lastError());
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
    for(EntryJobVolume jobvol : job.backup) {
      currentjobvol = jobvol;
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
      ServerClient client = BackupService.server.getClient(jobvol.host);
      if (client == null) {
        log("Error:client not found:" + jobvol.host);
        return false;
      }
      boolean success = false;
      EntryVolume volume = new EntryVolume();
      volume.host = jobvol.host;
      volume.volume = jobvol.volume;
      volume.path = jobvol.path;
      volume.name = "vol-" + volCount++;
      cat.volumes.add(volume);
      currentvolume = volume;
      if (client.getVersion() < Config.APIVersionReadFolders) {
        //old slow recursive "listfolder" then recursive "readfile" commands
        log("Listing files...");
        EntryFolder root = listFolder(jobvol, jobvol.path);
        if (root == null) {
          log("Error:ListVolume failed");
          return false;
        }
        root.name = "";
        //save to catalog
        volume.root = root;
        log("Backing up files...");
        success = doFolder(root, "");
      } else {
        //new faster "readfolders" command
        log("Backing up files...");
        success = readFolders();
      }
      if (!unmountVolume(jobvol)) {
        log("Error:Unable to unmount volume");
        return false;
      }
      if (!success) return false;
    }
    if (haveChanger) {
      emptyDrive();
    }
    log("Total:" + ConfigService.toEng(Status.copied) + " files " + Status.files);
    return true;
  }
  public void log(Exception e) {
    JFLog.log(3, e);
  }
  public void log(String msg) {
    JFLog.log(3, msg);
    Status.log.append(msg + "\r\n");
  }
  private boolean doFolder(EntryFolder parent, String path) {
    if (parent == null) return false;
    for(EntryFile file : parent.files) {
      if (Status.abort) return false;
      if (!doFile(file, path)) {
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
    if (!readFile(file, path)) return false;
    return true;
  }
  public boolean prepNextTape() {
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
      log("Error:Tape rewind failed" + ":Error=" + tape.lastError());
      return false;
    }
    currentTape = new EntryTape(barcode, backupid, retentionid, job.name, catnfo.tapes.size() + 1);
    //get tape info
    MediaInfo mediainfo = getMediaInfo();
    if (mediainfo == null) {
      log("Error:Tape get Media Info failed" + ":Error=" + tape.lastError());
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
      log("Error:Tape write header failed" + ":Error=" + tape.lastError());
      return false;
    }
    return true;
  }
  private EntryTape findTape(String barcode) {
    for(EntryTape tape : catnfo.tapes) {
      if (tape.barcode.equals(barcode)) return tape;
    }
    return null;
  }
  private boolean loadEmptyTape() {
    if (!updateList()) return false;
    for(int idx=0;idx<elements.length;idx++) {
      if (elements[idx].barcode.equals("<empty>")) continue;
      //check if tape is protected
      if (Tapes.findTape(elements[idx].barcode) != null) continue;
      if (findTape(elements[idx].barcode) != null) continue;
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
          log("Error:Move Tape failed:" + elements[driveIdx].name + " to " + elements[emptySlotIdx].name + ":Error=" + changer.lastError());
          return false;
        }
        return loadEmptyTape();
      }
      //move desired tape to drive
      log("Move Tape:" + elements[idx].name + " to " + elements[driveIdx].name);
      if (!changer.move(elements[idx].name, elements[driveIdx].name)) {
        log("Error:Move Tape failed:" + elements[idx].name + " to " + elements[driveIdx].name + ":Error=" + changer.lastError());
        return false;
      }
      return loadEmptyTape();
    }
    return false;
  }
  private boolean emptyDrive() {
    if (!updateList()) return false;
    if (emptySlotIdx == -1) {
      log("Error:No empty slot to move tapes");
      return false;
    }
    if (elements[driveIdx].barcode.equals("<empty>")) return true;
    log("Move Tape:" + elements[driveIdx].name + " to " + elements[emptySlotIdx].name);
    if (!changer.move(elements[driveIdx].name, elements[emptySlotIdx].name)) {
      log("Error:Move Tape failed:" + elements[driveIdx].name + " to " + elements[emptySlotIdx].name + ":Error=" + changer.lastError());
      return false;
    }
    return true;
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

    log("WriteHeader to tape:" + barcode);

    if (!tape.write(data, 0, data.length)) {
      log("Error:Tape write failed" + ":Error=" + tape.lastError());
      return false;
    }

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
        if (req == null || req.reply == null) {
          reply = "NOPE";
        } else {
          reply = req.reply;
        }
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
        if (req == null || req.reply == null) {
          reply = "NOPE";
        } else {
          reply = req.reply;
        }
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
    ServerClient client = getClient(jobvol);
    Object lock = new Object();
    reply = null;
    synchronized(lock) {
      client.addRequest(new Request("listfolder", path), (Request req) -> {
        if (req == null || req.reply == null) {
          reply = "NOPE";
        } else {
          reply = req.reply;
        }
        synchronized(lock) {
          lock.notify();
        }
      });
      while (reply == null) {
        try {lock.wait();} catch (Exception e) {}
      }
    }
    if (reply == null) return null;
    EntryFolder folder = new EntryFolder();
    if (reply.length() > 0) {
      String list[] = reply.split("\r\n");
      for(String ff : list) {
        if (Status.abort) return null;
        String f[] = ff.split("[|]");
        if (f[0].startsWith("\\")) {
          EntryFolder subfolder = listFolder(jobvol, path + f[0]);
          if (subfolder == null) return null;
          subfolder.name = f[0].substring(1);
          folder.folders.add(subfolder);
        } else {
          if (f[0].length() == 0) continue;
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
    ServerClient client = getClient(currentjobvol);
    Object lock = new Object();
    long maxBlocks = file.u / blocksize + 4;  //estimate on compression
    if (haveChanger) {
      if (currentTape == null || currentTape.left < maxBlocks) {
        if (!prepNextTape()) return false;
      }
    }
    file.o = currentTape.position;
    file.t = catnfo.tapes.size();
    try {
      PipedInputStream pis = new PipedInputStream();
      PipedOutputStream pos = new PipedOutputStream(pis);
      Transfer transfer = new Transfer(pis);
      transfer.start();
      synchronized(lock) {
        client.addRequest(new Request("readfile", currentjobvol.path + path + "\\" + file.name, pos), (Request req) -> {
          if (req == null) {
            file.c = -1;
          } else {
            transfer.compressed = req.compressed;
            try {
              pos.flush();
              pos.close();
            } catch (Exception e) {}
            file.c = req.compressed;
          }
          synchronized(lock) {
            lock.notify();
          }
        });
        while (file.c == 0) {
          try {lock.wait();} catch (Exception e) {}
        }
      }
      transfer.join();
      if (file.c == -1) {
        log("Error:Read file failed");
        return false;
      }
      file.b = file.c / blocksize;
      if (file.c % blocksize > 0) {
        file.b++;
      }
      currentTape.left -= file.b;
      currentTape.position += file.b;
      Status.copied += file.u;
      Status.files++;
      return true;
    } catch (Exception e) {
      log(e);
      return false;
    }
  }
  private static byte buffer[] = new byte[64 * 1024];
  private static final int bufferSize = 64 * 1024;
  public class Transfer extends Thread {
    private InputStream is;
    public long copied;
    public long compressed = -1;
    public Transfer(InputStream is) {
      this.is = is;
    }
    public void run() {
      int pos = 0;
      int len = 0;
      try {
        while (Status.active) {
          if (copied == compressed) break;
          int toread = blocksize - len;
          int read = is.read(buffer, pos, toread);
          if (read == -1) break;
          if (read == 0) continue;
          copied += read;
          pos += read;
          len += read;
          if (len == bufferSize) {
            if (!tape.write(buffer, 0, bufferSize)) {
              throw new Exception("tape write failed");
            }
            len = 0;
            pos = 0;
          }
        }
        if (len > 0) {
          if (len < bufferSize) {
            Arrays.fill(buffer, pos, bufferSize, (byte)0);
          }
          if (!tape.write(buffer, 0, bufferSize)) {
            throw new Exception("tape write failed");
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
  private boolean readFolders() {
    Object lock = new Object();
    ServerClient client = getClient(currentjobvol);
    try {
      synchronized(lock) {
        client.addRequest(new Request("readfolders", currentjobvol.path, tape), (Request req) -> {
          if (req == null) {
            Status.abort = true;
            currentvolume.root = new EntryFolder();
          } else {
            currentvolume.root = req.root;
          }
          synchronized(lock) {
            lock.notify();
          }
        });
        while (currentvolume.root == null) {
          try {lock.wait();} catch (Exception e) {}
        }
      }
      return true;
    } catch (Exception e) {
      log(e);
      return false;
    }
  }
  private boolean isValid(String str) {
    if (str == null) return false;
    if (str.length() == 0) return false;
    return true;
  }
  private void email_notify(boolean success) {
    if (!isValid(Config.current.email_server)) return;
    if (!isValid(Config.current.emails)) return;
    log("Sending email notification");
    SMTP smtp = new SMTP();
    try {
      int port = -1;
      String server = Config.current.email_server;
      int idx = server.indexOf(':');
      if (idx != -1) {
        port = Integer.valueOf(server.substring(idx+1));
        server = server.substring(0, idx);
      }
      if (Config.current.email_secure) {
        if (port == -1) port = 465;
        smtp.connect(server, port);
      } else {
        if (port == -1) port = 25;
        smtp.connect(server, port);
      }
      if (!smtp.login()) {
        throw new Exception("SMTP HELLO failed");
      }
      if (isValid(Config.current.email_user) && isValid(Config.current.email_pass) && isValid(Config.current.email_type)) {
        if (!smtp.auth(Config.current.email_user, Config.current.email_pass, Config.current.email_type)) {
          throw new Exception("SMTP auth failed");
        }
      }
      if (isValid(Config.current.email_user)) {
        smtp.from(Config.current.email_user);
      } else {
        smtp.from("jfBackup@" + Config.current.server_host);
      }
      String emails[] = Config.current.emails.split(",");
      for(String email : emails) {
        smtp.to(email);
      }
      StringBuilder msg = new StringBuilder();
      if (isValid(Config.current.email_user)) {
        msg.append("From: <" + Config.current.email_user + ">\r\n");
      } else {
        msg.append("From: <jfBackup@" + Config.current.server_host + ">\r\n");
      }
      for(String email : emails) {
        msg.append("To: <" + email + ">\r\n");
      }
      msg.append("Subject:Backup ");
      msg.append(job.name);
      msg.append(" ");
      msg.append(success ? "successful" : "failed");
      msg.append("\r\n\r\n");
      msg.append("Backup job:");
      msg.append(job.name);
      msg.append("\r\n");
      msg.append("Date:");
      msg.append(ConfigService.toDateTime(backupid));
      msg.append("\r\n");
      if (success) {
        msg.append("Backup job was successful.\r\n");
        msg.append("Tapes Expire:");
        msg.append(ConfigService.toDateTime(retentionid));
        msg.append("\r\n");
        msg.append("Eject these tapes:\r\n");
        for(EntryTape tape : catnfo.tapes) {
          msg.append("Tape:");
          msg.append(tape.barcode);
          msg.append("\r\n");
        }
      } else {
        msg.append("Backup job failed, see logs for more details.");
      }
      if (!smtp.data(msg.toString())) {
        throw new Exception("Send email message failed");
      }
      smtp.logout();
      smtp.disconnect();
    } catch (Exception e) {
      log("Email notification failed");
      log("SMTP response=" + smtp.getLastResponse());
      log(e);
    }
  }
}
