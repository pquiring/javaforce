package javaforce.db;

public class Row implements java.io.Serializable {
  public static final long serialVersionUID = 1;
  
  /** auto-increment id */
  public int id;
  
  /** Override this method to sort your rows. 
   *  @return 0=equal -1=this is lower +1=other is lower
   */
  public int compare(Row other) {
    if (id == other.id) return 0;
    if (id < other.id) return -1;
    return 1;
  }
}
