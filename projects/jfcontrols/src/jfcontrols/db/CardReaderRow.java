package jfcontrols.db;

/** CardReaderRow
 *
 * @author pquiring
 */

public class CardReaderRow extends javaforce.db.Row {
  public static final long serialVersionUID = 1L;
  public String name;  //display name
  public String addr;  //ip address of IP card reader
  public String door;  //output tag
  public int type;  //0=generic 1=RFideas
}
