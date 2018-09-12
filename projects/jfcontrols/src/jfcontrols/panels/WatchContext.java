package jfcontrols.panels;

/** Watch Table Context
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.tags.*;
import jfcontrols.db.*;

public class WatchContext extends Thread {
  private volatile boolean active;
  private Object lock = new Object();
  private TagBase tags[];
  private Label tv[];

  public boolean init(WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    String wid = (String)client.getProperty("watch");
    WatchRow data[] = Database.getWatchTagsById(Integer.valueOf(wid));
    if (data == null) return false;
    int cnt = data.length;
    tags = new TagBase[cnt];
    tv = new Label[cnt];
    Panel panel = client.getPanel();
    for(int a=0;a<cnt;a++) {
      tags[a] = context.getTag(data[a].tag);
      tv[a] = (Label)panel.getComponent("tag_" + a);
    }
    return true;
  }
  public void run() {
    active = true;
    int cnt = tags.length;
    while (active) {
      for(int a=0;a<cnt;a++) {
        TagBase tag = tags[a];
        if (tag == null) continue;
        Label lbl = tv[a];
        if (lbl == null) continue;
        lbl.setText(tag.getValue());
      }
      if (!active) break;
      JF.sleep(100);
    }
    synchronized(lock) {
      lock.notify();
    }
  }
  public void cancel() {
    synchronized(lock) {
      active = false;
      try {lock.wait();} catch (Exception e) {}
    }
  }
}
