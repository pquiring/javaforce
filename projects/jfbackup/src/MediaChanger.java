/** MediaChanger API
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.api.*;

public class MediaChanger {
  private long handle;
  public boolean open(String name) {
    handle = WindowsAPI.getInstance().changerOpen("\\\\.\\" + name);
    return handle != 0;
  }
  public void close() {
    if (handle == 0) return;
    WindowsAPI.getInstance().changerClose(handle);
    handle = 0;
  }
  public Element[] list() {
    String lns[] = WindowsAPI.getInstance().changerList(handle);
    if (lns == null) return null;
    ArrayList<Element> elements = new ArrayList<Element>();
    for(int a=0;a<lns.length;a++) {
      String fs[] = lns[a].split("[:]");
      Element element = new Element();
      element.name = fs[0];
      if (fs.length == 1) {
        element.barcode = "<empty>";
      } else {
        element.barcode = fs[1];
      }
      elements.add(element);
    }
    return elements.toArray(new Element[elements.size()]);
  }
  public boolean move(String src, String dst) {
    boolean ok = WindowsAPI.getInstance().changerMove(handle, src, null, dst);
    JF.sleep(1000);
    return ok;
  }
  public boolean move(String src, String transport, String dst) {
    boolean ok = WindowsAPI.getInstance().changerMove(handle, src, transport, dst);
    JF.sleep(1000);
    return ok;
  }
  public String lastError() {
    return String.format("0x%08x : Handle=0x%08x", WindowsAPI.getInstance().tapeLastError(), handle);
  }
}
