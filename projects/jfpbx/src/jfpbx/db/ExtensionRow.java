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
}
