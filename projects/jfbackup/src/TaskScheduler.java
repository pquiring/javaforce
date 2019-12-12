/** TaskScheduler
 *
 *  Monitors when jobs need to be started.
 *
 * @author pquiring
 */

import java.util.*;

public class TaskScheduler extends TimerTask {
  private Timer timer;
  private int lastmin = -1;
  public void init() {
    timer = new Timer();
    timer.scheduleAtFixedRate(this, 45 * 1000, 45 * 1000);
  }
  public void run() {
    if (Status.running) return;
    Calendar c = Calendar.getInstance();
    int year = c.get(Calendar.YEAR);  //2000+
    int month = c.get(Calendar.MONTH) + 1;  //1-12
    int day = c.get(Calendar.DAY_OF_MONTH);  //1-31
    int dayWeek = c.get(Calendar.DAY_OF_WEEK);  //1-7
    int hour = c.get(Calendar.HOUR_OF_DAY);  //0-23
    int minute = c.get(Calendar.MINUTE);  //0-59
    if (minute == lastmin) return;
    lastmin = minute;
    for(EntryJob job : Config.current.backups) {
      switch (job.freq) {
        case "daily": {
          if (hour == job.hour && minute == job.minute) {
            startJob(job);
            return;
          }
          break;
        }
        case "weekly": {
          if (dayWeek == job.day && hour == job.hour && minute == job.minute) {
            startJob(job);
            return;
          }
          break;
        }
        default: {
          System.out.println("Error:Unsupported task freq:" + job.freq);
          break;
        }
      }
    }
  }
  public static synchronized void startJob(EntryJob job) {
    Status.running = true;
    Status.abort = false;
    Status.desc = "Running backup job";
    Status.copied = 0;
    Status.files = 0;
    Status.log = new StringBuilder();
    Status.backup = new BackupJob(job);
    Status.job = Status.backup;
    Status.job.start();
  }
}
