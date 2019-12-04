/** MediaChanger API
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class MediaChanger {
  public Element[] list() {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"tapetool", Config.current.changerDevice, "list"}, false);
    String lns[] = out.split("\r\n");
    ArrayList<Element> elements = new ArrayList<Element>();
    for(int a=0;a<lns.length;a++) {
      if (lns[a].startsWith("Error:")) {
        JFLog.log(2, "Tape:" + lns[a]);
        return null;
      }
      else if (lns[a].startsWith("element:")) {
        String fs[] = lns[a].split("[:]");
        Element element = new Element();
        element.name = fs[1];
        element.barcode = fs[2];
        elements.add(element);
      }
    }
    return elements.toArray(new Element[elements.size()]);
  }
  public boolean move(String src, String dst) {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"tapetool", Config.current.changerDevice, "move", src, dst}, false);
    String lns[] = out.split("\r\n");
    for(int a=0;a<lns.length;a++) {
      if (lns[a].startsWith("Error:")) {
        JFLog.log(2, "Tape:" + lns[a]);
        return false;
      }
    }
    JF.sleep(1000);
    return true;
  }
  public boolean move(String src, String transport, String dst) {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"tapetool", Config.current.changerDevice, "move", src, transport, dst}, false);
    String lns[] = out.split("\r\n");
    for(int a=0;a<lns.length;a++) {
      if (lns[a].startsWith("Error:")) {
        JFLog.log(2, "Tape:" + lns[a]);
        return false;
      }
    }
    return true;
  }
}
