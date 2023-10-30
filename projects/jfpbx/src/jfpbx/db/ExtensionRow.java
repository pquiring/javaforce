package jfpbx.db;

/**
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.db.*;

public class ExtensionRow extends Row {
  public String number;
  public int type;
  public static final int EXT = 1;
  public static final int IVR = 2;
  public static final int QUEUE = 3;

  //common
  public String display;

  //extension
  public String cid;
  public String password;
  public String routetable;
  public boolean voicemail;
  public String voicemailpass;
  public String email;  //ver 2

  //IVR
  public String script;

  //Queue
  public String agents;  //comma list
  public String message;

  private static final int version = 2;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    type = readInt();
    number = readString();
    display = readString();
    switch (type) {
      case EXT:
        cid = readString();
        password = readString();
        routetable = readString();
        voicemail = readBoolean();
        voicemailpass = readString();
        if (ver >= 2) {
          email = readString();
        } else {
          email = "";
        }
        break;
      case IVR:
        script = readString();
        break;
      case QUEUE:
        agents = readString();
        message = readString();
        break;
      default:
        JFLog.log("Error:ExtensionRow:readObject():type=" + type);
        break;
    }
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(type);
    writeString(number);
    writeString(display);
    switch (type) {
      case EXT:
        writeString(cid);
        writeString(password);
        writeString(routetable);
        writeBoolean(voicemail);
        writeString(voicemailpass);
        writeString(email);
        break;
      case IVR:
        writeString(script);
        break;
      case QUEUE:
        writeString(agents);
        writeString(message);
        break;
      default:
        JFLog.log("Error:ExtensionRow:writeObject():type=" + type);
        break;
    }
  }
}
