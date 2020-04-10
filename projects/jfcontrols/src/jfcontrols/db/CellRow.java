package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class CellRow extends javaforce.db.Row {
  public CellRow() {};
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
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    pid = readInt();
    x = readInt();
    y = readInt();
    w = readInt();
    h = readInt();
    comp = readString();
    name = readString();
    text = readString();
    tag = readString();
    func = readString();
    arg = readString();
    style = readString();
    events = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(pid);
    writeInt(x);
    writeInt(y);
    writeInt(w);
    writeInt(h);
    writeString(comp);
    writeString(name);
    writeString(text);
    writeString(tag);
    writeString(func);
    writeString(arg);
    writeString(style);
    writeString(events);
  }
}
