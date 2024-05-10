package service;

/** Stats
 *
 * Performs VM stats generation every 20 seconds.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.vm.*;

public class Stats extends TimerTask {
  private long last_clean = 0L;
  private long day = 1000L * 60L * 60L * 24L;
  private Timer timer;

  public void start() {
    timer = new Timer();
    Calendar now = Calendar.getInstance();
    int _sec = now.get(Calendar.SECOND);
    long delay = ((_sec / 20) * 1000L) + 5000L;  //start 5 seconds into next sample
    timer.scheduleAtFixedRate(this, delay, 20L * 1000L);
  }

  public void stop() {
    timer.cancel();
  }

  public void run() {
    for(int a=0;a<20;a++) {
      JF.sleep(1000);
    }
    {
      Calendar now = Calendar.getInstance();
      int _year = now.get(Calendar.YEAR);
      int _month = now.get(Calendar.MONTH) + 1;
      int _day = now.get(Calendar.DAY_OF_MONTH);
      int _hour = now.get(Calendar.HOUR_OF_DAY);
      int _min = now.get(Calendar.MINUTE);
      int _sec = now.get(Calendar.SECOND);
      int _sample = (_min * 60 + _sec) / 20;  //0-179
      VMHost.get_all_stats(_year, _month, _day, _hour, _sample);
    }
    {
      long now = System.currentTimeMillis();
      if (now > last_clean) {
        VMHost.clean_stats(Config.current.stats_days);
      }
      last_clean = now + day;
    }
  }
}
