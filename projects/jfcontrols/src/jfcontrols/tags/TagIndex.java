package jfcontrols.tags;

/** Index Tag.
 *
 * @author pquiring
 */

public class TagIndex extends TagBase {
  public TagBase tag;
  public int idx;
  public TagIndex(TagBase tag, int idx) {
    this.tag = tag;
    this.idx = idx;
  }

  public String toString(int idx) {
    if (tag == null) return "";
    return tag.toString(idx);
  }

  public int getLength() {
    return 1;
  }

  public boolean getBoolean(int idx) {
    return tag.getBoolean(idx);
  }

  public void setBoolean(int idx, boolean value) {
    tag.setBoolean(idx, value);
  }

  public int getInt(int idx) {
    return tag.getInt(idx);
  }

  public void setInt(int idx, int value) {
    tag.setInt(idx, value);
  }

  public long getLong(int idx) {
    return tag.getLong(idx);
  }

  public void setLong(int idx, long value) {
    tag.setLong(idx, value);
  }

  public float getFloat(int idx) {
    return tag.getFloat(idx);
  }

  public void setFloat(int idx, float value) {
    tag.setFloat(idx, value);
  }

  public double getDouble(int idx) {
    return tag.getDouble(idx);
  }

  public void setDouble(int idx, double value) {
    tag.setDouble(idx, value);
  }

  public boolean getBoolean() {
    return getBoolean(idx);
  }
  public void setBoolean(boolean value) {
    setBoolean(idx, value);
  }
  public int getInt() {
    return getInt(idx);
  }
  public void setInt(int value) {
    setInt(idx, value);
  }
  public long getLong() {
    return getLong(idx);
  }
  public void setLong(long value) {
    setLong(idx, value);
  }
  public float getFloat() {
    return getFloat(idx);
  }
  public void setFloat(float value) {
    setFloat(idx, value);
  }
  public double getDouble() {
    return getDouble(idx);
  }
  public void setDouble(double value) {
    setDouble(idx, value);
  }
  public TagBase[] getFields(int idx) {
    TagUDT udt = (TagUDT)tag;
    return udt.getFields(idx);
  }
  public TagBase[] getFields() {
    TagUDT udt = (TagUDT)tag;
    return udt.getFields(idx);
  }
  public void readObject() throws Exception {
    super.readObject();
  }
  public void writeObject() throws Exception {
    super.writeObject();
  }
}
