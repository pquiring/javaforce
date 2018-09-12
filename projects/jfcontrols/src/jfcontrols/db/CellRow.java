package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class CellRow extends javaforce.db.Row {
  public CellRow(int pid, int x, int y, int w, int h, String comp, String name, String text) {
    this.pid = pid;
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.comp = comp;
    this.name = name;
    this.text = text;
  }
  public CellRow setTag(String tag) {
    this.tag = tag;
    return this;
  }
  public CellRow setFunc(String func) {
    this.func = func;
    return this;
  }
  public CellRow setArg(String arg) {
    this.arg = arg;
    return this;
  }
  public CellRow setFuncArg(String func, String arg) {
    this.func = func;
    this.arg = arg;
    return this;
  }
  public CellRow setStyle(String style) {
    this.style = style;
    return this;
  }
  public CellRow setEvents(String events) {
    this.events = events;
    return this;
  }
  public int pid;
  public int x,y;
  public int w,h;
  public String comp;
  public String name;
  public String text;
  public String tag;
  public String func;
  public String arg;
  public String style;
  public String events;
}
