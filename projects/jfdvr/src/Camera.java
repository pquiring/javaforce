/**
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.io.*;

public class Camera extends SerialObject implements Serializable {
  public static final long serialVersionUID = 1;
  public String name;
  public String url;
  public boolean record_motion;
  public int record_motion_threshold;
  public int record_motion_after;
  public int max_file_size;  //in MBs
  public int max_folder_size;  //in GBs

  public transient volatile float motion_value;
  public transient volatile byte[] preview;  //png image
  public transient volatile boolean viewing;
  public transient volatile boolean update_preview;

  public static final short id_name = id_len + 1;
  public static final short id_url = id_len + 2;

  public static final short id_record_motion = id_1 + 1;

  public static final short id_record_motion_threshold = id_4 + 1;
  public static final short id_record_motion_after = id_4 + 2;
  public static final short id_max_file_size = id_4 + 3;
  public static final short id_max_folder_size = id_4 + 4;

  public void readObject() throws Exception {
    do {
      short id = readShort();
      switch (id) {
        case id_name: name = readString(); break;
        case id_url: url = readString(); break;
        case id_record_motion: record_motion = readBoolean(); break;
        case id_record_motion_threshold: record_motion_threshold = readInt(); break;
        case id_record_motion_after: record_motion_after = readInt(); break;
        case id_max_file_size: max_file_size = readInt(); break;
        case id_max_folder_size: max_folder_size = readInt(); break;
        case id_end: return;
        default: skipChunk(id); break;
      }
    } while (true);
  }

  public void writeObject() throws Exception {
    writeShort(id_name);
    writeString(name);
    writeShort(id_url);
    writeString(url);
    writeShort(id_record_motion);
    writeBoolean(record_motion);
    writeShort(id_record_motion_threshold);
    writeInt(record_motion_threshold);
    writeShort(id_record_motion_after);
    writeInt(record_motion_after);
    writeShort(id_max_file_size);
    writeInt(max_file_size);
    writeShort(id_max_folder_size);
    writeInt(max_folder_size);
    writeShort(id_end);
  }
}
