package jfcontrols.db;

/** CardRow
 *
 * @author pquiring
 */

public class CardRow extends javaforce.db.Row {
  public int uid;  //user id
  public long card;  //card #
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    uid = readInt();
    card = readLong();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(uid);
    writeLong(card);
  }
}
