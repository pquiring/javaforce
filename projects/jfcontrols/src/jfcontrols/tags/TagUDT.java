package jfcontrols.tags;

/** Base class for all UDT instance tags.
 *
 * @author User
 */

public class TagUDT extends TagBase {
  public TagUDT() {}
  public TagUDT(int type) {
    this.type = type;
  }
  public TagUDT(int cid, int tid, int type, int length, int fieldCount) {
    this.cid = cid;
    this.tid = tid;
    this.type = type;
    if (length == 0) {
      isArray = false;
      fields = new TagBase[1][];
      length = 1;
    } else {
      isArray = true;
      fields = new TagBase[length][];
    }
    for(int a=0;a<length;a++) {
      fields[a] = new TagBase[fieldCount];
    }
  }
  public TagBase fields[][];

  public void init(TagBase parent) {
    super.init(parent);
    if (fields == null) return;
    for(int a=0;a<fields.length;a++) {
      TagBase row[] = fields[a];
      for(int b=0;b<row.length;b++) {
        row[b].init(this);
      }
    }
  }

  public String toString() {return null;}
  public String toString(int idx) {return null;}
  public int getLength() {return fields.length;}

  public boolean getBoolean(int idx) {
    return false;
  }

  public void setBoolean(int idx, boolean value) {
  }

  public int getInt(int idx) {
    return 0;
  }

  public void setInt(int idx, int value) {
  }

  public long getLong(int idx) {
    return 0;
  }

  public void setLong(int idx, long value) {
  }

  public float getFloat(int idx) {
    return 0;
  }

  public void setFloat(int idx, float value) {
  }

  public double getDouble(int idx) {
    return 0;
  }

  public void setDouble(int idx, double value) {
  }

  public TagBase[] getFields(int idx) {
    return fields[idx];
  }

  public TagBase[] getFields() {
    return getFields(0);
  }

  public TagBase getField(int idx, String name) {
    for(int a=0;a<fields.length;a++) {
      if (fields[idx][a].name.equals(name)) {
        return fields[idx][a];
      }
    }
    return null;
  }
  public void readObject() throws Exception {
    super.readObject();
    int cnt = readInt();
    fields = new TagBase[cnt][];
    for(int a=0;a<cnt;a++) {
      int fieldCnt = readInt();
      fields[a] = new TagBase[fieldCnt];
      for(int b=0;b<fieldCnt;b++) {
        int type = readInt();
        fields[a][b] = TagsService.createTag(type);
        fields[a][b].readInit(this);
        fields[a][b].readObject();
      }
    }
  }
  public void writeObject() throws Exception {
    super.writeObject();
    int cnt = fields.length;
    writeInt(cnt);
    for(int a=0;a<cnt;a++) {
      int fieldCnt = fields[a].length;
      writeInt(fieldCnt);
      for(int b=0;b<fieldCnt;b++) {
        writeInt(fields[a][b].type);
        fields[a][b].writeInit(this);
        fields[a][b].writeObject();
      }
    }
  }
}
